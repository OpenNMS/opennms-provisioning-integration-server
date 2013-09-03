import org.opennms.ocs.inventory.client.response.Computer;
import org.opennms.ocs.inventory.client.response.Computers;
import org.opennms.ocs.inventory.client.response.Hardware;
import org.opennms.ocs.inventory.client.response.Network;
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
    RequisitionInterface requisitionInterface = new RequisitionInterface();

    Computer myComputer = computer;
    myRequisitionNode.setNodeLabel(myComputer.getHardware().getName());

    myRequisitionNode.setForeignId(myComputer.getHardware().getId() + "");

    Network managementNetwork = myComputer.getNetworks().get(0);
    requisitionInterface.setIpAddr(managementNetwork.getIPAddress());
    requisitionInterface.setDescr(managementNetwork.getDescription());

    requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY);
    requisitionInterface.setManaged(Boolean.TRUE);
    requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("ICMP"));
    requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("SNMP"));

    myRequisitionNode.getInterfaces().add(requisitionInterface);

    return myRequisitionNode;
}