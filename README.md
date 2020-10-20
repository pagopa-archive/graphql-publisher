# Graphql Publisher - Expose hive/impala tables using Graphql

## Introduction
This project aims to expose via a Graphql endpoint the data managed by either Hive or Impala.
It uses JDBC introspection capabilities for dynamically generating a Graphql schema for all the tables associated with a given database.
The generated schema tries to follow the specification defined by the [Hasura](https://github.com/hasura/graphql-engine) platform. You can find the SQL to Graphql mapping [here](https://hasura.io/docs/1.0/graphql/core/schema/index.html#schema).

## How to use it
Once cloned the project, go to the project root and in the configuration file etc/application.conf set the proper JDBC URL. After, just run: 
```
sbt run -Dconfig.file=etc/application.conf
```
and open the browser here: [http://localhost:8088/graphql/ui](http://localhost:8088/graphql/ui). 
You should then log in using your Hive/Impala credentials and choose the database you'd like to expose.