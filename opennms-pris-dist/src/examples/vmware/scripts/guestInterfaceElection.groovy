import org.opennms.netmgt.model.PrimaryType
import org.opennms.netmgt.provision.persist.requisition.Requisition
import org.opennms.netmgt.provision.persist.requisition.RequisitionAsset
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface
import org.opennms.netmgt.provision.persist.requisition.RequisitionMonitoredService
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode
import org.opennms.pris.ASSET_FIELD

logger.debug(" --- Start script: " + script.getFileName())
for (RequisitionNode node : requisition.getNodes()) {

    /*
     * Guest has just one interface, we assign the services to the first interface for data collection and service monitoring.
     * The services don't need the IP interface, we use the VMware management IP from the asset field for status monitoring and
     * data collection.
     */
    if (node.getInterfaces().size() > 0) {
        logger.debug("Guest " + node.getNodeLabel() + " has just 1 interface " + node.getInterfaces().get(0) + ". Set to monitored and assign services for monitoring and data collection")

        RequisitionInterface requisitionInterface = node.getInterfaces().get(0)
        requisitionInterface.setStatus(1)

        // Assign services for power state monitoring and data collection through vCenter
        requisitionInterface.getMonitoredServices().add(new RequisitionMonitoredService("VMware-VirtualMachine"));
        requisitionInterface.getMonitoredServices().add(new RequisitionMonitoredService("VMware-ManagedEntity"));
    } else {
        logger.warn("No interface found on Guest " + node.getNodeLabel() + ". Data collection and service monitoring is not possible.")
    }
}
logger.debug(" --- Return script output: " + script.getFileName())
return requisition
