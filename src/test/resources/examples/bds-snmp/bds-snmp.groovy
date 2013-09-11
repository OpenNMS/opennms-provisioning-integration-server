import java.lang.StringBuilder;
import java.util.Set;
import java.util.HashSet;

import org.opennms.ocs.inventory.client.response.snmp.Snmp;
import org.opennms.ocs.inventory.client.response.snmp.SnmpDevice;
import org.opennms.ocs.inventory.client.response.snmp.SnmpDevices;
import org.opennms.netmgt.provision.persist.requisition.RequisitionAsset;
import org.opennms.netmgt.provision.persist.requisition.RequisitionCategory;
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode;
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface;
import org.opennms.netmgt.provision.persist.requisition.RequisitionMonitoredService;
import org.opennms.netmgt.provision.persist.requisition.Requisition
import org.opennms.netmgt.model.PrimaryType;

String foreignSource;
String ocsUrl;
String mapper;

SnmpDevices mySnmpDevices = snmpDevices;
Requisition myRequisition = new Requisition(foreignSource);
Set existingForeignIDs = new HashSet();

for (SnmpDevice snmpDevice : mySnmpDevices.getSNMPDevices()) {
    RequisitionNode rNode = this.getRequisitionNode(snmpDevice);
    // true indicates the set did not already contain this element
    if (existingForeignIDs.add(rNode.getForeignId())) {
        myRequisition.getNodes().add(rNode);
    } else {
        // TODO actually do something useful here
        logger.error("Ignoring duplicate foreign-ID '{}'", rNode.getForeignId());
    }
}

logger.info("Returning requisition with {} nodes", myRequisition.getNodes().size());
return myRequisition;

private RequisitionNode getRequisitionNode(SnmpDevice snmpDevice) {
    RequisitionNode myRequisitionNode = new RequisitionNode();
    SnmpDevice mySnmpDevice = snmpDevice;

    myRequisitionNode.setForeignId(mySnmpDevice.getSNMP().getName());
    myRequisitionNode.setNodeLabel(mySnmpDevice.getSNMP().getName());

    populateInterfaces(mySnmpDevice, myRequisitionNode);

    return myRequisitionNode;
}

private void populateInterfaces(SnmpDevice mySnmpDevice, RequisitionNode myRequisitionNode) {
    RequisitionInterface requisitionInterface = new RequisitionInterface();
    requisitionInterface.setIpAddr(mySnmpDevice.getSNMP().getIPAddr());
    requisitionInterface.setDescr("From OCS");

    requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY);
    requisitionInterface.setManaged(Boolean.TRUE);
    requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("ICMP"));
    requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("SNMP"));

    myRequisitionNode.getInterfaces().add(requisitionInterface);
}
