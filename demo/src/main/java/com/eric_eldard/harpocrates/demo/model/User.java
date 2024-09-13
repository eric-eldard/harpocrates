package com.eric_eldard.harpocrates.demo.model;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Date;

import com.eric_eldard.harpocrates.enumeration.Action;
import com.eric_eldard.harpocrates.annotation.DataClassification;
import com.eric_eldard.harpocrates.enumeration.DataType;

@Getter
@Setter
@Entity
public class User
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "lastName")
    @DataClassification(DataType.GIVEN_NAME)
    private String givenName;

    @Column(name = "lastName")
    @DataClassification(DataType.SURNAME)
    private String surname;

    @DataClassification(DataType.FULL_NAME)
    private String fullName;

    @DataClassification(DataType.EMAIL_ADDRESS)
    private String email;

    @DataClassification(DataType.PHONE_NUMBER)
    private String phoneNumber;

    @Column(name = "street_address")
    @DataClassification(DataType.STREET_ADDRESS)
    private String streetAddress;

    @DataClassification(DataType.CITY)
    private String city;

    @DataClassification(DataType.STATE)
    private String state;

    @DataClassification(DataType.ZIP_CODE)
    private String zip;

    @DataClassification(DataType.DATE)
    private Date dob;

    @DataClassification(DataType.SSN)
    private String ssn;

    @DataClassification(DataType.PAYMENT_CARD)
    private String paymentCard;

    @DataClassification
    private String removeThisText;

    @DataClassification(action = Action.IGNORE)
    private String ignoreThisText;

    @DataClassification(action = Action.REPLACE, pattern = "A#{000000}")
    private String replaceThisText;
}