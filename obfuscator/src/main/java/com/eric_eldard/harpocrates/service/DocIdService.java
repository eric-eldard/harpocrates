package com.eric_eldard.harpocrates.service;

import java.util.concurrent.ThreadLocalRandom;

public class DocIdService
{
    public static final DocIdService INSTANCE = new DocIdService();

    public String makeDocId(String mask)
    {
        char[] docId = new char[mask.length()];
        for (int i = 0; i < mask.length(); i++)
        {
            char maskChar = mask.charAt(i);
            if (Character.isDigit(maskChar))
            {
                docId[i] = (char) (ThreadLocalRandom.current().nextInt(9) + '0');
            }
            else
            {
                docId[i] = (char) (ThreadLocalRandom.current().nextInt(26) + 'A');
            }
        }
        return new String(docId);
    }

    private DocIdService()
    {
        // singleton ctor
    }
}