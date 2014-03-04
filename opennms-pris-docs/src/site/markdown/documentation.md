# OpenNMS Provisioning Integration Server
The provisioning integration server (_pris_) is a software which provides the ability to get external information from your inventory into an _OpenNMS_ requisition model. The output from _pris_ is provided as _XML_ over _HTTP_ and can be used in _OpenNMS Provisiond_ to import and discover nodes from. In _OpenNMS_ a requisition is set of nodes where you want to import network devices into _OpenNMS_. You can assign service detectors and policies to model the network monitoring behavior. For this reason a requisition should contain nodes which have a similar network monitoring profile. To make it easier to integrate external data sources the OpenNMS pris is introduced.

![OpenNMS pris overview](images/pris-overview.png "OpenNMS pris overview")

The service is used to provide an integration point for _OpenNMS_ external inventories or home brew inventories. With pris the data is normalized to the _OpenNMS_ requisition model and can be consumed from _OpenNMS provisiond_. It is highly specialized to enrich the requisition model with _SNMP_ information i.e. _SNMP_ interfaces, generic _SNMP_ attributes like system location, contact and the system description. Beside that _Provisiond_ can also run service detectors against IP interfaces and allows to run policies to control the monitoring behavior. _Pris_ is an instance in front of _Provisiond_, it allows to aggregate information from different sources and manipulate them in a flexible way.

_Pris_ needs at minimum two configuration files. First one is called `global.properties`, it controls the general behavior of the provisioning integration server. The second configuration file defines the requisition itself and is called `requisition.properties`. You can create multiple requisitions using different sources, by creating a directory for each requisition with a `requisition.properties` inside. The `global.properties` and the directories with your requisitions has to be in the same directory where the `java -jar opennms-pris.jar` command is called. By default we suggest to install your `opennms-pris.jar` to `/opt/opennms-pris`. If you want to provide the _OpenNMS_ requisitions via _HTTP_ from the build in _Jetty_ web server as background daemon, you can use the init script in `opennms-pris/src/examples/opennms-pris`.

## Quickstart example
To give an example we want to provide two requisitions from an poor mans inventory as _XLS file_ (myInventory.xls).  The first requisition has an worksheet containing all router and the second worksheet has all server of our network. This example can be found in `src/examples/xls.source`.

![Worksheet with Router](images/myRouter.png "Worksheet with Router")

In line 5, 6 and 7 there is an Router defined with more than one IP interface. All three interfaces will be manually provisioned. The private IP interface with _192.168.30.1_ is not used for SNMP agent communication. The services ICMP, SNMP and StrafePing are forced on some IP interfaces. For all other IP interfaces you can use the OpenNMS Provisiond mechanism scanning IP interface table from SNMP and the detectors for additional services. The server will also be categorized in _Backbone_ and _Office_.

![Worksheet with Server](images/myServer.png "Worksheet with Server")

The _OpenNMS_ requisition should be provided via _HTTP_ and we use _OpenNMS Provisiond_ to synchronize it on a regular base. We build the following file structure:

    [root@localhost opennms-pris]# clear && pwd && tree
    /opt/opennms-pris
    .
    ├── global.properties
    ├── myInventory.xls
    ├── myRouter
    │   └── requisition.properties
    ├── myServer
    │   └── requisition.properties
    └── opennms-pris.jar

Providing the _OpenNMS requisition_ over _HTTP_ we create the following `global.properties`

__File: global.properties__

    ## start an http server that provides access to requisition files
    driver = http
    host = 127.0.0.1
    port = 8000

    ### file run to create a requisition file in target
    #driver = file
    #target = /tmp/

The HTTP server is listening on localhost port 8000/TCP. We have to create two directories, each directory `myServer` and `myRouter` have a `requisition.properties` file. Both `requisition.properties` reference the main `myInventory.xls` file which contains two worksheets named _myServer_ and _myRouter_. The `requisition.properties` is for both requisitions the same. It is possible to create for each requisition different script or mapping steps.

__File: requisition.properties__

    source = xls.source
    mapper = echo.mapper
    xls.file = ../myInventory.xls

It is not necessary to restart the _pris_ server if you change property files or the _XLS_ file. All changes will be executed with the next request against the server. With the given configuration you see the result of the OpenNMS requisitions with the URL http://localhost:8000/myRouter and http://localhost:8000/myServer and can be used in _OpenNMS Provisiond_.

