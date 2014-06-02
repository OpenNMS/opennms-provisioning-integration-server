import org.opennms.pris.model.Requisition
import org.opennms.pris.model.RequisitionAsset
import org.opennms.pris.model.RequisitionNode
import org.opennms.ocs.inventory.client.response.Computer;
import org.opennms.ocs.inventory.client.response.Computers;
import org.opennms.ocs.inventory.client.response.Drive;

final Computers myComputers = data;
Requisition myRequisition = requisition;

for (Computer computer : myComputers.getComputers()) {
    logger.debug("Processing Computer {}", computer.getHardware().getName());

    RequisitionNode node = myRequisition.getNode(computer.getHardware().getName());
    logger.debug("Processing node {}", node);
 
    if (node != null) {
        logger.debug("looking for drives")
        
        //map more stuff
        int i = 1
        for (Drive drive : computer.getDrives()) {
            
            logger.debug("drive '{}'", drive)
            String hddAsset = ""
            
            // Windows
            if (drive.getType() == "Hard Drive") {
                hddAsset = drive.getLetter() + " is " + drive.getFilesystem() + " as " + drive.getVolumn() + " " + drive.getFree() + "/" + drive.getTotal()
            }
            
            //Linxu/Unix
            if (drive.getType().startsWith("/")) {
                hddAsset = drive.getType() + " is " + drive.getFilesystem() + " " + drive.getFree() + "/" + drive.getTotal()
            }
            logger.debug("Node '{}' \t hddAsset '{}'", node.getNodeLabel(), hddAsset)
            
            if (i <= 6 && !hddAsset.isEmpty()) {
                node.getAssets().add(new RequisitionAsset("hdd" +i, hddAsset))
            }
            i++
        }
    }
}

logger.info("Returning {} requisition with {} nodes", myRequisition.getForeignSource(), myRequisition.getNodes().size());
return myRequisition;