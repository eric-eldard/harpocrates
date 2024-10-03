package com.eric_eldard.harpocrates.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
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
            Pattern tablePattern  = Pattern.compile("CREATE TABLE (`.*?`) \\(");
            Pattern columnPattern = Pattern.compile("\s*(`.+?`) .+ COMMENT 'dataClassification=(.+?)',?");
            Pattern insertPattern = Pattern.compile("INSERT INTO (`.+?`) \\((.+?)\\) VALUES \\((.+?)\\);");
            Pattern enablePattern = Pattern.compile(".*ALTER TABLE (`.+?`) ENABLE KEYS.*");

            String currentTable = null;
            Map<String, DataDefinition> currentDataDefs = null;

            String readLine = reader.readLine();
            while (readLine != null)
            {
                String writeLine = readLine;

                // TODO short-circuit matching
                Matcher insertMatcher = insertPattern.matcher(writeLine);
                Matcher tableMatcher  = tablePattern.matcher(writeLine);
                Matcher columnMatcher = columnPattern.matcher(writeLine);
                Matcher enableMatcher = enablePattern.matcher(writeLine);

                if (tableMatcher.find())
                {
                    if (currentTable != null)
                    {
                        throw new BadMatchException("Table def found [" + columnMatcher.group() +
                            "], but we're currently examining a different table: [" + currentTable + ']');
                    }
                    currentTable = tableMatcher.group(1);
                    currentDataDefs = new HashMap<>();
                }
                else if (columnMatcher.find())
                {
                    if (currentTable == null)
                    {
                        throw new BadMatchException("Column def found without table: [" + columnMatcher.group() + ']');
                    }
                    String colName = columnMatcher.group(1);
                    String encodedDataDef = columnMatcher.group(2);
                    currentDataDefs.put(colName, decodeDataDef(encodedDataDef));
                }
                else if (insertMatcher.find())
                {
                    writeLine = obfuscateInsertStmt(insertMatcher, currentTable, currentDataDefs);
                }
                else if (enableMatcher.find())
                {
                    if (!enableMatcher.group(1).equals(currentTable))
                    {
                        throw new BadMatchException("End of table encountered [" + enableMatcher.group() +
                            "], but it doesn't match the current table [" + currentTable + ']');
                    }
                    currentTable = null;
                    currentDataDefs = null;
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
        List<String> data = Arrays.asList(insertMatcher.group(3).split(",")); // TODO problem when data contains comma

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
            "VALUES (.+)",
            String.format("VALUES (%s)", String.join(",", data))
        );

        return obfuscatedStmt;
    }

    private String obfuscateDatum(DataDefinition dataDefinition)
    {
        DataType type = dataDefinition.getType();
        String pattern = type == DataType.CUSTOM ? dataDefinition.getPattern() : type.getPattern();

        if (pattern == null)
        {
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

        return "'" + pattern + "'";
    }

    private static String obfuscateDocIds(String pattern)
    {
        Pattern docIdPattern = Pattern.compile("#\\{([0A]+?)}");
        Matcher matcher = docIdPattern.matcher(pattern);
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
}