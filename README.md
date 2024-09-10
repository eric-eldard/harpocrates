# Harpocrates Data Classifier &amp; Obfuscator

## About
**Harpocrates** helps you classify the types of sensitive data stored in your MySQL database,
and allows you to create obfuscated dumps with practical replacements for that data.

### Please Note
<ul>
    <li><b>Harpocrates will manipulate your database schema</b>, live, when your app starts up</li>
    <li>Harpocrates is available under the MIT license and no warranty of any kind is given</li>
    <li>Harpocrates is largely a proof of concept and is a work in progress</li>
</ul>

## Build
`mvn clean install`

## Use

### Include dependencies in your app
```xml
<dependency>
    <groupId>com.eric_eldard</groupId>
    <artifactId>harpocrates-annotation</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>

<dependency>
    <groupId>com.eric_eldard</groupId>
    <artifactId>harpocrates-persistence</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```

### Annotate entity fields
```java
@Entity
public class User
{
    @Column(name = "given_name")
    @DataClassification(DataType.GIVEN_NAME)
    private String firstName;

    @Column(name = "surname")
    @DataClassification(DataType.SURNAME)
    private String lastName;

    @DataClassification(type = DataType.EMAIL_ADDRESS, pattern = "{SURNAME}.{GIVEN_NAME}@my-company.com")
    private String email;

    @DataClassification(type = DataType.PHONE_NUMBER, action = Action.REMOVE)
    private String phoneNumber;
}
```

### Run

#### With Spring

1. Set the property for the location of your entities
```properties
harpocrates.base-package-to-scan=your.entity.package
```

2. Set the property for the name of the `javax.sql.DataSource` which has access to modify the structure of your entity
tables. This defaults to "dataSource", so it's only necessary to set this prop if you've given your DataSource a custom
name, or if you wish to use a DataSource for Harpocrates separate from that used by the rest of your app (this may be
desirable, since the Harpocrates DataSource needs DDL permissions on your database).
```properties
harpocrates.datasource=myCustomDataSource
```

3. Scan Harpocrates base package
```java
@Configuration
@ComponentScan("com.eric_eldard.harpocrates")
public class MyAppConfig
{
  ...
```

Harpocrates will run automatically on Spring startup, write data from your `@DataClassification` annotations to the
corresponding column comment areas in you database, then destroy itself.

To prevent Harpocrates from shutting down after it runs, specify
```properties
harpocrates.destroy-after-exec=false
```

#### Without Spring
```java
new DataClassifierImpl(dataSource, "your.entity.package").writeClassificationsToDb();
```

## TODO

### Persistence module
- [x] encode json strings
- [x] read @Table & @Column names
    - [ ] more robust logic for table/column naming pattern detection
- [ ] more robust error handling
- [ ] detection and logging of changed/removed classifications
- [ ] how to clean out old defs that are removed? use an annotation?
- [x] shut bean down after startup?
- [ ] MySQL user for app must have permission to change the schema
    - [x] support for custom DataSource 
    - [ ] alternate mode which can be run statically against the app?

### Obfuscator module
- [ ] write it