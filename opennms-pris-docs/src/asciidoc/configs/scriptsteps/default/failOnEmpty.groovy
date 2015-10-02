/**
* This script forces a hard failure if the requisition is empty.
* This can avoid provisioning an empty requisition,
* including removing the existing nodes, on a failure of a source or mapper.
**/

import org.slf4j.Logger
import org.opennms.pris.model.Requisition

logger.info("starting failOnEmpty.groovy")

logger.debug("Amount of nodes in the requisition '{}'", (requisition.getNodes().size()))

if (requisition.getNodes().size() == 0) {
  throw new Exception("The requisition '" + requisition.getForeignSource()  + "' had no nodes. The failOnEmpty.groovy script is failing the request on purpose.")
}
logger.info("done with failOnEmpty.groovy")

return requisition 
