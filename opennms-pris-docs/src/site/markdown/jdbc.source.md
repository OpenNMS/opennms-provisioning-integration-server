### JDBC Source
The jdbc source provides the ability to run an SQL-Query against an external system and interpret the result as an OpenNMS requisition.

    source = jdbc.source

| parameter            | required | description                     |
|----------------------|:--------:|--------------------------------:|
| jdbc.driver          | * |JDBC driver, e.g. org.postgresql.Driver|
| jdbc.url             | * |JDBC URL, e.g. jdbc:postgresql://host:port/database|
| jdbc.selectStatement | * |SQL statement |
| jdbc.user            |   |user name for database connection |
| jdbc.password        |   |password for database connection |

This source also supports all asset-fields by using `Asset_` as a prefix followed by the `asset-field-name`. The city field of the assets can be addressed like this: `yourvalue AS Asset_City`. This is not case-sensitive.
