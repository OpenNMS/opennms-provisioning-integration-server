# OpenNMS Provisioning Integration Server
The provisioning integration server (pris) is a software which provides the ability to get external information from your inventory into an OpenNMS requisition model. The output from pris is provided as XML over HTTP and can be used in OpenNMS Provisiond to import and discover nodes from.

![OpenNMS pris overview](images/pris-overview.png "OpenNMS pris overview")

The service is used to provide an integration point for OpenNMS external inventories or homebrew inventories. With pris the data is normalized to the OpenNMS requisition model and can be consumed from OpenNMS provisiond. It is highly specialized to enrich the requisition model with SNMP information i.e. SNMP interfaces, generic SNMP attributes like system location, contact and the system description. Beside that Provisiond can also run service detectors against IP interfaces and allows to run policies to control the monitoring behavior. Pris is an instance in front of Provisiond, it allows to aggregate information from different sources and manipulate them in a flexible way.

* What is a requistion
* How is the structure in the file system

## Driver
The driver is responsible for the overall way the integration is done. Two different drivers are availible:

### File driver
This driver offers the ability to call the integration application and get requisition files as an result. This driver requires the following parameters in the "global.properties":

* driver = file (selects the file driver)
* target = /tmp/requisitions (the folder to store the requisition file)
* requisitions = * (a filter for the requisitions to generate)

### HTTP driver
This driver starts up a web server and provides up-to-date requisitions on a http request base. This driver requires the following parameters in the "global.properties":

* driver = http (points to this driver)
* host = 127.0.0.1 (the ip for the http-server)
* port = 8000 (the port used for the http-server)

## Sources
* What are sources

### merge-source
| source name             | custom mapper                   |
|-------------------------|--------------------------------:|
| requisitionMerge.source | provides a requisition directly |

| parameter                  | required | description         |
|----------------------------|:--------:|--------------------:|
| requisition.A.url          | *        |                     |
| requisition.A.username     |          |                     |
| requisition.A.password     |          |                     |
|                            |          |                     |
| requisition.B.url          | *        |                     |
| requisition.B.username     |          |                     |
| requisition.B.password     |          |                     |
|                            |          |                     |
| requisition.merge.keepAllA |          | if this parameters is present in the config all nodes from requisition A will be present in the resulting requisition. |
| requisition.merge.keepAllB |          | if this parameters is present in the config all nodes from requisition B will be present in the resulting requisition. |

This source is reading two already defined requisitions via http and merges them into one new requisition. By default the resulting requisition will contain all nodes that are present in both requisitions, identified by the foreignId. The A-Node (from requisition A) is enriched with the data from B-Node.


### XLS Source
The "xls.source" is abel to read xls spreadsheet files and create an OpenNMS requisition based on its content.

| source name | custom mapper                   |
|-------------|--------------------------------:|
| xls.source  | provides a requisition directly |

| parameter | required | description                      |
|-----------|:--------:|---------------------------------:|
| xls.file  | *        | the path of the xls file to read |

The structure of the spreadsheet has to follow this rules. The source is reading from a sheet named like the requisition you are requesting. The first row of the sheet is reserved to column-names. This column-names have to start with certen prefixes to be recognized.

| prefixes  | required | description                      |
|-----------|:--------:|---------------------------------:|
| `Node_`     | * | will be interpreted as nodelabel and foreignId |
| `IP_`       | * | will be interpreted as an ipaddress as a new interface on the node |
| `IfType_` | * | is interpreted as snmp-primary flag. Thas controlls the snmp behavior. Valid are `P`, `S` and `N`. |
| `cat_`     | | will be interpreted as a surrvailance-category. Multiple comma seperated categories can be provided. It can be used multiple times per sheet.|
| `svc_`     | | will be interpreted as a service on the interface of the node. Multiple comma seperated services can be provided. It can be used multiple times per sheet.|

This source also supports all asset-fields by using `Asset_` as a prefix followed by the `asset-field-name`. The city field of the assets can be addressed like this: `Asset_City`. This is not case-sensitive.

To add a node with multiple interfaces, add an additional sequent row with the same nodelabel (Node_). This row will be added as a new interface based on the data from the  IP_, IfType_, svc_ columns.

The order in which the columns are arranged is irelevant. Also additional columns can be pressent.

### opennms-requisition Source
This source is reading a already defined requisition via http. If username an password is provided it will be used as basic auth credentions.

| source name         | custom mapper                   |
|---------------------|--------------------------------:|
| requisition.source  | provides a requisition directly |

