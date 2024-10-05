package com.eric_eldard.harpocrates.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.eric_eldard.harpocrates.demo.config.DemoAppConfig;
import com.eric_eldard.harpocrates.demo.model.Customer;
import com.eric_eldard.harpocrates.demo.model.User;
import com.eric_eldard.harpocrates.demo.persistence.CustomerRepository;
import com.eric_eldard.harpocrates.demo.persistence.UserRepository;
import com.eric_eldard.harpocrates.persistence.DataClassifierImpl;

public class DemoApp
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoApp.class);

    public static void main(String[] args)
    {
        LOGGER.info("Loading Spring config...");

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DemoAppConfig.class);
        assert context.getBeansOfType(DataClassifierImpl.class).isEmpty(); // verify DataClassifier removed itself

        LOGGER.info("Creating test data...");

        UserRepository userRepo = context.getBean(UserRepository.class);
        CustomerRepository customerRepo = context.getBean(CustomerRepository.class);

        for (int i = 0; i < 20; i++)
        {
            userRepo.save(User.builder()
                .givenName("SecretFirstName" + i)
                .surname("SecretLastName" + i)
                .fullName("SecretFirstName" + i + " SecretLastName" + i)
                .email("secret.email." + i +"@pii.com")
                .email2("secret.alt.email." + i +"@pii.com")
                .phoneNumber("202-000-0000")  // faker than the replacement data
                .streetAddress("123 Private Street")
                .city("Private Town")
                .state("PII")
                .zip("99999")  // faker than the replacement data
                .dob(new Date(1248613200000L))
                .ssn("000-00-0000")  // faker than the replacement data
                .ipAddress("192.168.1.1")
                .build()
            );

            // Tests having more than one table in original and obfuscated dumps
            customerRepo.save(Customer.builder()
                .fullAddress("123 Private St Private Town PII 99999")
                .randomAddress("123 Private St Private Town PII 99999")
                .organization("Super Secret Co")
                .paymentCard("0000-0000-0000-0000") // faker than the replacement data
                .removeThisText("This is so secret...we better not see anything in the output")
                .ignoreThisText("This text should appear in the obfuscated dump'") // tests trailing '
                .notSensitiveText("This text was never sensitive, but enjoy this comma") // tests , in text
                .docId("This text should be replaced by a custom pattern")
                .multiDocId("This text should be replaced by a custom pattern")
                .build()
            );
        }
    }
}