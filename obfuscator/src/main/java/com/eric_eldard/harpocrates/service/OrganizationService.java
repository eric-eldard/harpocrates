package com.eric_eldard.harpocrates.service;

import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import com.eric_eldard.harpocrates.util.ResourceUtils;

public class OrganizationService
{
    public static final OrganizationService INSTANCE = new OrganizationService();

    private final List<String> ORGANIZATIONS;

    @SneakyThrows
    private OrganizationService()
    {
        Scanner scanner = ResourceUtils.makeFileScanner("data/organizations.txt");
        List<String> readInOrgList = new ArrayList<>();
        while (scanner.hasNextLine())
        {
            readInOrgList.add(scanner.nextLine());
        }
        ORGANIZATIONS = List.copyOf(readInOrgList);
    }

    public String makeOrganization()
    {
        return ORGANIZATIONS.get(ThreadLocalRandom.current().nextInt(ORGANIZATIONS.size()));
    }
}