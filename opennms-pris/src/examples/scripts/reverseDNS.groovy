import org.opennms.netmgt.provision.persist.requisition.Requisition
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface

logger.info("starting reverseDNS.groovy")

for (RequisitionNode node : requisition.getNodes()) {
  for (RequisitionInterface myInterface : node.getInterfaces()) {
    String ipAddress = myInterface.getIpAddr()
    String dnsNodeLabel = InetAddress.getByName(ipAddress).getCanonicalHostName()
    logger.debug("For foreignID '{}' dnsNodeLabel for IP '{}' is '{}'", node.getForeignId(), ipAddress, dnsNodeLabel)
    if (!ipAddress.equals(dnsNodeLabel)) {
      logger.info("Using '{}' as NodeLabel for foreignId '{}' based on IP '{}'", dnsNodeLabel, node.getForeignId(), ipAddress)
      node.setNodeLabel(dnsNodeLabel)
      break
    }
  }
}
logger.info("done with reverseDNS.groovy")
return requisition