import java.lang.StringBuilder;
import java.util.Set;
import java.util.HashSet;

import org.opennms.provisioner.ocs.IpInterfaceHelper;
import org.opennms.ocs.inventory.client.response.Bios;
import org.opennms.ocs.inventory.client.response.Computer;
import org.opennms.ocs.inventory.client.response.Computers;
import org.opennms.ocs.inventory.client.response.Entry;
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
Set existingForeignIDs = new HashSet();

Properties catMap = new Properties();
try {
    catMap.load(new FileInputStream("categorymap.properties"));
    logger.info("Loaded properties");
} catch (Exception e) {
    logger.error("Could not read category mappings from " + catMapFile.getAbsolutePath(), e);
    System.exit(1);
}

// Execution starts here
for (Computer computer : myComputers.getComputers()) {
    logger.info("Processing Computer {}", computer.getHardware().getName());
    RequisitionNode rNode = this.getRequisitionNode(computer, catMap);
    // true indicates the set did not already contain this element
    if (existingForeignIDs.add(rNode.getForeignId())) {
        myRequisition.getNodes().add(rNode);
    } else {
        // TODO actually do something useful here
        logger.error("Ignoring duplicate foreign-ID '{}'", rNode.getForeignId());
    }
}

logger.info("Returning {} requisition with {} nodes", myRequisition.getForeignSource(), myRequisition.getNodes().size());
return myRequisition;

private RequisitionNode getRequisitionNode(Computer computer, Properties catMap) {
    RequisitionNode myRequisitionNode = new RequisitionNode();
    Computer myComputer = computer;

    myRequisitionNode.setForeignId(myComputer.getHardware().getName() + "");
    myRequisitionNode.setNodeLabel(myComputer.getHardware().getName());

    populateBiosAssets(myComputer, myRequisitionNode);
    populateCpuAssets(myComputer, myRequisitionNode);
    populateOSAssets(myComputer, myRequisitionNode);
    populateInterfaces(myComputer, myRequisitionNode);
    populateCategories(myComputer, myRequisitionNode, catMap);

    return myRequisitionNode;
}

private void populateBiosAssets(Computer myComputer, RequisitionNode myRequisitionNode) {
    Bios myBios = myComputer.getBios();
    if (myBios != null) {
        myRequisitionNode.getAssets().add(new RequisitionAsset("manufacturer", assetStringCleaner(myBios.getSManufacturer(), 64)));
        myRequisitionNode.getAssets().add(new RequisitionAsset("modelNumber", assetStringCleaner(myBios.getSModel(), 64)));
        myRequisitionNode.getAssets().add(new RequisitionAsset("serialNumber", assetStringCleaner(myBios.getSSN(), 64)));
    }
}

private void populateCpuAssets(Computer myComputer, RequisitionNode myRequisitionNode) {
    StringBuilder cpuStringBuilder = new StringBuilder(String.valueOf(myComputer.getHardware().getProcessorn()))
    .append(" x ").append(String.valueOf(myComputer.getHardware().getProcessors()))
    .append("MHz ").append(myComputer.getHardware().getProcessort());    
    myRequisitionNode.getAssets().add(new RequisitionAsset("cpu", assetStringCleaner(cpuStringBuilder.toString(), 64)));
}

private void populateOSAssets(Computer myComputer, RequisitionNode myRequisitionNode) {
    StringBuilder osStringBuilder = new StringBuilder(myComputer.getHardware().getOsname())
    .append(" ").append(myComputer.getHardware().getOsversion())
    .append(" (").append(myComputer.getHardware().getOscomments()).append(")");
    myRequisitionNode.getAssets().add(new RequisitionAsset("operatingSystem", assetStringCleaner(osStringBuilder.toString(), 64)));
}

private void populateInterfaces(Computer myComputer, RequisitionNode myRequisitionNode) {
    RequisitionInterface requisitionInterface = new RequisitionInterface();
    
    Network managementNetwork = ipInterfaceHelper.selectManagementNetworkWhiteAndBlackOnly(myComputer);
    if (managementNetwork != null) {
        requisitionInterface.setIpAddr(managementNetwork.getIPAddress());
        requisitionInterface.setDescr(managementNetwork?.getDescription());

        requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY);
        requisitionInterface.setManaged(Boolean.TRUE);
        requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("ICMP"));
        requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("SNMP"));

        myRequisitionNode.getInterfaces().add(requisitionInterface);
    } else {
        logger.error("No valid interface was found for computer '{}'", myComputer.getHardware().getName());
    }
}

private void populateCategories(Computer myComputer, RequisitionNode myRequisitionNode, Properties catMap) {
    for (Entry entry : myComputer.getAccountInfo().getEntries()) {
        if ("".equals(entry.getValue())) continue;
        logger.info("On computer {} got an accountinfo entry called {} with value {}", myComputer.getHardware().getName(), entry.getName(), entry.getValue());
        if (catMap.containsKey(entry.getName() + "." + entry.getValue())) {
            myRequisitionNode.putCategory(new RequisitionCategory(catMap.get(entry.getName() + "." + entry.getValue())));
        } else {
            logger.info("NOT Adding category {}.{} to node {}", entry.getName(), entry.getValue(), myComputer.getHardware().getName());
        }
    }
}

private String assetStringCleaner(String assetString, Integer maxSize) {
    
    String result = assetString;
    //Trademarks
    result = result.replace("®", "");
    result = result.replace("(R)", "");
    result = result.replace("(tm)", "");
    
    //OperatingSystems
    result = result.replace("Microsoft", "MS");
    result = result.replace("Service Pack", "SP");
    result = result.replace("CentOS release", "CentOS");
    result = result.replace("Red Hat Enterprise Linux Server release", "Red Hat Linux");
    
    //duplicat spaces
    result = result.replaceAll("\\s+", " ");
    
    result = result.take(maxSize);    
    return result;
}