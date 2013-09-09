package org.opennms.provisioner.ocs;

import java.util.ArrayList;
import java.util.List;

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

public class OcsDefaultMapper {

    private String ocsUrl;
    
    public OcsDefaultMapper(String ocsUrl) {
        this.ocsUrl = ocsUrl;
    }
    
    private List<String> managementIpBlackList = new ArrayList<>();
    {
        managementIpBlackList.add("0.0.0.0");
        managementIpBlackList.add("127.0.0.1");
        managementIpBlackList.add("10.10.11.");
        managementIpBlackList.add("10.11.11.");
        managementIpBlackList.add("192.168.1.");

        managementIpBlackList.add("10.202.201.");
        managementIpBlackList.add("10.200.131.");
        managementIpBlackList.add("192.168.10.");
    }
    private List<String> managementIpWhiteList = new ArrayList<>();

    {
        managementIpWhiteList.add("192.168.211.");
        managementIpWhiteList.add("192.168.210.");
    }
  
    private RequisitionNode mapComputerToRequisitionNode(Computer computer) {
        RequisitionNode requisitionNode = new RequisitionNode();
        requisitionNode.setForeignId(computer.getHardware().getId() + "");
        requisitionNode.setNodeLabel(computer.getHardware().getName());

        RequisitionInterface requisitionInterface = new RequisitionInterface();
        Network managementNetwork = selectManagementNetwork(computer);
        requisitionInterface.setIpAddr(managementNetwork.getIPAddress());
        requisitionInterface.setDescr(managementNetwork.getDescription());
        requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY);
        requisitionInterface.setManaged(Boolean.TRUE);
        requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("SNMP"));
        requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("ICMP"));
        requisitionNode.getAssets().add(new RequisitionAsset("operatingSystem", computer.getHardware().getOsname()));

        String ocsComputerLink = "<a href=" + ocsUrl + "/index.php?function=computer&head=1&systemid=" + requisitionNode.getForeignId() + ">OCS-Inventory</a>";
        requisitionNode.getAssets().add(new RequisitionAsset("comment", ocsComputerLink));

        requisitionNode.getAssets().add(new RequisitionAsset("cpu", computer.getHardware().getProcessort()));

        requisitionNode.getInterfaces().add(requisitionInterface);

        return requisitionNode;
    }

    private Boolean isManagementIp(String ipAddress) {
        String lastNumberOfThirdOcted;
        if (!ipAddress.isEmpty() && !isManagementIpBlackListed(ipAddress)) {

            if (isManagementIpWhiteListed(ipAddress)) {
                return true;
            }

            lastNumberOfThirdOcted = ipAddress.substring(ipAddress.lastIndexOf(".") - 1, ipAddress.lastIndexOf("."));
            if (lastNumberOfThirdOcted.equals("1")) {
                return true;
            }
        }
        return false;
    }

    private Boolean isManagementIpBlackListed(String ipAddress) {
        for (String blackedIp : managementIpBlackList) {
            if (ipAddress.startsWith(blackedIp)) {
                return true;
            }
        }
        return false;
    }

    private Boolean isManagementIpWhiteListed(String ipAddress) {
        for (String whiteIp : managementIpWhiteList) {
            if (ipAddress.startsWith(whiteIp)) {
                return true;
            }
        }
        return false;
    }

    private Network selectManagementNetwork(Computer computer) {
        if (isManagementIp(computer.getHardware().getIpaddr())) {
            // find the network that was selected by OCS
            for (Network network : computer.getNetworks()) {
                if (network.getIPAddress().equals(computer.getHardware().getIpaddr())) {
                    return network;
                }
            }
        }

        // find any network that has a managementIp
        for (Network network : computer.getNetworks()) {
            if (isManagementIp(network.getIPAddress())) {
                return network;
            }
        }

        // set the OCS default interface if it exists as a network
        for (Network network : computer.getNetworks()) {
            if (network.getIPAddress().equals(computer.getHardware().getIpaddr())) {
                return network;
            }
        }

        // set the first OCS interface
        return computer.getNetworks().get(0);
    }

    public Requisition mapComputersToRequisition(Computers computers) {
        Requisition requisition = new Requisition();
        RequisitionNode requisitionNode;
        for (Computer computer : computers.getComputers()) {
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
