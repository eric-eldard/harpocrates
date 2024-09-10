package com.eric_eldard.harpocrates.demo;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class DemoApp
{
    public static void main(String[] args)
    {
        new AnnotationConfigApplicationContext(DemoAppConfig.class);
    }
}