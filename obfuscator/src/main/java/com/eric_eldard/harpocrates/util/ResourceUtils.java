package com.eric_eldard.harpocrates.util;

import lombok.SneakyThrows;

import java.io.File;
import java.net.URL;
import java.util.Scanner;

public final class ResourceUtils
{
    @SneakyThrows
    public static Scanner makeFileScanner(String classpathLocation)
    {
        ClassLoader classLoader = ResourceUtils.class.getClassLoader();
        URL url = classLoader.getResource(classpathLocation);
        File file = new File(url.getFile());
        return new Scanner(file);
    }

    private ResourceUtils()
    {
        // util ctor
    }
}