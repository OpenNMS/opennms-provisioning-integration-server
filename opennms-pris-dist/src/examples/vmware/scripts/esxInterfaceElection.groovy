import org.opennms.netmgt.model.PrimaryType
import org.opennms.netmgt.provision.persist.requisition.Requisition
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface
import org.opennms.netmgt.provision.persist.requisition.RequisitionMonitoredService
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode

logger.debug(" --- Start script: " + script.getFileName())
for (RequisitionNode node : requisition.getNodes()) {

    // The ESX host has just one interface, we set it to monitored in hope OpenNMS can reach it
    if (node.getInterfaces().size() > 0) {
        logger.debug("ESX host " + node.getNodeLabel() + " has just 1 interface " + node.getInterfaces().get(0) + ". Set to monitored and assign services for monitoring and data collection")

        RequisitionInterface requisitionInterface = node.getInterfaces().get(0)
        requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY)
        requisitionInterface.setStatus(1)
        requisitionInterface.getMonitoredServices().add(new RequisitionMonitoredService("VMware-HostSystem"))
        requisitionInterface.getMonitoredServices().add(new RequisitionMonitoredService("VMware-ManagedEntity"))
    } else {
        logger.warn("No interface found on ESX host " + node.getNodeLabel() + ". Data collection and service monitoring is not possible.")
    }
}
logger.debug(" --- Return script output: " + script.getFileName())
return requisition