| parameter       | required | description                      |
|-----------------|:--------:|---------------------------------:|
| requisition.url | *        | the path of the xls file to read |
| requisition.username | | |
| requisition.password | | |

### JDBC Source
The jdbc source provides the ability to run an SQL-Query against an external system and interpret the result as an OpenNMS requisition.

| source name | custom mapper                   |
|-------------|--------------------------------:|
| jdbc.source | provides a requisition directly |

| parameter            | required | description                     |
|----------------------|:--------:|--------------------------------:|
| jdbc.driver          | * | |
| jdbc.url             | * | |
| jdbc.selectStatement | * | |
| jdbc.user            |   | |
| jdbc.password        |   | |

This source also supports all asset-fields by using `Asset_` as a prefix followed by the `asset-field-name`. The city field of the assets can be addressed like this: `yourvalue AS Asset_City`. This is not case-sensitive.

### OCS Source
OCS is handling computers and snmp-devices separately in its APIs. For that reason there are two different sources available to connect to OCS. Some parameters are part of both sources and described first.

#### general ocs parameters
The following parameters are **required**:

* ocs.url = The url of the OCS webapplication.
* ocs.username = A OCS user with rights to access the OCS Soap interface.
* ocs.password = The password for the OCS user with rights to access the OCS Soap interface.
* ocs.checksum = The ocs.checksum parameter controls how detailed the data is that the integration is requesting from the OCS. It is important to request all the data you want to map into your requisition but not to much, cause a high checksum causes the request to be significantly slower. Read the [OCS Web-Services](http://wiki.ocsinventory-ng.org/index.php/Developers:Web_services) documentation for more information. The default checksum for the defaultmappers is 4099.

The follwoing parameter is **optional**:

* ocs.tags = OCS supports tags / custom fields. If a tag is added to the ocs.tags list, just computers and snmpDevices that are marked with all the tags will be read from the OCS. This feature can be used to tag computers as "testing" or "production".

#### source (ocs.computers)
This source is reading computers from a OCS instance. It supports all parameters listed as general and the following additions:

* ocs.accountinfo = Accountinfo data is based on custom fields managed in OCS. There are managed by the Administrative-Data section of the webui. The name of the custom field is presented in all caps. The value of the field as provided by the user. The ocs.accountinfo parameters supports a list of accountinfos that has to be present on the computer. If one of the accountinfos is not present the computers is skipped. To add multiple accountinfos separate them with spaces.

#### source (ocs.snmpDevices)
This source is reading snmpDevices from a OCS instance. It supports all parameters listed as general and no additional at the moment.

### Mock Sources
For development and testing there are ocs.computers.replay and ocs.snmpDevices.replay sources available. This sources require a file that contains the computers or snmpDevices as xml file. The file has also be referenced in the configuration.

## Mapper

### Mapper for specific sources
Mappers are used to map the OCS datamodel with computers and snmp-devices to the OpenNMS datamodel for provisioning with nodes, interfaces, services and assets. The OCS integration provides one default mapper for computers and one for snmp-devices out of the box. Additionally it provides script based mapping via the script mapper.

#### OCS Mappers
The default mappers for OCS are a simple way to map computers and snmp-devices to OpenNMS nodes.

##### Computers
To use this mapper, configure your requisition config to use "default.ocs.computers" as mapper. This mapper requires a ocs.checksum of 4099 to get all required data. It elects one of the ip-addresses of a computer to be the management-interface of the node. This is controlled by the black- and whitelisting. The default ip-filter is used for the election. If no interface is valid, the node will have no interfaces and a corresponding log message is written. The elected management-interface is enriched with the interface description, if available. The SNMP and ICMP service are forced to the management-interface. Additionally the comment field of the node assets are used to provide a html link to the computer-page of the ocs instance. The assets for cpu and operationgSystem will be mapped from the OCS computer too. The computer name is used as foreignId and nodeLable.

###### CategoryMap
The Default mapper for Computers supports a mapping between OCS Accountinfo data from OCS to OpenNMS surveillance-categories. To use this feature add the categoryMap parameter to the requisition.properties file and reference a properties file following this syntax example:
 
* ADMINISTRATIVEDATAFILEDNAME.data=OpenNMSCategoryName
* ENVIRONMENT.Production=Production JOB.Mailserver=Mail

##### snmp-devices
To use this mapper, configure your requisition config to use "default.ocs.snmpDevices" as mapper. This mapper requires a ocs.checksum of 4099. It validates the ip-address of the snmpDevice verses the black- and whitelists. For the election the default ip-fiter is used. If the ip-address of the snmpDevice is "blocked" a log message is written and the node will not have any interfaces. The interface is equipped with ICMP and SNMP as services. The foreignId of is mapped with the OCS id of the snmpDevice. The nodeLable is provided by the OCS name of the snmpDevice. The assets for cpu and operationSystem are mapped against OCS. Additionally a link to the OCS snmpDevice page is added to the asset comment field.

##### Black- and Whitelists
The OCS Integration supports Black- and Whitelists to control the selection of the management-interface for the node. OCS it self dose not define a management-interface, it just selects one ip-address as default and maintains a networks-list for every computer. For the election of the management-interface two ip-filters are implemented in the IpInterfaceHelper-class. Both read the black- and whitelist from the requisition configuration folder. Name them "blackList.properties" and "whiteList.properties". Every line in those files is interpreted as an IPLike statement to offer ranges.

###### Default ip-filter
This filter is accepting every ip-address as valid that is not blacklisted. Ip-addresses that are white-listed are preferred over not listed ip-addresses.

###### Computers
The first ip-address of the ocs-networks-list that is white-listed is used. If no ip-address of the ocs-networks-list is white-listed the first not ip-address that is not black-listed is elected as management interface. If no ip-address of the ocs-networks-list qualifies, the ocs-default-ip is checked against the black-list. If it is not black-listed, it is elected as management-interface (no interface description will be available). If it is black-listed, no interface is added to the node. (selectManagementNetwork)

###### SnmpDevices
The ip-address of a SnmpDevice is elected as management-interface as long as it is not black-listed. If it is black-listed no interface is added to the node. (selectIpAddress)

###### Strict ip-filter "WhiteAndBackOnly"
This filter is as strict black- and white-list approach. Computers and SnmpDevices are handled independently.

###### Computers
This mode is just accepting ip-addresses that are white-listed and not black-listed. If there are multiple ip-addresses listed on ocs-networks-list that are white-listed but and not black-listed, the first one is selected as management-ip. If no ip-address from the ocs-networks-list matches the black- and whitelist, the ocs-default-ip is tested against the black- and whitelist. If the ocs-defaul-ip is white-listed and not black-listed it is elected as management-ip. If no ip-address matches the black- and whitelist, no interface is added to the node. If the ocs-default-ip is selected, the interface of the node will not contain any additional parameters like description. (selectManagementNetworkWhiteAndBlackOnly)

###### SnmpDevices
If the ip-address of the snmpDevice is white-listed and not black-listed it is elected as management-interface. If the ip-address is not passing the lists, no interface is added to the node. (selectIpAddressWhiteAndBlackOnly)

###### IPLike expressions in lists
In both lists the IPLike syntax can be used to express ip-ranges and wildcards. Follow the IPLike description at [IPLIKE documentaion](http://www.opennms.org/wiki/IPLIKE).



### Null.Mapper
The null.mapper is a special mapper the just provides an empty requisition without any computer or snmpDevice mapping. It is mend to be used only in combination with the script-mapper. To use it configure the requisition.properties file parameter "mapper" with the value "null.mapper".

### Mapper scripts
The script mapper is an additional mapping step to customize the default mapping for computers or snmp devices to OpenNMS nodes. If activated the script mapper runs after the normal mapping step. The script mapper starts with the "premapped" requisition and can access all the computers / snmp-devices provided by the source. To use a the script mapper add a "script" property to the requisition.properties file and reference the script file you want to run after the default mapping has been applied. The mapper-script will be executed by Apache-BSF. By default Groovy 2.1.6 and Beanshell 2.0b5 are supported.

The mapper-script is provided with the following parameter:

* A Path object called script that points the the script file
* A Object called data, that contains a Computers or SnmpDevices object, depending on the source that is used.
* A Requisition object called requisition with the mappings result from the mapper.
* A Logger object called logger from the slf4j project.
* A Configuration object called config that contains all configured parameters, from the apache.commons.configuration framework.
* An instance of the IpInterfaceHelper called ipInterfaceHelper, that provides the black- and whitelisting.

The mapper-script has the provide a Requisition object as its result. For every request of a requisition the mapper-script is reloaded. As a reference the folders "src/examples/advanced/computers" and "src/examples/advanced/snmpDevices" are containing mapper.groovy scripts to demonstrate implementations. If no "premapping" is required use the "null.mapper" mapper. It will provide an empty requisition.

## Scripts
* What are scripts
* Chaining scripts

## Integration in OpenNMS Provisiond