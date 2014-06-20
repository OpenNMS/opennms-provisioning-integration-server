import java.lang.StringBuilder;

//FIXME: Unused import
import org.opennms.ocs.inventory.client.response.snmp.Snmp;

import org.opennms.ocs.inventory.client.response.snmp.SnmpDevice;
import org.opennms.ocs.inventory.client.response.snmp.SnmpDevices;
import org.opennms.pris.model.RequisitionAsset;

//FIXME: Unused import
import org.opennms.pris.model.RequisitionCategory;

import org.opennms.pris.model.RequisitionNode;
import org.opennms.pris.model.RequisitionInterface;
import org.opennms.pris.model.RequisitionMonitoredService;
import org.opennms.pris.model.Requisition
import org.opennms.netmgt.model.PrimaryType;

String foreignSource;

//FIXME: Variable mapper is not used
String mapper;

final SnmpDevices mySnmpDevices = data;
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
    requisitionInterface.setStatus(1);
    requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("ICMP"));
    requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("SNMP"));

    myRequisitionNode.getInterfaces().add(requisitionInterface);
}
