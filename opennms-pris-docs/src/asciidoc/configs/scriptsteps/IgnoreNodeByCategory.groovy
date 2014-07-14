/**
* This script step removes every node from the requisiton that has the "ignore" category assigned to it.
* The category match ignores case.
 **/

import org.opennms.pris.model.Requisition
import org.opennms.pris.model.RequisitionNode
import org.opennms.pris.model.RequisitionCategory
import org.opennms.pris.util.RequisitionUtils

logger.info("starting IgnoreNodeByCategory.groovy")
final String IGNORE_CATEGORY = "ignore"
List <RequisitionNode> nodes = new ArrayList<>();

for (RequisitionNode node : requisition.getNodes()) {
    if (RequisitionUtils.hasCategory(node, IGNORE_CATEGORY, true)) {
        logger.debug("node '{}' has to be ignored", node.getForeignId())
    } else {
        nodes.add(node)
        logger.debug("node '{}' is ok", node.getForeignId())
    }
}
requisition.unsetNodes()
requisition.withNodes(nodes)
logger.info("done with IgnoreNodeByCategory.groovy")
return requisition
