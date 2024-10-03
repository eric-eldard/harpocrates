package com.eric_eldard.harpocrates.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

public class DateService
{
    private static final Long MILLISECONDS_FOR_JAN_1_1900 = -2208970800000L;

    public static final DateService INSTANCE = new DateService();

    public String makeRandomDateString()
    {
        long randomMillis = ThreadLocalRandom.current().nextLong(
            MILLISECONDS_FOR_JAN_1_1900,
            System.currentTimeMillis()
        );
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(new Date(randomMillis));
    }

    private DateService()
    {
        // singleton ctor
    }
}