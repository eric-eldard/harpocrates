package com.eric_eldard.harpocrates.util;

import jakarta.annotation.Nonnull;

public final class DbNameUtils
{
    /**
     * Converts camelCase and PascalCase to snake_case
     */
    public static String camelToSnake(@Nonnull String in)
    {
        // We'll most likely need more space in the array than there are characters in the input, and we'd
        // rather take a brief mem hit than risk needing to resize the array. It's all about speed.
        StringBuilder out = new StringBuilder(in.length() * 2);

        // First char gets special treatment because we definitely won't prepend the underscore to it
        char ch1 = in.charAt(0);
        out.append(Character.isUpperCase(ch1) ? Character.toLowerCase(ch1) : ch1);

        for (int i = 1; i < in.length(); i++)
        {
            char ch = in.charAt(i);
            if (Character.isUpperCase(ch))
            {
                out.append('_');
                out.append(Character.toLowerCase(ch));
            }
            else
            {
                out.append(ch);
            }
        }
        return out.toString();
    }

    /**
     * Converts snake_case to camelCase
     */
    public static String snakeToCamel(@Nonnull String in)
    {
        return toUpperHelper(in, false);
    }

    /**
     * Converts snake_case to PascalCase
     */
    public static String snakeToPascal(@Nonnull String in)
    {
        return toUpperHelper(in, true);
    }

    private static String toUpperHelper(String in, boolean initialCapital)
    {
        StringBuilder out = new StringBuilder(in.length());
        boolean capitalizeNext = initialCapital;

        for (int i = 0; i < in.length(); i++)
        {
            char ch = in.charAt(i);

            if (ch == '_')
            {
                capitalizeNext = true;
                continue;
            }

            out.append(capitalizeNext ? Character.toUpperCase(ch) : ch);
            capitalizeNext = false;
        }
        return out.toString();
    }

    private DbNameUtils()
    {
        // util ctor
    }
}