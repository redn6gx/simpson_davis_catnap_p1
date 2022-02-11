# Catnap
---

## Description
---
Catnap is a Custom ORM inspired by Hibernate. It makes use of annotation based model configuration to map entities in the database to declared models in a web application. There is a mapping.properties file that is required to map Java types to PostgreSQL types, allowing the user to customize what types they want mapped. Catnap also has support for a session based cache as well as connection pooling.

## Technologies Used

 * PostgreSQL JDBC Driver - Version 42.3.1
 * JUnit - Version 5.8.2
 * Mockito - Version 4.2.0
 * Mockito JUnit - Version 4.3.1
 * Log4j - Version 2.14.1
 * Java Servlet API - 4.0.1
 * Custom Web Application LetSleepingCatsLie - found at https://github.com/redn6gx/simpson_davis_let_sleeping_cats_lie/tree/main

## Features
---
Completed feature list:
 * Annotation support for primary keys, field ordering, String length restrictions, and entity table names
 * Session based caching
 * Connection Pooling
 * Basic transaction support (begin, commit, and rollback)

To-dos:
 * Entity relationships: OneToOne, OneToMany, etc.
 * Column names
 * Transaction savepoints


## Getting Started
---
First clone this repository's main branch using:
```
git clone https://github.com/redn6gx/simpson_davis_catnap_p1.git
```
then navigate to the project folder and run maven package:
```
mvn package
```
Once you have the jar filed you're free to use it in your web application! Make sure to use a maven local repository and add the following to your maven dependencies:
```
<dependency>
    <groupId>com.revature</groupId>
    <artifactId>catnap</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Usage
---
To use Catnap you need to instantiated ```SessionFactory``` in your application start up process. It takes ```ConnectionPool``` and ```MappingStrategy``` as arguments, so you instantiate those two first (supplying the former with your database credentials) and then pass them into the constructor of ```SessionFactory```. From there your database manipulating objects can call ```getSessionContext``` on the ```SessionFactory```, passing in the web session id (such as from ```HttpSession```'s ```getId``` method) and it will return a ```Session``` instance that can be used to perform database operations. ```Session``` supports the following operations:
 * ```persist```: save the entity to the database
 * ```update```: update the entity in the database
 * ```delete```: delete an entity from the database
 * ```get```: get an entity from the database
 * ```getAll```: get all entities from a single table
 * ```beginTransaction```: begin a transaction
 * ```commit```: commit a transaction
 * ```rollback```: rollback a transaction

## Contributors
---
 * Richard Simpson - https://github.com/RichardSimpson235
 * Robert Davis - https://github.com/redn6gx
