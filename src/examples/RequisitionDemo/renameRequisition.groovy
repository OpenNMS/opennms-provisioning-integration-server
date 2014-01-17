import org.opennms.netmgt.provision.persist.requisition.Requisition
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode

Requisition requisition = data
requisition.setForeignSource(instance)
for (RequisitionNode node : requisition.getNodes()) {
    node.setBuilding(instance)
}
logger.info("Requisition: {}", requisition)

return requisition
