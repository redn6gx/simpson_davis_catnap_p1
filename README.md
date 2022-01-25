# Catnap

Simple JPA compliant ORM for Java.

## Class Structure

The following is a list of the classes used within the framework:

 - Session (implements EntityManager)
 - SessionFactory (implements EntityManagerFactory)
 - Cache
 - Transaction (implements EntityTransaction)
 - ConnectionPool
 - MappingStrategy (should have an interface)

### Session
This object is used to perform CRUD operations on the database by a service. In Hibernate's
terminology this object is the "persistence context".

### SessionFactory
This object is used to obtain instances of the Session object and manages all persistence contexts
within the application. This is what Hibernate calls the "persistence unit".

### Cache
This object is used to cache results obtained from the database. If we are doing an l1 type
cache than the Session object holds a reference to it. For l2 the SessionFactory would.

### Transaction
This object represents a single unit of work to be done by the database. Transactions objects
are created and maintained by the Session object. As a service makes calls to the Criteria API
of the Session it makes a record of the database operations that need performing in the Transaction,
and then the Transaction is committed.

### ConnectionPool
This object manages a list of used and free JDBC connection objects that are given to
Session objects during runtime. 

### MappingStrategy
This object performs the logic of looking up annotations in domain models and mapping
them to database schema.