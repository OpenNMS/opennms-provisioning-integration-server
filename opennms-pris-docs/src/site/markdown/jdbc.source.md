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

The following column-headers will be mapped from the result set to the OpenNMS requisitoin:

| column-header    | required | description                        |
|------------------|:--------:|-----------------------------------:|
| Foreign_Id       | * | will be interpreted as `foreignId` on the node |
| IP_Address       |   | will be interpreted as an IP address as a new interface on the node |
| MgmtType         |   | is interpreted as `snmp-primary` flag and controls how the interface can be used to communicate with the SNMP agent. Valid are `P` (Primary), `S` (Secondary) and `N` (None). |
| Node_Label       |   | will be interpreted as node label for the node identified by the `Foreign_Id`|
| Cat              |   | will be interpreted as a surveillance-category for the node identified by the `Foreign_Id`.
| Svc              |   | will be interpreted as a service on the interface of the node identified by the `Foreign_Id` and `IP_Address` field.|

This source also supports all asset-fields by using `Asset_` as a prefix followed by the `asset-field-name`. The city field of the assets can be addressed like this: `yourvalue AS Asset_City`. This is not case-sensitive.

Every row of the result set will be checked for the listed column-headers. The provided data will be added to the corresponding node. Multiple result rows with matching `Foreign_Id` will be added to the corresponding node.
