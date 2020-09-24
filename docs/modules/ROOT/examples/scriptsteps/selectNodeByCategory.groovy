/**
* This script step adds every node from the requisiton that has a specific category assigned to it.
 **/

import org.opennms.pris.model.Requisition
import org.opennms.pris.model.RequisitionNode
import org.opennms.pris.model.RequisitionCategory
import org.opennms.pris.util.RequisitionUtils

logger.info("starting selectNodeByCategory.groovy")
final String SELECT_CATEGORY = "CATEGORY"
List <RequisitionNode> nodes = new ArrayList<>();

for (RequisitionNode node : requisition.getNodes()) {
    if (RequisitionUtils.hasCategory(node, SELECT_CATEGORY, true)) {
        logger.debug("node '{}' is ok", node.getForeignId())
        nodes.add(node)
    } else {
        logger.debug("node '{}' has to be ignored", node.getForeignId())
    }
}
requisition.unsetNodes()
requisition.withNodes(nodes)
logger.info("done with selectNodeByCategory.groovy")
return requisition
