package org.opennms.provisioner.ocs;

import org.opennms.netmgt.model.PrimaryType;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.netmgt.provision.persist.requisition.RequisitionAsset;
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface;
import org.opennms.netmgt.provision.persist.requisition.RequisitionMonitoredService;
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode;
import org.opennms.ocs.inventory.client.response.Computer;
import org.opennms.ocs.inventory.client.response.Computers;
import org.opennms.ocs.inventory.client.response.Network;
import org.opennms.ocs.inventory.client.response.snmp.SnmpDevice;
import org.opennms.ocs.inventory.client.response.snmp.SnmpDevices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcsDefaultMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcsDefaultMapper.class);

    private String ocsUrl;
    private IpInterfaceHelper ipInterfaceHelper = new IpInterfaceHelper();
    
    
    public OcsDefaultMapper(String ocsUrl) {
        this.ocsUrl = ocsUrl;
        ipInterfaceHelper.initListsFromConfigs();
    }
  
    private RequisitionNode mapComputerToRequisitionNode(Computer computer) {
        RequisitionNode requisitionNode = new RequisitionNode();
        requisitionNode.setForeignId(computer.getHardware().getId() + "");
        requisitionNode.setNodeLabel(computer.getHardware().getName());

        RequisitionInterface requisitionInterface = new RequisitionInterface();
        Network managementNetwork = ipInterfaceHelper.selectManagementNetworkWhiteAndBlackOnly(computer);
        if (managementNetwork != null) {
            requisitionInterface.setIpAddr(managementNetwork.getIPAddress());
            requisitionInterface.setDescr(managementNetwork.getDescription());
            requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY);
            requisitionInterface.setManaged(Boolean.TRUE);
            requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("SNMP"));
            requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("ICMP"));
            requisitionNode.getInterfaces().add(requisitionInterface);
        }
        requisitionNode.getAssets().add(new RequisitionAsset("operatingSystem", computer.getHardware().getOsname()));

        String ocsComputerLink = "<a href=" + ocsUrl + "/index.php?function=computer&head=1&systemid=" + requisitionNode.getForeignId() + ">OCS-Inventory</a>";
        requisitionNode.getAssets().add(new RequisitionAsset("comment", ocsComputerLink));

        requisitionNode.getAssets().add(new RequisitionAsset("cpu", computer.getHardware().getProcessort()));


        return requisitionNode;
    }

    public Requisition mapComputersToRequisition(Computers computers) {
        Requisition requisition = new Requisition();
        RequisitionNode requisitionNode;
        for (Computer computer : computers.getComputers()) {
            LOGGER.debug("Processing Computer {}", computer.getHardware().getName());
            requisitionNode = mapComputerToRequisitionNode(computer);
            if (requisitionNode != null) {
                requisition.getNodes().add(requisitionNode);
            }
        }
        return requisition;
    }

    private RequisitionNode mapSnmpDeviceToRequisitionNode(SnmpDevice snmpDevice) {
        RequisitionNode requisitionNode = new RequisitionNode();
        requisitionNode.setForeignId(snmpDevice.getSNMP().getId() + "");
        requisitionNode.setNodeLabel(snmpDevice.getSNMP().getName());
    
        RequisitionInterface requisitionInterface = new RequisitionInterface();
        requisitionInterface.setIpAddr(snmpDevice.getSNMP().getIPAddr());
        requisitionInterface.setDescr("OCS");
        requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY);
        requisitionInterface.setManaged(Boolean.TRUE);
        requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("SNMP"));
        requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("ICMP"));
    
        String ocsSnmpDeviceLink = "<a href=" + ocsUrl + "/index.php?function=snmp_detail&head=1&id=" + requisitionNode.getForeignId() + ">OCS-Inventory</a>";
        requisitionNode.getAssets().add(new RequisitionAsset("comment", ocsSnmpDeviceLink));
    
        requisitionNode.getInterfaces().add(requisitionInterface);
    
        return requisitionNode;
    }

    public Requisition mapSnmpDevicesToRequisition(SnmpDevices snmpDevices) {
        Requisition requisition = new Requisition();
        RequisitionNode requisitionNode;
        for (SnmpDevice snmpDevice : snmpDevices.getSNMPDevices()) {
            requisitionNode = mapSnmpDeviceToRequisitionNode(snmpDevice);
            if (requisitionNode != null) {
                requisition.getNodes().add(requisitionNode);
            }
        }
        return requisition;
    }
}
