package com.eric_eldard.harpocrates.demo;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.eric_eldard.harpocrates.persistence.DataClassifierImpl;

public class DemoApp
{
    public static void main(String[] args)
    {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DemoAppConfig.class);
        assert context.getBeansOfType(DataClassifierImpl.class).isEmpty(); // verify DataClassifier removed itself
    }
}