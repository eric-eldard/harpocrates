package com.eric_eldard.harpocrates.enumeration;

/**
 * The action to perform during obfuscation
 */
public enum Action
{
    /**
     * Do not alter this value in the dump
     */
    IGNORE,

    /**
     * null out this value in the dump
     */
    REMOVE,

    /**
     * Replace value in the dump using a pattern from {@link DataType}
     */
    REPLACE
}