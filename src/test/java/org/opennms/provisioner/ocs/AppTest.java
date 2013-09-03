package org.opennms.provisioner.ocs;

//import java.io.File;
//import java.net.MalformedURLException;
//import java.util.ArrayList;
//import java.util.List;
//import javax.xml.bind.JAXBContext;
//import javax.xml.bind.Marshaller;
//import javax.xml.soap.SOAPException;
//import org.junit.Before;
//import org.junit.Test;
//import org.opennms.forge.provisioningrestclient.api.RequisitionManager;
//import org.opennms.forge.restclient.utils.OnmsRestConnectionParameter;
//import org.opennms.forge.restclient.utils.RestConnectionParameter;
//import org.opennms.netmgt.model.PrimaryType;
//import org.opennms.netmgt.provision.persist.requisition.Requisition;
//import org.opennms.netmgt.provision.persist.requisition.RequisitionAsset;
//import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface;
//import org.opennms.netmgt.provision.persist.requisition.RequisitionMonitoredService;
//import org.opennms.netmgt.provision.persist.requisition.RequisitionNode;
//import org.opennms.ocs.inventory.client.request.logic.OcsInventoryClientLogic;
//import org.opennms.ocs.inventory.client.request.logic.OcsInventoryClientLogicImp;
//import org.opennms.ocs.inventory.client.response.Computer;
//import org.opennms.ocs.inventory.client.response.Computers;
//import org.opennms.ocs.inventory.client.response.Network;

public class AppTest {

//    private String ocsUrl = "http://the-best-ip-ever/ocsinterface";
//    private String ocsUsername = "user";
//    private String ocsPassword = "pass";
//
//    private OcsInventoryClientLogic ocsClient = new OcsInventoryClientLogicImp();
//    private String onmsUrl = "http://the-best-ip-ever:8980/opennms/";
//    private String onmsUsername = "user";
//    private String onmsPassword = "pass";
//    private String onmsFroeignSource = "OCS";
//    private RestConnectionParameter restConnectionParameter;
//
//    private List<String> managementIpBlackList = new ArrayList<String>();
//    {
//        managementIpBlackList.add("0.0.0.0");
//        managementIpBlackList.add("127.0.0.1");
//        managementIpBlackList.add("10.10.11.");
//        managementIpBlackList.add("10.11.11.");
//        managementIpBlackList.add("192.168.1.");
//        
//        managementIpBlackList.add("10.202.201.");
//        managementIpBlackList.add("10.200.131.");
//        managementIpBlackList.add("192.168.10.");
//    }
//    
//    private List<String> managementIpWhiteList = new ArrayList<String>();
//    {
//        managementIpWhiteList.add("192.168.211.");
//        managementIpWhiteList.add("192.168.210.");
//    }
//
//    @Before
//    public void setup() throws SOAPException, MalformedURLException {
//        ocsClient.init(ocsUrl, ocsUsername, ocsPassword);
//        restConnectionParameter = new OnmsRestConnectionParameter(onmsUrl, onmsUsername, onmsPassword);
//    }
//
//    @Test
//    public void testRequisitionBuildUp() throws SOAPException, Exception {
//        Computers computers = ocsClient.getComputers();
//
//        RequisitionManager requisitionManager = new RequisitionManager(restConnectionParameter, onmsFroeignSource);
//        Requisition requisition = requisitionManager.getRequisition();
//
//        for (Computer computer : computers.getComputers()) {
//            requisition.getNodes().add(mapComputerToRequisitionNode(computer));
//        }
//
//        //Write the requisition to disc, still requires rest calls to opennms...
//        if (true) {
//            JAXBContext jaxbContext = JAXBContext.newInstance(Requisition.class);
//            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
//
//            // output pretty printed
//            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
//
//            jaxbMarshaller.marshal(requisition, new File("/tmp/requisition.xml"));
////            jaxbMarshaller.marshal(requisition, System.out);
//        }
//        //Send the Requisition to OpenNMS
//        if (false) {
//            requisitionManager.sendManagedRequisitionToOpenNMS();
//            //Reload all to work arround a 1.12 provisioning change
//            new RequisitionManager(restConnectionParameter, onmsFroeignSource);
//        }
//    }
//
//    private RequisitionNode mapComputerToRequisitionNode(Computer computer) {
//        RequisitionNode requisitionNode = new RequisitionNode();
//        requisitionNode.setForeignId(computer.getHardware().getId() + "");
//        requisitionNode.setNodeLabel(computer.getHardware().getName());
//        
//        RequisitionInterface requisitionInterface = new RequisitionInterface();
//        Network managementNetwork = selectManagementNetwork(computer);
//        requisitionInterface.setIpAddr(managementNetwork.getIPAddress());
//        requisitionInterface.setDescr(managementNetwork.getDescription());
////        requisitionInterface.setIpAddr(computer.getHardware().getIpaddr());
//        requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY);
//        requisitionInterface.setManaged(Boolean.TRUE);
//        requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("SNMP"));
//        requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("ICMP"));
//        requisitionNode.getAssets().add(new RequisitionAsset("operatingSystem", computer.getHardware().getOsname()));
//        
//        String ocsComputerLink = "<a href=" + ocsUrl + "/index.php?function=computer&head=1&systemid=" + requisitionNode.getForeignId() + ">OCS-Inventory</a>";
//        requisitionNode.getAssets().add(new RequisitionAsset("comment", ocsComputerLink));
//        
//        requisitionNode.getAssets().add(new RequisitionAsset("cpu", computer.getHardware().getProcessort()));
//        
//        requisitionNode.getInterfaces().add(requisitionInterface);
//
//        return requisitionNode;
//    }
//
//    private Boolean isManagementIp(String ipAddress) {
//        String lastNumberOfThirdOcted;
//        if (!ipAddress.isEmpty() && !isManagementIpBlackListed(ipAddress)) {
//            
//            if (isManagementIpWhiteListed(ipAddress)) {
//                return true;
//            }
//            
//            lastNumberOfThirdOcted = ipAddress.substring(ipAddress.lastIndexOf(".") - 1, ipAddress.lastIndexOf("."));
//            if (lastNumberOfThirdOcted.equals("1")) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private Boolean isManagementIpBlackListed(String ipAddress) {
//        for (String blackedIp : managementIpBlackList) {
//            if (ipAddress.startsWith(blackedIp)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private Boolean isManagementIpWhiteListed(String ipAddress) {
//        for (String whiteIp : managementIpWhiteList) {
//            if (ipAddress.startsWith(whiteIp)) {
//                return true;
//            }
//        }
//        return false;
//    }
//    
//    private Network selectManagementNetwork(Computer computer) {
//        if (isManagementIp(computer.getHardware().getIpaddr())) {
//            // find the network that was selected by OCS
//            for (Network network : computer.getNetworks()) {
//                if (network.getIPAddress().equals(computer.getHardware().getIpaddr())) {
//                    return network;
//                }
//            }
//        }
//
//        // find any network that has a managementIp
//        for (Network network : computer.getNetworks()) {
//            if (isManagementIp(network.getIPAddress())) {
//                return network;
//            }
//        }
//        
//        // set the OCS default interface if it exists as a network
//        for (Network network : computer.getNetworks()) {
//            if (network.getIPAddress().equals(computer.getHardware().getIpaddr())) {
//                return network;
//            }
//        }
//        
//        // set the first OCS interface
//        return computer.getNetworks().get(0);
//    }
    
}