![Pris output for OpenNMS Provisiond via HTTP](images/requisitions-http.png "Pris output for OpenNMS Provisiond via HTTP")

## Driver
The driver is responsible for the overall way the integration is done. Two different drivers are available:

### File driver
This driver offers the ability to call the integration application and get OpenNMS requisition XML files as an result. This driver requires the following parameters in the `global.properties`:

* `driver` = file (selects the file driver)
* `target` = /tmp/requisitions (the folder to store the requisition file)
* `requisitions` = * (a filter for the requisitions to generate)

### HTTP driver
This driver starts up a web server and provides up-to-date requisitions on a http request base. This driver requires the following parameters in the `global.properties`:

* `driver` = http (points to this driver)
* `host` = 127.0.0.1 (the ip for the http-server)
* `port` = 8000 (the port used for the http-server)

## Sources
Sources are used to get the data to build an _OpenNMS requisition_. Sources are defined for each requisition directory in the `requisition.properties` file.

### XLS Source
The `xls.source` reads a _XLS_ spreadsheet file and creates an _OpenNMS requisition_ based on the worksheet content.

    source = xls.source

| parameter  | required  | description                      |
|------------|:---------:|---------------------------------:|
| xls.file   | *         | the path of the xls file to read |

The structure of the spreadsheet has to follow this rules. The source is reading from a sheet named like the requisition you are requesting. The first row of the sheet is reserved to column-names. This column-names have to start with certain prefixes to be recognized.

| prefixes  | required | description                        |
|-----------|:--------:|-----------------------------------:|
| Node_     | * | will be interpreted as node label and `foreignId` |
| IP_       | * | will be interpreted as an IP address as a new interface on the node |
| MgmtType_ | * | is interpreted as `snmp-primary` flag and controls how the interface can be used to communicate with the SNMP agent. Valid are `P` (Primary), `S` (Secondary) and `N` (None). |
| cat_      |   | will be interpreted as a surveillance-category. Multiple comma separated categories can be provided. It can be used multiple times per sheet.|
| svc_      |   | will be interpreted as a service on the interface of the node. Multiple comma separated services can be provided. It can be used multiple times per sheet.|

This source also supports all asset-fields by using `Asset_` as a prefix followed by the `asset-field-name`. The city field of the assets can be addressed like this: `Asset_City`. This is not case-sensitive.

To add a node with multiple interfaces, add an additional sequent row with the same nodelabel (Node_). This row will be added as a new interface based on the data from the  IP_, MgmtType_, svc_ columns.

The order in which the columns are arranged is irrelevant. Also additional columns can be present.

### opennms-requisition Source
This source is reading a requisition via _HTTP_ from another _OpenNMS_ given by `requisition.url`. For authentication a username an password can be provided.

    source = requisition.source

| parameter            | required | description                      |
|----------------------|:--------:|---------------------------------:|
| requisition.url      | *        | OpenNMS requisition ReST URL, e.g. https://opennms.opennms-edu.net/opennms/rest/requisitions  |
| requisition.username |          | OpenNMS user name for access the requisition ReST URL |
| requisition.password |          | OpenNMS user password for access the requisition ReST URL|

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

### OCS Source
_OCS-Inventory NG_ is handling computers and SNMP devices separately in its APIs. For that reason there are two different sources available to import nodes from _OCS_. Some parameters are part of both sources and described first.

#### general ocs parameters
The following parameters are **required**:

