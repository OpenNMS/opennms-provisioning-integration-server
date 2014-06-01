/**
* This script step sets the nodelabel based on a reverse dns lookup of the ip interfaces.
* It reverse dns lookups all interfaces for each node until it findes a dns name for a node.
* If a dns name was found it is set as nodelabel and no other interface of the nodes will be checked.
* If no dns name was found the nodelabel will be changed.
*/

import org.opennms.pris.model.Requisition
import org.opennms.pris.model.RequisitionNode
import org.opennms.pris.model.RequisitionInterface

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