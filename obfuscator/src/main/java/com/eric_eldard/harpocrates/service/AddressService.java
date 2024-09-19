package com.eric_eldard.harpocrates.service;

import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import com.eric_eldard.harpocrates.util.ResourceUtils;

public class AddressService
{
    public static final AddressService INSTANCE = new AddressService();

    private final List<String> STREET_ADDRESSES;

    private final List<String> CITIES;

    private final List<String> STATES;

    private final List<String> ZIPS;

    @SneakyThrows
    private AddressService()
    {
        Scanner scanner = ResourceUtils.makeFileScanner("data/us_post_offices.txt");
        List<String> readInAddressList = new ArrayList<>();
        List<String> readInCityList = new ArrayList<>();
        List<String> readInStateList = new ArrayList<>();
        List<String> readInZipList = new ArrayList<>();
        while (scanner.hasNextLine())
        {
            String[] split = scanner.nextLine().split(",");
            readInAddressList.add(split[0]);
            readInCityList.add(split[1]);
            readInStateList.add(split[2]);
            readInZipList.add(split[3]);
        }
        STREET_ADDRESSES = List.copyOf(readInAddressList);
        CITIES = List.copyOf(readInCityList);
        STATES = List.copyOf(readInStateList);
        ZIPS = List.copyOf(readInZipList);
    }

    public String makeStreetAddress()
    {
        return STREET_ADDRESSES.get(ThreadLocalRandom.current().nextInt(STREET_ADDRESSES.size()));
    }

    public String makeCity()
    {
        return CITIES.get(ThreadLocalRandom.current().nextInt(CITIES.size()));
    }

    public String makeState()
    {
        return STATES.get(ThreadLocalRandom.current().nextInt(STATES.size()));
    }

    public String makeZip()
    {
        return ZIPS.get(ThreadLocalRandom.current().nextInt(ZIPS.size()));
    }

    public String makeAddress()
    {
        int i = ThreadLocalRandom.current().nextInt(STREET_ADDRESSES.size());
        return STR."\{STREET_ADDRESSES.get(i)}, \{CITIES.get(i)}, \{STATES.get(i)}  \{ZIPS.get(i)}";
    }
}
