## Mapper
### Null.Mapper
The `null.mapper` is a special mapper the just provides an empty requisition without any `computer` or `snmpDevice` mapping.

### Mapper scripts
The script mapper is a step to customize the behavior to assign data from the given source to the OpenNMS requisition model. The script mapper starts with the "pre-assigned" requisition and can access all data provided by the source. To use a the script mapper add a "script" property to the requisition.properties file and reference the script file you want to run after the default mapping has been applied. The mapper-script will be executed by Apache-BSF. By default Groovy 2.1.6 and Beanshell 2.0b5 are supported.

The mapper-script is provided with the following parameter:

* A Path object called script that points the the script file
* A Object called data, that contains a Computers or SnmpDevices object, depending on the source that is used.
* A Requisition object called requisition with the mappings result from the mapper.
* A Logger object called logger from the slf4j project.
* A Configuration object called config that contains all configured parameters, from the apache.commons.configuration framework.
* An instance of the IpInterfaceHelper called ipInterfaceHelper, that provides the black- and whitelisting.

The mapper-script has the provide a Requisition object as its result. For every request of a requisition the mapper-script is reloaded. As a reference the folders "src/examples/advanced/computers" and "src/examples/advanced/snmpDevices" are containing mapper.groovy scripts to demonstrate implementations. If no "premapping" is required use the "null.mapper" mapper. It will provide an empty requisition.
