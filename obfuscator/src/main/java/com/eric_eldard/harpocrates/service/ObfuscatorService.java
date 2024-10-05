package com.eric_eldard.harpocrates.service;

import jakarta.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.eric_eldard.harpocrates.enumeration.Action;
import com.eric_eldard.harpocrates.enumeration.DataType;
import com.eric_eldard.harpocrates.exception.BadMatchException;
import com.eric_eldard.harpocrates.model.DataDefinition;

public class ObfuscatorService
{
    private static final Pattern CREATE_TABLE_PATTERN =
        Pattern.compile("CREATE TABLE (`.*?`) \\(");

    private static final Pattern DATA_CLASSIFICATION_COL_DEF_PATTERN =
        Pattern.compile("\s*(`.+?`) .+ COMMENT 'dataClassification=(.+?)',?");

    private static final Pattern DOC_ID_PATTERN =
        Pattern.compile("#\\{([0A]+?)}");

    private static final Pattern ENABLE_KEYS_PATTERN =
        Pattern.compile(".*ALTER TABLE (`.+?`) ENABLE KEYS.*");

    private static final Pattern INSERT_PATTERN =
        Pattern.compile("INSERT INTO (`.+?`) \\((.+?)\\) VALUES \\((.+?)\\);");


    public void obfuscate(File dumpFile, String outputFileLocation) throws IOException
    {
        File outputFile = new File(outputFileLocation);
        if (outputFile.exists())
        {
            outputFile.delete();
        }
        outputFile.createNewFile();

        try (BufferedReader reader = new BufferedReader(new FileReader(dumpFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true)))
        {
            String currentTable = null;
            Map<String, DataDefinition> currentDataDefs = null;

            String readLine = reader.readLine();
            while (readLine != null)
            {
                String writeLine = readLine;

                // Nasty if-else chain avoids unnecessary matching, since each line can only match one of these patterns
                // (ordered by likelihood of a MySQL dump line being a match)
                //<editor-fold desc="Match INSERT statements">
                Matcher insertMatcher = INSERT_PATTERN.matcher(writeLine);
                if (insertMatcher.find())
                {
                    writeLine = obfuscateInsertStmt(insertMatcher, currentTable, currentDataDefs);
                }
                //</editor-fold>
                else
                {
                    //<editor-fold desc="Match column def statements with data classifications">
                    Matcher dataClassificationMatcher = DATA_CLASSIFICATION_COL_DEF_PATTERN.matcher(writeLine);
                    if (dataClassificationMatcher.find())
                    {
                        if (currentTable == null)
                        {
                            throw new BadMatchException(
                                "Column def found without table: [" + dataClassificationMatcher.group() + ']');
                        }
                        String colName = dataClassificationMatcher.group(1);
                        String encodedDataDef = dataClassificationMatcher.group(2);
                        currentDataDefs.put(colName, decodeDataDef(encodedDataDef));
                    }
                    //</editor-fold>
                    else
                    {
                        //<editor-fold desc="Match CREATE TABLE statements">
                        Matcher createTableMatcher = CREATE_TABLE_PATTERN.matcher(writeLine);
                        if (createTableMatcher.find())
                        {
                            if (currentTable != null)
                            {
                                throw new BadMatchException("Table def found [" + createTableMatcher.group() +
                                    "], but we're currently examining a different table: [" + currentTable + ']');
                            }
                            currentTable = createTableMatcher.group(1);
                            currentDataDefs = new HashMap<>();
                        }
                        //</editor-fold>
                        else
                        {
                            //<editor-fold desc="Match ENABLE KEYS statements (signals the end of inserts to a table)">
                            Matcher enableKeyMatcher = ENABLE_KEYS_PATTERN.matcher(writeLine);
                            if (enableKeyMatcher.find())
                            {
                                if (!enableKeyMatcher.group(1).equals(currentTable))
                                {
                                    throw new BadMatchException(
                                        "End of table encountered [" + enableKeyMatcher.group() +
                                            "], but it doesn't match the current table [" + currentTable + ']');
                                }
                                currentTable = null;
                                currentDataDefs = null;
                            }
                            //</editor-fold>
                        }
                    }
                }

                writer.write(writeLine);
                writer.newLine();

                readLine = reader.readLine();
            }
        }
    }

    private DataDefinition decodeDataDef(String encodedJson)
    {
        return DataDefinition.from(
            URLDecoder.decode(
                encodedJson,
                Charset.defaultCharset()
            ));
    }

    private String obfuscateInsertStmt(Matcher insertMatcher, String tableName, Map<String, DataDefinition> dataDefs)
    {
        if (!insertMatcher.group(1).equals(tableName))
        {
            throw new BadMatchException(
                "Table name [" + tableName + "] not matched in insert statement [" + insertMatcher.group() + ']');
        }

        List<String> colNames = Arrays.asList(insertMatcher.group(2).split(", "));
        List<String> data = splitValues(insertMatcher.group(3), colNames.size());

        for (String datum : data)
        {
            int index = data.indexOf(datum);
            String colName = colNames.get(index);
            if (dataDefs.containsKey(colName))
            {
                DataDefinition dataDef = dataDefs.get(colName);
                if (dataDef.getType() == DataType.NOT_SENSITIVE || dataDef.getAction() == Action.IGNORE)
                {
                    continue;
                }
                if (dataDef.getAction() == Action.REMOVE)
                {
                    data.set(index, null);
                }
                else if (dataDef.getAction() == Action.REPLACE)
                {
                    data.set(index, obfuscateDatum(dataDef));
                }
            }
        }

        String obfuscatedStmt = insertMatcher.group().replaceAll(
            "VALUES \\(.+\\)",
            String.format("VALUES (%s)", String.join(",", data))
        );

        return obfuscatedStmt;
    }

