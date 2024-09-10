package com.eric_eldard.harpocrates.service;

import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import com.eric_eldard.harpocrates.util.ResourceUtils;

public class NameService
{
    public static final NameService INSTANCE = new NameService();

    private final List<String> GIVEN_NAMES;

    private final List<String> SURNAMES;

    @SneakyThrows
    private NameService()
    {
        Scanner gnScanner = ResourceUtils.makeFileScanner("data/given_names.txt");
        List<String> readInGnList = new ArrayList<>();
        while (gnScanner.hasNextLine())
        {
            readInGnList.add(gnScanner.nextLine());
        }
        GIVEN_NAMES = List.copyOf(readInGnList);

        Scanner snScanner = ResourceUtils.makeFileScanner("data/surnames.txt");
        List<String> readInSnList = new ArrayList<>();
        while (snScanner.hasNextLine())
        {
            readInSnList.add(snScanner.nextLine());
        }
        SURNAMES = List.copyOf(readInSnList);
    }

    public String makeGivenName()
    {
        return GIVEN_NAMES.get(ThreadLocalRandom.current().nextInt(GIVEN_NAMES.size()));
    }

    public String makeSurname()
    {
        return SURNAMES.get(ThreadLocalRandom.current().nextInt(SURNAMES.size()));
    }
}
