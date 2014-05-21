import org.opennms.netmgt.provision.persist.requisition.Requisition
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode
import org.opennms.ocs.inventory.client.response.Computer;
import org.opennms.ocs.inventory.client.response.Computers;

final Computers myComputers = data;
Requisition myRequisition = requisition;

for (Computer computer : myComputers.getComputers()) {
    logger.debug("Processing Computer {}", computer.getHardware().getName());

    RequisitionNode node = myRequisition.getNode(computer.getHardware().getName());
    logger.debug("Processing node {}", node);
 
    if (node != null) {
        //map more stuff
    }
    
}

logger.info("Returning {} requisition with {} nodes", myRequisition.getForeignSource(), myRequisition.getNodes().size());
return myRequisition;