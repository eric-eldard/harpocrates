package com.eric_eldard.harpocrates.demo.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import com.eric_eldard.harpocrates.annotation.DataClassification;
import com.eric_eldard.harpocrates.enumeration.Action;
import com.eric_eldard.harpocrates.enumeration.DataType;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor  // JPA needs
@AllArgsConstructor // @Builder needs
public class Customer
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @DataClassification(DataType.ORGANIZATION)
    private String organization;

    @DataClassification(DataType.FULL_ADDRESS)
    private String fullAddress;

    @DataClassification(DataType.ADDRESS)
    private String randomAddress;

    @DataClassification(DataType.PAYMENT_CARD)
    private String paymentCard;

    @DataClassification
    private String removeThisText;

    @DataClassification(action = Action.IGNORE)
    private String ignoreThisText;

    @DataClassification(DataType.NOT_SENSITIVE)
    private String notSensitiveText;

    @DataClassification(action = Action.REPLACE, pattern = "ID-#{AA000000}")
    private String docId;

    @DataClassification(action = Action.REPLACE, pattern = "ID-#{AA}-#{A0}-#{00}")
    private String multiDocId;
}