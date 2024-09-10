package com.eric_eldard.harpocrates.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.eric_eldard.harpocrates.annotation.DataClassification;

@Getter
@AllArgsConstructor
public enum DataType
{
    /**
     * A randomly assembled, non-real post office address (see also {@link #FULL_ADDRESS})
     */
    ADDRESS("{STREET_ADDRESS}, {CITY}, {STATE}, {ZIP_CODE}"),

    /**
     * A randomly selected US city
     */
    CITY("{CITY}"),

    /**
     * A randomly selected date between 1900-01-01 and now. The replacement will be an actual java.util.Data object.
     */
    DATE("{DATE}"),

    /**
     * A fake email address built from a randomly paired {@link #GIVEN_NAME} and {@link #SURNAME}
     */
    EMAIL_ADDRESS("{GIVEN_NAME}.{SURNAME}@test.com"),

    /**
     * The full postal address of a randomly selected US post office (CITY matches STATE matches ZIP_CODE, etc.)
     */
    FULL_ADDRESS("{FULL_ADDRESS}"),

    /**
     * A randomly paired {@link #GIVEN_NAME} and {@link #SURNAME}
     */
    FULL_NAME("{GIVEN_NAME} {SURNAME}"),

    /**
     * A randomly selected first name
     */
    GIVEN_NAME("{GIVEN_NAME}"),

    /**
     * Hard-coded loopback address
     */
    IP_ADDRESS("127.0.0.1"),

    /**
     * Visa test card number for response "SUCCESS"
     */
    PAYMENT_CARD("4111111111111111"),

    /**
     * A 555 number with a real area code and random last 4
     */
    PHONE_NUMBER("{PHONE_NUMBER}"),

    /**
     * A randomly selected 2-digit code for a US state or province
     */
    STATE("{STATE}"),

    /**
     * The street address of a real US post office
     */
    STREET_ADDRESS("{STREET_ADDRESS}"),

    /**
     * An invalid, but well-formed US Social Security Number
     */
    SSN("{SSN}"),

    /**
     * A randomly selected last name
     */
    SURNAME("{SURNAME}"),

    /**
     * A randomly selected US zip code
     */
    ZIP_CODE("{ZIP_CODE}"),

    /**
     * Always {@link Action#IGNORE}
     */
    NOT_SENSITIVE(null),

    /**
     * Create your own pattern.
     * <br><br>
     * @see DataClassification#pattern()
     */
    CUSTOM(null);

    private final String pattern;
}