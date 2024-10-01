package com.eric_eldard.harpocrates.service;

import java.util.concurrent.ThreadLocalRandom;

public class SsnService
{
    public static final SsnService INSTANCE = new SsnService();

    public String makeSsn()
    {
        return String.format("%s-%02d-0000", randArea(), randGroup()); // no valid SSNs end in 0000
    }

    /**
     * @return an invalid SSN area (900-999)
     */
    private int randArea()
    {
        return ThreadLocalRandom.current().nextInt(99) + 901;
    }

    /**
     * @return a valid SSN group (01-99)
     */
    private int randGroup()
    {
        return ThreadLocalRandom.current().nextInt(99) + 1;
    }

    private SsnService()
    {
        // singleton ctor
    }
}
