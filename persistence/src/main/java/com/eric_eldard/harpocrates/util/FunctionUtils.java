package com.eric_eldard.harpocrates.util;

public final class FunctionUtils
{
    /**
     * A {@link java.util.function.BinaryOperator} which always chooses the first element
     */
    public static <T> T chooseFirst(T a, T b)
    {
        return a;
    }

    private FunctionUtils()
    {
        // util ctor
    }
}