* `ocs.url` = The _URL_ of the _OCS web application_.
* `ocs.username` = A _OCS user_ with rights to access the _OCS Soap interface_.
* `ocs.password` = The password for the _OCS user_ with rights to access the _OCS Soap interface_.
* `ocs.checksum` = The `ocs.checksum` parameter controls how detailed the data is that the integration is requesting from the _OCS_. It is important to request all the data you want to map into your requisition but not to much, cause a high checksum causes the request to be significantly slower. Read the [OCS Web-Services](http://wiki.ocsinventory-ng.org/index.php/Developers:Web_services) documentation for more information. The default _checksum_ for the _default mapper_ is `4099`.

The following parameter is **optional**:

* `ocs.tags` = OCS supports tags / custom fields. If a tag is added to the ocs.tags list, just computers and `snmpDevices` that are marked with all the tags will be read from the _OCS_. This feature can be used to tag computers as `testing` or `production`.

#### source ocs.computers
This source is reading computers from a _OCS instance_. It supports all parameters listed as general and the following additions:

* `ocs.accountinfo` = `accountinfo` data is based on custom fields managed in OCS. There are managed by the _Administrative-Data_ section of the _OCS web application_. The name of the custom field is presented in all caps. The value of the field as provided by the user. The `ocs.accountinfo` parameters supports a list of `accountinfo` that has to be present on the computer. If one of the `accountinfo` is not present the computers is skipped. To add multiple `accountinfo` they can be separated with spaces.

#### source ocs.snmpDevices
This source is reading `snmpDevices` from a _OCS instance_. It supports all parameters listed as general and no additional at the moment.

#### Mock Sources
For development and testing there are `ocs.computers.replay` and `ocs.snmpDevices.replay` sources available. This sources require a file that contains the computers or `snmpDevices` as _XML_ file. The file has also be referenced in the configuration.


### merge-source
The merge source allows to merge two requisitions. You can also use provided resources by pris recursively.

    source = requisitionMerge.source

| parameter                    | required | description             |
|------------------------------|:--------:|------------------------:|
|  requisition.A.url           | *        |URL to the requisition A |
|  requisition.A.username      |          |username for access      |
|  requisition.A.password      |          |password for access      |
|                              |          |                         |
|  requisition.B.url           | *        |URL to the requisition B |
|  requisition.B.username      |          |username for access      |
|  requisition.B.password      |          |password for access      |
|                              |          |                         |
|  requisition.A.keepAll       |          | if this parameters is present in the config all nodes from requisition A will be present in the resulting requisition. |
|  requisition.B.keepAll       |          | if this parameters is present in the config all nodes from requisition B will be present in the resulting requisition. |

This source is reading two already defined requisitions via _HTTP_ and merges them into one new requisition. By default the resulting requisition will contain all nodes that are present in both requisitions, identified by the `foreignId`. The A-Node (from requisition A) is enriched with the data from B-Node.

## Asset field mapping
The asset field mapping can be used in `xls.source` and `jdbc.source`.

| OpenNMS requisition asset field mapping |
|-----------------------------------------|
| `Asset_additionalhardware` |
| `Asset_address1` |
| `Asset_address2` |
| `Asset_admin` |
| `Asset_assetNumber` |
| `Asset_autoenable` |
| `Asset_building` |
| `Asset_category` |
| `Asset_circuitId` |
| `Asset_city` |
| `Asset_comment` |
| `Asset_connection` |
| `Asset_country` |
| `Asset_cpu` |
| `Asset_dateInstalled` |
| `Asset_department` |
| `Asset_description` |
| `Asset_displayCategory` |
| `Asset_division` |
| `Asset_enable` |
| `Asset_floor` |
| `Asset_hdd1` |
| `Asset_hdd2` |
| `Asset_hdd3` |
| `Asset_hdd4` |
| `Asset_hdd5` |
| `Asset_hdd6` |
| `Asset_inputpower` |
| `Asset_latitude` |
| `Asset_lease` |
| `Asset_leaseExpires` |
| `Asset_longitude` |
| `Asset_maintContractExpiration` |
| `Asset_maintContractNumber` |
| `Asset_manufacturer` |
| `Asset_modelNumber` |
| `Asset_notifyCategory` |
| `Asset_numpowersupplies` |
| `Asset_operatingSystem` |
| `Asset_password` |
| `Asset_pollerCategory` |
| `Asset_port` |
| `Asset_rack` |
| `Asset_rackunitheight` |
| `Asset_ram` |
| `Asset_region` |
| `Asset_room` |
| `Asset_serialNumber` |
| `Asset_slot` |
| `Asset_snmpcommunity` |
| `Asset_state` |
| `Asset_storagectrl` |
| `Asset_supportPhone` |
| `Asset_thresholdCategory` |
| `Asset_username` |
| `Asset_vendor` |
| `Asset_vendorAssetNumber` |
| `Asset_vendorFax` |
| `Asset_vendorPhone` |
| `Asset_vmwareManagedEntityType` |
| `Asset_vmwareManagedObjectId` |
| `Asset_vmwareManagementServer` |
| `Asset_vmwareState` |
| `Asset_vmwareTopologyInfo` |
| `Asset_zip` |

## Mapper

### Mapper for specific sources
Mappers are used to map the OCS data model with computers and SNMP devices to the OpenNMS data model for provisioning with nodes, interfaces, services and assets. The OCS integration provides one default mapper for computers and one for snmp-devices out of the box. Additionally it provides script based mapping via the script mapper.

#### OCS Mappers
The default mappers for OCS are a simple way to map computers and snmp-devices to OpenNMS nodes.

##### Computers
To use this mapper, configure your requisition config to use "default.ocs.computers" as mapper. This mapper requires a ocs.checksum of 4099 to get all required data. It elects one of the ip-addresses of a computer to be the management-interface of the node. This is controlled by the black- and whitelisting. The default ip-filter is used for the election. If no interface is valid, the node will have no interfaces and a corresponding log message is written. The elected management-interface is enriched with the interface description, if available. The SNMP and ICMP service are forced to the management-interface. Additionally the comment field of the node assets are used to provide a html link to the computer-page of the ocs instance. The assets for cpu and operationgSystem will be mapped from the OCS computer too. The computer name is used as foreignId and nodeLable.

###### CategoryMap
The Default mapper for Computers supports a mapping between OCS Accountinfo data from OCS to OpenNMS surveillance-categories. To use this feature add the categoryMap parameter to the requisition.properties file and reference a properties file following this syntax example:
 
     ADMINISTRATIVEDATAFILEDNAME.data=OpenNMSCategoryName
     ENVIRONMENT.Production=Production JOB.Mailserver=Mail

##### snmp-devices
To use this mapper, configure your requisition config to use `default.ocs.snmpDevices` as mapper. This mapper requires a `ocs.checksum` of `4099`. It validates the IP address of the `snmpDevice` verses the black- and whitelists. For the election of the default an IP filter can be used. If the IP address of the `snmpDevice` is _blocked_ a log message is written and the node will not have any interfaces. The interface has assigned _ICMP_ and _SNMP_ as services. The `foreignId` of is mapped with the _OCS id_ of the `snmpDevice`. The `nodeLabel` is provided by the _OCS name_ of the `snmpDevice`. The assets for CPU and operating system are mapped against _OCS_. Additionally a link to the OCS `snmpDevice` page is added to the asset comment field.

##### Black- and Whitelists
The OCS Integration supports Black- and Whitelists to control the selection of the management-interface for the node. OCS it self dose not define a management-interface, it just selects one ip-address as default and maintains a networks-list for every computer. For the election of the management-interface two ip-filters are implemented in the IpInterfaceHelper-class. Both read the black- and whitelist from the requisition configuration folder. Name them "blackList.properties" and "whiteList.properties". Every line in those files is interpreted as an IPLike statement to offer ranges.

###### Default ip-filter
This filter is accepting every IP address as valid that is not blacklisted. IP addresses that are white listed are preferred over not listed IP addresses.

###### Computers
The first IP address of the `ocs-networks-list` that is white listed is used. If no IP address of the `ocs-networks-list` is white listed the first not IP address that is not black-listed is elected as management interface. If no IP address of the `ocs-networks-list` qualifies, the `ocs-default-ip` is checked against the blacklist. If it is not black listed, it is elected as management interface (no interface description will be available). If it is black listed, no interface is added to the node. (`selectManagementNetwork`)

###### SnmpDevices
The IP address of a `snmpDevice` is elected as management interface as long as it is not black listed. If it is black listed no interface is added to the node. (`selectIpAddress`)

###### Strict ip-filter "WhiteAndBackOnly"
This filter is as strict black- and white list approach. Computers and `snmpDevices` are handled independently.

###### Computers
This mode is just accepting IP addresses that are white listed and not black listed. If there are multiple IP addresses listed on `ocs-networks-list` that are white listed but and not black listed, the first one is selected as management IP. If no IP address from the `ocs-networks-list` matches the black- and whitelist, the `ocs-default-ip` is tested against the black- and whitelist. If the `ocs-defaul-ip` is white listed and not black listed it is elected as management-ip. If no IP address matches the black- and whitelist, no interface is added to the node. If the `ocs-default-ip` is selected, the interface of the node will not contain any additional parameters like description. (`selectManagementNetworkWhiteAndBlackOnly`)

###### SnmpDevices
If the IP address of the `snmpDevice` is white listed and not black listed it is elected as management interface. If the IP address is not passing the lists, no interface is added to the node. (`selectIpAddressWhiteAndBlackOnly`)

###### IPLike expressions in lists
In both lists the `IPLike` syntax can be used to express IP ranges and wildcards. Follow the `IPLike` description at [IPLIKE documentaion](http://www.opennms.org/wiki/IPLIKE).

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

## Scripts
* What are scripts
* Chaining scripts

## Integration in OpenNMS Provisiond
