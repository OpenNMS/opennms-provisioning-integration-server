import java.lang.StringBuilder;

import org.opennms.ocs.inventory.client.response.Bios;
import org.opennms.ocs.inventory.client.response.Computer;
import org.opennms.ocs.inventory.client.response.Computers;
import org.opennms.ocs.inventory.client.response.Hardware;
import org.opennms.ocs.inventory.client.response.Network;
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

Computers myComputers = computers;
Requisition myRequisition = new Requisition(foreignSource);

for (Computer computer : myComputers.getComputers()) {
    myRequisition.getNodes().add(this.getRequisitionNode(computer));
}

return myRequisition;

private RequisitionNode getRequisitionNode(Computer computer) {
    RequisitionNode myRequisitionNode = new RequisitionNode();
    Computer myComputer = computer;

    myRequisitionNode.setForeignId(myComputer.getHardware().getId() + "");
    myRequisitionNode.setNodeLabel(myComputer.getHardware().getName());

    populateBiosAssets(myComputer, myRequisitionNode);
    populateCpuAssets(myComputer, myRequisitionNode);
    populateOSAssets(myComputer, myRequisitionNode);
    populateInterfaces(myComputer, myRequisitionNode);

    return myRequisitionNode;
}

private void populateBiosAssets(Computer myComputer, RequisitionNode myRequisitionNode) {
    Bios myBios = myComputer.getBios();
    if (myBios != null) {
        myRequisitionNode.getAssets().add(new RequisitionAsset("manufacturer", myBios.getSManufacturer()));
        myRequisitionNode.getAssets().add(new RequisitionAsset("model", myBios.getSModel()));
        myRequisitionNode.getAssets().add(new RequisitionAsset("serialNumber", String.valueOf(myBios.getSSN())));
    }
}

private void populateCpuAssets(Computer myComputer, RequisitionNode myRequisitionNode) {
    StringBuilder cpuStringBuilder = new StringBuilder(String.valueOf(myComputer.getHardware().getProcessorn()))
        .append(" x ").append(String.valueOf(myComputer.getHardware().getProcessors()))
        .append("MHz ").append(myComputer.getHardware().getProcessort());
    myRequisitionNode.getAssets().add(new RequisitionAsset("cpu", cpuStringBuilder.toString()));
}

private void populateOSAssets(Computer myComputer, RequisitionNode myRequisitionNode) {
    StringBuilder osStringBuilder = new StringBuilder(myComputer.getHardware().getOsname())
        .append(" ").append(myComputer.getHardware().getOsversion())
        .append(" (").append(myComputer.getHardware().getOscomments()).append(")");
    myRequisitionNode.getAssets().add(new RequisitionAsset("operatingsystem", osStringBuilder.toString()));
}

private void populateInterfaces(Computer myComputer, RequisitionNode myRequisitionNode) {
    RequisitionInterface requisitionInterface = new RequisitionInterface();
    Network managementNetwork = myComputer.getNetworks().get(0);
    requisitionInterface.setIpAddr(managementNetwork.getIPAddress());
    requisitionInterface.setDescr(managementNetwork.getDescription());

    requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY);
    requisitionInterface.setManaged(Boolean.TRUE);
    requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("ICMP"));
    requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("SNMP"));

    myRequisitionNode.getInterfaces().add(requisitionInterface);
}
