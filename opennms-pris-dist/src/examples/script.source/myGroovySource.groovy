import org.opennms.pris.model.*

/**
 * <p>myGroovySource class.</p>
 *
 * Sample source creating a requisition with one node with Groovy
 *
 * @author <a href="mailto:ronny@opennms.org">Ronny Trommer</a>
 * @version $Id: $
 * @since 1.0-SNAPSHOT
 */

// Name of the requisition. XML file name should be the same, e.g. $OPENNMS_HOME/etc/imports/pending/myGroovySource.xml
Requisition requisition = new Requisition()
requisition.setForeignSource(instance)

config.getInt("count").times {
    logger.info("Adding node: {}", it)

    // Create a new requisition node
    RequisitionNode requisitionNode = new RequisitionNode()

    // IP interfaces
    List<RequisitionInterface> interfaceList = new ArrayList<RequisitionInterface>()
    RequisitionInterface requisitionInterface = new RequisitionInterface()

    // List of services
    List<RequisitionMonitoredService> monitoredServiceList = new ArrayList<RequisitionMonitoredService>()

    // Asset variables
    List<RequisitionAsset> assetList = new ArrayList<RequisitionAsset>()
    RequisitionAsset assetCity = new RequisitionAsset()
    RequisitionAsset assetZip = new RequisitionAsset()
    RequisitionAsset assetCountry = new RequisitionAsset()

    // Set node label and foreign ID for the node
    requisitionNode.setNodeLabel("MyNodeLabel" + it)
    requisitionNode.setForeignId("MyForeignId" + it)

    // Create IP interface and set status monitored (1 managed / 3 is not managed)
    requisitionInterface.setStatus(1);
    requisitionInterface.setIpAddr("127.0.0." + it)
    requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY)
    interfaceList.add(requisitionInterface)

    // Assign services to monitor to interface
    monitoredServiceList.add(new RequisitionMonitoredService(null, "ICMP"))
    monitoredServiceList.add(new RequisitionMonitoredService(null, "SNMP"))
    monitoredServiceList.add(new RequisitionMonitoredService(null, "HTTP"))

    // Assign services for monitoring to IP interface
    requisitionInterface.withMonitoredServices(monitoredServiceList)

    // Set Asset information
    assetCity.setName("city")
    assetCity.setValue("Fulda")
    assetZip.setName("zip")
    assetZip.setValue("36039")
    assetCountry.setName("country")
    assetCountry.setValue("Germany")

    assetList.add(assetCity)
    assetList.add(assetZip)
    assetList.add(assetCountry)

    // Assign Interfaces and assets to the node
    requisitionNode.withInterfaces(interfaceList)
    requisitionNode.withAssets(assetList)

    // Put new node into requisition
    requisition.withNodes(requisitionNode)
}

return requisition