    private String obfuscateDatum(DataDefinition dataDef)
    {
        String pattern = dataDef.getPattern() == null ?
            dataDef.getType().getPattern() :
            dataDef.getPattern();

        if (pattern == null)
        {
            // DataType#OTHER selected, but no replacement pattern provided; treat instead as Action#REMOVE
            return null;
        }

        pattern = pattern
            .replace(DataType.CITY.getPattern(),           AddressService.INSTANCE.makeCity())
            .replace(DataType.DATE.getPattern() ,          DateService.INSTANCE.makeRandomDateString())
            .replace(DataType.FULL_ADDRESS.getPattern(),   AddressService.INSTANCE.makeAddress())
            .replace(DataType.GIVEN_NAME.getPattern(),     NameService.INSTANCE.makeGivenName())
            .replace(DataType.ORGANIZATION.getPattern(),   OrganizationService.INSTANCE.makeOrganization())
            .replace(DataType.PHONE_NUMBER.getPattern(),   PhoneNumberService.INSTANCE.makePhoneNumber())
            .replace(DataType.SSN.getPattern(),            SsnService.INSTANCE.makeSsn())
            .replace(DataType.STATE.getPattern(),          AddressService.INSTANCE.makeState())
            .replace(DataType.STREET_ADDRESS.getPattern(), AddressService.INSTANCE.makeStreetAddress())
            .replace(DataType.SURNAME.getPattern(),        NameService.INSTANCE.makeSurname())
            .replace(DataType.ZIP_CODE.getPattern(),       AddressService.INSTANCE.makeZip());

        pattern = obfuscateDocIds(pattern);

        return quoteStmt(escapeQuotes(pattern));
    }

    private String obfuscateDocIds(String pattern)
    {
        Matcher matcher = DOC_ID_PATTERN.matcher(pattern);
        while (matcher.find())
        {
            String mask = matcher.group(1);
            pattern = pattern.replaceFirst(
                "#\\{" + mask + "}",
                DocIdService.INSTANCE.makeDocId(mask)
            );
        }
        return pattern;
    }

    /**
     * Extracts a list of values from a comma separate string of values by examining the string character by character.
     * This can't be achieved accurately with String.split(), since individual values may contain commas, and is nigh
     * impossible with a regex, since values may or may not be surrounded with single quotes (that is, some are strings
     * and some aren't) and those that are may contain single quotes (escaped as '' or \').
     */
    private List<String> splitValues(String values, int numValues)
    {
        List<String> valList = new ArrayList<>(numValues);
        StringBuilder currentWord = new StringBuilder();
        boolean insideString = false;

        for (int i = 0; i < values.length(); i++)
        {
            char currentChar = values.charAt(i);
            boolean lastChar = i == values.length() - 1;

            if (currentChar == ',' && !insideString)
            {
                valList.add(currentWord.toString());
                currentWord.setLength(0);
            }
            else
            {
                // Track whether we're currently inside a string value, so we can know to ignore commas. If we encounter
                // escaped single quote chars inside the string, we'll write those off together (escape char + quote
                // char) and then skip to the next char.
                // Ignore all of this if this is the last char; we'll just write it off w/o caring about tracking
                // whether we're in a string.
                if (!lastChar)
                {
                    // Detect next character is an escaped single quote (\' or '') inside of a string
                    char nextChar = values.charAt(i + 1);
                    if (insideString && (currentChar == '\'' || currentChar == '\\') && nextChar == '\'')
                    {
                        // Write escape char and single quote to our current string, then skip the next char (the single
                        // quote we peaked ahead at), since we already wrote it. This allows escaped single quotes to be
                        // tackled in pairs, which avoids issues when a string ends in an escaped quote
                        // (ex: 'We're obfuscatin''')
                        if (currentChar == '\\')
                        {
                            currentWord.append('\\'); // if escape char is \, it also needs to be escaped in our string
                        }
                        currentWord.append(currentChar).append('\'');
                        i++;
                        continue;
                    }
                    else if (currentChar == '\'')
                    {
                        // This is single quote that wasn't escaped, so it's either the beginning or end of a string
                        insideString = !insideString;
                    }
                }

                currentWord.append(currentChar);

                if (lastChar)
                {
                    // This char won't be a comma (our previous word-boundary delimiter) if this SQL is well-formed,
                    // but we still want to capture this final value
                    valList.add(currentWord.toString());
                }
            }
        }

        // Sanity check that the number of values we found matches the number of columns for this table
        if (valList.size() != numValues)
        {
            throw new BadMatchException(String.format(
                "Expected to find %d values in statement, but only found %d\n\tsource: [%s]\n\tvalues: [%s]",
                numValues, valList.size(), values, String.join("|", valList)
            ));
        }

        return valList;
    }

    private String escapeQuotes(@Nonnull String stmt)
    {
        return stmt.replace("'", "''");
    }

    private String quoteStmt(@Nonnull String stmt)
    {
        return "'" + stmt + "'";
    }
}