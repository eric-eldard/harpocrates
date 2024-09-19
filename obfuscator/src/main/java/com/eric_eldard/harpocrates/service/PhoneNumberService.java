package com.eric_eldard.harpocrates.service;

import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import com.eric_eldard.harpocrates.util.ResourceUtils;

public class PhoneNumberService
{
    public static final PhoneNumberService INSTANCE = new PhoneNumberService();

    private final List<String> AREA_CODES;

    @SneakyThrows
    private PhoneNumberService()
    {
        Scanner scanner = ResourceUtils.makeFileScanner("data/us_area_codes.txt");

        List<String> readInList = new ArrayList<>();
        while (scanner.hasNextLine())
        {
            readInList.add(scanner.nextLine());
        }

        AREA_CODES = List.copyOf(readInList);
    }

    public String makePhoneNumber()
    {
        String areaCode = AREA_CODES.get(ThreadLocalRandom.current().nextInt(AREA_CODES.size()));
        return STR."\{areaCode}555\{randDigit()}\{randDigit()}\{randDigit()}\{randDigit()}";
    }

    private int randDigit()
    {
        return ThreadLocalRandom.current().nextInt(10);
    }
}