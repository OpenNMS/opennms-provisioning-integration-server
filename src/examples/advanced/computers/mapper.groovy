import java.lang.StringBuilder;
import java.util.Set;
import java.util.HashSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

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
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.nio.file.Files;

String foreignSource;
String mapper;

final Computers myComputers = data;
Requisition myRequisition = new Requisition(foreignSource);
Set existingForeignIDs = new HashSet();

Properties catMap = new Properties();
try {
    catMap.load(Files.newInputStream(script.getParent().resolve(config.getString("categoryMap", "categorymap.properties"))));
    logger.info("Loaded properties");
} catch (Exception e) {
    logger.error("Could not read category mappings", e);
    throw new RuntimeException(e);
}

// Execution starts here

for (Computer computer : myComputers.getComputers()) {
    logger.info("Processing Computer {}", computer.getHardware().getName());
    if (this.isDisabled(computer)) {
        logger.info("The computer {} is disabled, so it won't be added to the requisition", computer.getHardware().getName());
        continue;
    }
    RequisitionNode rNode = this.getRequisitionNode(computer, catMap);
    // true indicates the set did not already contain this element
    if (existingForeignIDs.add(rNode.getForeignId())) {
        myRequisition.getNodes().add(rNode);
    } else {
        // TODO actually do something useful here
        logger.error("Ignoring duplicate foreign-ID '{}'", rNode.getForeignId());
    }
}

myRequisition = doRequisitionOverlay(myRequisition);

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
    populateCategories(myComputer,   myRequisitionNode, catMap);
    populateCommentLinks(myComputer, myRequisitionNode, config.getString("ocs.url"));

    return myRequisitionNode;
}


public void populateCommentLinks(Computer myComputer, RequisitionNode myRequisitionNode, String ocsUrl) {
    String ocsComputerLink = "<a href=" + ocsUrl + "/index.php?function=computer&head=1&systemid=" + myComputer.getHardware().getId() + ">OCS-Inventory</a>";
    myRequisitionNode.getAssets().add(new RequisitionAsset("comment", ocsComputerLink));
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
        requisitionInterface.setStatus(1);
        requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("ICMP"));
        requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("SNMP"));

        myRequisitionNode.getInterfaces().add(requisitionInterface);
    } else {
        logger.error("No valid interface was found for computer '{}'", myComputer.getHardware().getName());
    }
}

private boolean isDisabled(Computer myComputer) {
    for (Entry entry : myComputer.getAccountInfo().getEntries()) {
        if (entry.getName().equalsIgnoreCase("disabled") && entry.getValue().equalsIgnoreCase("yes")) {
            return true;
        }
    } 
    return false;
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
    
    //duplicate spaces
    result = result.replaceAll("\\s+", " ");
    
    result = result.take(maxSize);    
    return result;
}

private String getOverlayRequisitionUrl(String foreignSource) {
    // FIXME Parameterize this value via config.properties
    // TODO we get foreignSource the right way in newer ocs-integration versions, so use it
    return "file:///opt/opennms/etc/ocs-integration/overlay-requisitions/ocs-computers-overlay.xml";
}

private Requisition loadOverlayRequisition(String foreignSource) {
    Requisition ovlReq;
    try {
        Resource ovlResource = new UrlResource(getOverlayRequisitionUrl(foreignSource));
        JAXBContext jc = JAXBContext.newInstance(Requisition.class);
        Unmarshaller ju = jc.createUnmarshaller();
        ovlReq = ju.unmarshal(ovlResource.getInputStream());
        ovlReq.setResource(ovlResource);
        logger.info("Loaded overlay requisition with {} nodes from {}", ovlReq.getNodeCount(), ovlResource.getDescription());
        return ovlReq;
    } catch (MalformedURLException mue) {
        logger.error("Failed to load overlay requisition for foreign-source {} due to malformed URL", mue, foreignSource);
    } catch (JAXBException jaxbe) {
        logger.error("Failed to unmarshal overlay requisition for foreign-source {}", jaxbe, foreignSource);
    } catch (IOException ioe) {
        logger.error("Failed to load overlay requisition for foreign-source {} due to IOException", ioe, foreignSource);
    }
    return new Requisition();
}

private Requisition doRequisitionOverlay(Requisition myRequisition) {
    Requisition overlayRequisition = loadOverlayRequisition(myRequisition.getForeignSource());
    for (RequisitionNode ovlNode : overlayRequisition.getNodes()) {
        RequisitionNode ocsNode = myRequisition.getNode(ovlNode.getForeignId());
        if (ocsNode == null) continue;
        logger.info("Applying overlay for node with foreign ID {}", ovlNode.getForeignId());
        for (RequisitionInterface ovlIface : ovlNode.getInterfaces()) {
            RequisitionInterface ocsIface = ocsNode.getInterface(ovlIface.getIpAddr());
            if (ocsIface == null) {
                ocsNode.putInterface(ovlIface);
                logger.info("Putting whole interface {} from overlay onto node {}", ovlIface.getIpAddr(), ocsNode.getForeignId());
            } else {
                if (! "".equals(ovlIface.getDescr())) ocsIface.setDescr(ovlIface.getDescr());
                ocsIface.setStatus(ovlIface.getStatus());
                ocsIface.setSnmpPrimary(ovlIface.getSnmpPrimary());
                for (RequisitionMonitoredService ovlSvc : ovlIface.getMonitoredServices()) {
                    ocsNode.getInterface(ovlIface.getIpAddr()).putMonitoredService(ovlSvc);
                    logger.info("Putting service {} on interface {} from overlay onto node {}", ovlSvc.getServiceName(), ovlIface.getIpAddr(), ocsNode.getForeignId());
                }
            }
        }
        for (RequisitionAsset ovlAsset : ovlNode.getAssets()) {
            ocsNode.putAsset(ovlAsset);
            logger.info("Putting asset {} from overlay onto node {}", ovlAsset.getName(), ocsNode.getForeignId());
        }
        for (RequisitionCategory ovlCategory : ovlNode.getCategories()) {
            ocsNode.putCategory(ovlCategory);
            logger.info("Putting category {} from overlay onto node {}", ovlCategory.getName(), ocsNode.getForeignId());
        }
    }
    return myRequisition;
}
