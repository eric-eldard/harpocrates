package com.eric_eldard.harpocrates.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

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
            String readLine = reader.readLine();
            while (readLine != null)
            {
                String writeLine = readLine;

                // TODO perform the obfuscation

                writer.write(writeLine);
                writer.newLine();

                readLine = reader.readLine();
            }
        }
    }
}