package com.eric_eldard.harpocrates.service;

import java.util.concurrent.ThreadLocalRandom;

public class SsnService
{
    public static final SsnService INSTANCE = new SsnService();

    public String makeSsn()
    {
        return
            (ThreadLocalRandom.current().nextInt(99) + 901) + "-" + // 900-999 are not valid SSN areas
            String.format("%02d", (ThreadLocalRandom.current().nextInt(99) + 1)) + "-" + // 01-99 are valid groups
            "0000"; // no valid SSNs end in 0000
    }

    private SsnService()
    {
        // singleton ctor
    }
}
