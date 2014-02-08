# OpenNMS Provisioning Integration Server
The provisioning integration server (pris) is a software which provides the ability to get external information from your inventory into an OpenNMS requisition model. The output from pris is provided as XML over HTTP and can be used in OpenNMS Provisiond to import and discover nodes from.

![OpenNMS pris overview](images/pris-overview.png "OpenNMS pris overview")

The service is used to provide an integration point for OpenNMS external inventories or homebrew inventories. With pris the data is normalized to the OpenNMS requisition model and can be consumed from OpenNMS provisiond. It is highly specialized to enrich the requisition model with SNMP information i.e. SNMP interfaces, generic SNMP attributes like system location, contact and the system description. Beside that Provisiond can also run service detectors against IP interfaces and allows to run policies to control the monitoring behavior. Pris is an instance in front of Provisiond, it allows to aggregate information from different sources and manipulate them in a flexible way.

* What is a requistion
* How is the structure in the file system

## Driver
* What is a Driver
### File driver
### HTTP driver

## Sources
* What are sources

### merge-source
### XLS Source
### opennms-requisition Source
### JDBC Source
### OCS Source

## Mapper scripts
* What are mapper scripts

## Scripts
* What are scripts
* Chaining scripts

## Integration in OpenNMS Provisiond
