import org.opennms.netmgt.provision.persist.requisition.Requisition
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface

logger.info("starting reverseDNS.groovy")

for (RequisitionNode node : requisition.getNodes()) {

  for (RequisitionInterface myInterface : node.getInterfaces()) {
    String ipAddress = myInterface.getIpAddr()
    String dnsNodeLabel = InetAddress.getByName(ipAddress).getCanonicalHostName()
    logger.debug("For foreignID '{}' dnsNodeLabel for IP '{}' is '{}'", node.getForeignId(), ipAddress, dnsNodeLabel)
    if (ipAddress.equals(dnsNodeLabel)) {
      logger.debug("No dns-name was found for node '{}' and ip '{}' using ip as nodeLabel", node.getForeignId(), ipAddress)
    }
    node.setNodeLabel(dnsNodeLabel)
  }
}

logger.info("done with reverseDNS.groovy")

return requisition
