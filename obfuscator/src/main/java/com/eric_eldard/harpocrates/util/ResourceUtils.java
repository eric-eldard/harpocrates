package com.eric_eldard.harpocrates.util;

import lombok.SneakyThrows;

import java.util.Scanner;

import org.springframework.core.io.ClassPathResource;

public final class ResourceUtils
{
    @SneakyThrows
    public static Scanner makeFileScanner(String classpathLocation)
    {
        return new Scanner(new ClassPathResource(classpathLocation).getInputStream());
    }

    private ResourceUtils()
    {
        // util ctor
    }
}