package com.eric_eldard.harpocrates.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

import com.eric_eldard.harpocrates.enumeration.Action;
import com.eric_eldard.harpocrates.enumeration.DataType;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataClassification
{
    @AliasFor("type")
    DataType value() default DataType.OTHER;

    /**
     * What kind of data is this
     */
    @AliasFor("value")
    DataType type() default DataType.OTHER;

    /**
     * How should the obfuscator treat this piece of data
     */
    Action action() default Action.REPLACE;

    /**
     * A custom replacement pattern using any combination of hard-coded strings, placeholders for existing
     * {@link DataType}s, and/or placeholders for random numbers or letters (specified as a series of 0s and As, like
     * <code>"#{A00}"</code>).
     * <br><br>
     * Examples:
     * <ul>
     *     <li>custom email: <code>"{SURNAME}.{GIVEN_NAME}@my-company.com"</code></li>
     *     <li>custom phone number: <code>"202-555-{0000}"</code></li>
     *     <li>custom document number: <code>"ID-#{AA000000}"</code></li>
     * </ul>
     */
    String pattern() default "";

    /**
     * An optional classification description which can be used to specify why this data is protected, especially when
     * {@link #type()} {@link DataType#OTHER} is used.
     * <br><br>
     * Examples:
     * <ul>
     *     <li>"HIPAA-uniquely-identifying"</li>
     *     <li>"Confidential (External)"</li>
     *     <li>"PCI authentication data"</li>
     * </ul>
     */
    String description() default "";
}