package com.eric_eldard.harpocrates.persistence;

import org.springframework.beans.factory.BeanFactoryAware;

public interface DataClassifier extends BeanFactoryAware
{
    void writeClassificationsToDb();
}