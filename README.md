# Harpocrates Data Classifier &amp; Obfuscator

## About
<p>
    Harpocrates helps you classify the types of sensitive data stored in your MySQL database,
    and allows you to create obfuscated dumps with practical replacements for that data.
</p>

## Please Note
<ul>
    <li><b>Harpocrates will manipulate your database schema</b>, live, when your app starts up</li>
    <li>Harpocrates is largely a proof of concept and is a work in progress</li>
</ul>

## TODO
- [x] encode json strings
- [x] read @Table & @Column names
    - [ ] more robust logic for table/column naming pattern detection
- [ ] more robust error handling
- [ ] detection and logging of changed/removed classifications
- [ ] how to clean out old defs that are removed? use an annotation?
- [ ] shut bean down after startup?
- [ ] MySQL user for app must have permission to change the schema
    - [ ] alternate mode which can be run statically against the app?
