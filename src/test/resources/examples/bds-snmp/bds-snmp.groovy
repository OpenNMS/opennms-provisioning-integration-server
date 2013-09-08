import java.lang.StringBuilder;

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

for (SnmpDevice snmpDevice : mySnmpDevices.getSNMPDevices()) {
    myRequisition.getNodes().add(this.getRequisitionNode(snmpDevice));
}

return myRequisition;

private RequisitionNode getRequisitionNode(SnmpDevice snmpDevice) {
    RequisitionNode myRequisitionNode = new RequisitionNode();
    SnmpDevice mySnmpDevice = snmpDevice;

    myRequisitionNode.setForeignId(mySnmpDevice.getSNMP().getId() + "");
    myRequisitionNode.setNodeLabel(mySnmpDevice.getSNMP().getName());

    populateOSAssets(mySnmpDevice, myRequisitionNode);
    populateInterfaces(mySnmpDevice, myRequisitionNode);

    return myRequisitionNode;
}

private void populateOSAssets(SnmpDevice mySnmpDevice, RequisitionNode myRequisitionNode) {
    StringBuilder osStringBuilder = new StringBuilder(mySnmpDevice.getSNMP().getDescription());
    myRequisitionNode.getAssets().add(new RequisitionAsset("operatingsystem", osStringBuilder.toString()));
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
