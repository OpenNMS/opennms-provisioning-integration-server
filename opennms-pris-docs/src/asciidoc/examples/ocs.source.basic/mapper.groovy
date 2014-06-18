import org.opennms.pris.model.Requisition
import org.opennms.pris.model.RequisitionAsset
import org.opennms.pris.model.RequisitionNode
import org.opennms.ocs.inventory.client.response.Computer;
import org.opennms.ocs.inventory.client.response.Computers;
import org.opennms.ocs.inventory.client.response.Drive;
import org.opennms.pris.util.RequisitionUtils;

// This sample script for ocs computers overwrites the foreignId to be the ocs systemId / hardwareId
final Computers myComputers = data;
Requisition myRequisition = requisition;

for (Computer computer : myComputers.getComputers()) {
    logger.debug("Processing Computer {}", computer.getHardware().getName());

    RequisitionNode node = RequisitionUtils.findNode(myRequisition, computer.getHardware().getName());
    logger.debug("Processing node {}", node);

    if (node != null) {
        logger.debug("looking for drives")
        node.setForeignId(computer.getHardware().getId());
    }
}

logger.info("Returning {} requisition with {} nodes", myRequisition.getForeignSource(), myRequisition.getNodes().size());
return myRequisition;
