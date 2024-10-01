package com.eric_eldard.harpocrates.runner;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.eric_eldard.harpocrates.service.ObfuscatorService;

public class ObfuscatorRunner
{
    public enum Mode
    {
        CREATE_DUMP,
        READ_DUMP;

        public static String nameListString()
        {
            return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        }
    }

    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            throw new IllegalArgumentException("No Mode specified. Please choose from " + Mode.nameListString());
        }

        Mode mode = Mode.valueOf(args[0].toUpperCase());

        File dumpFile;
        String outputFolderPath;

        if (mode == Mode.CREATE_DUMP)
        {
            if (args.length < 3)
            {
                throw new IllegalArgumentException("""
                    Too few arguments. Expected 3:
                    CREATE_GROUP /path/to/spring/application.properties /output/folder/path
                    """.stripIndent());
            }
            dumpFile = makeDump(args[1]);
            outputFolderPath = args[2];
        }
        else if (mode == Mode.READ_DUMP)
        {
            if (args.length < 3)
            {
                throw new IllegalArgumentException("""
                    Too few arguments. Expected 3:
                    READ_DUMP /path/to/existing/dump /output/folder/path
                    """.stripIndent());
            }
            dumpFile = new File(args[1]);
            outputFolderPath = args[2];
        }
        else
        {
            throw new IllegalArgumentException("Unhandled Mode " + mode);
        }

        Preconditions.checkNotNull(dumpFile);
        Preconditions.checkArgument(dumpFile.exists());
        Preconditions.checkArgument(!Strings.isNullOrEmpty(outputFolderPath));

        try
        {
            new ObfuscatorService().obfuscate(dumpFile, outputFolderPath);
        }
        catch (IOException ex)
        {
            throw new RuntimeException("Error creating obfuscated dump", ex);
        }
    }

    // TODO Provide non-Spring prop file option?
    private static File makeDump(String propertiesFileLocation)
    {
        Properties props;
        try
        {
            Resource resource = new FileUrlResource(propertiesFileLocation);
            props = PropertiesLoaderUtils.loadProperties(resource);
        }
        catch (IOException ex)
        {
            throw new IllegalArgumentException(
                "Cannot read application properties file [" + propertiesFileLocation + ']', ex);
        }

        try
        {
            return makeDump(
                props.getProperty("spring.datasource.url"),
                props.getProperty("spring.datasource.username"),
                props.getProperty("spring.datasource.password", "")
            );
        }
        catch (IOException | InterruptedException ex)
        {
            throw new RuntimeException("Error creating unobfuscated dump", ex);
        }
    }

    private static File makeDump(String url, String username, String password) throws IOException, InterruptedException
    {
        String[] hostAndPort = url.substring(url.indexOf("//") + 2, url.lastIndexOf('/')).split(":");
        String host = hostAndPort[0];
        String port = hostAndPort.length == 2 ? hostAndPort[1] : "3306";
        String dbName = url.substring(url.lastIndexOf('/') + 1);
        String nonNullPassword = Strings.isNullOrEmpty(password) ? "" : password;

        File tmpDump = File.createTempFile(dbName + "-dump-", ".sql");
        tmpDump.deleteOnExit();

        // TODO Make OS agnostic
        String[] cmd = new String[] {
            "/bin/sh",
            "-c",
            String.format("mysqldump --host=\"%s\" --port=\"%s\" --user=\"%s\" --password=\"%s\" \"%s\" > %s",
                host, port, username, nonNullPassword, dbName, tmpDump),
        };

        Process process = new ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start();
        process.getInputStream().transferTo(System.out);
        process.waitFor();

        return tmpDump;
    }
}