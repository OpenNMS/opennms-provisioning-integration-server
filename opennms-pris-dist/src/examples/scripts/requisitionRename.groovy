/**
* This script renames the requisition to the value in the newName variable.
* If newBuilding is set to true, each node gets the newName set as building
*/

import org.opennms.pris.model.Requisition
import org.opennms.pris.model.RequisitionNode

logger.info("starting requisitionRename.groovy")

String newName = "myNewRequisitionName"
Boolean newBuilding = false // true

requisition.setForeignSource(newName)

if (newBuilding) {
  for (RequisitionNode node : requisition.getNodes()) {
    node.setBuilding(newName)  
  }
}
logger.info("done with requisitionRename.groovy")
return requisition