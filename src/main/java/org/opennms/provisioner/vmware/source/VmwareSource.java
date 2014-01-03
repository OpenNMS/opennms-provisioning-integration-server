package org.opennms.provisioner.vmware.source;

//import com.vmware.vim25.GuestNicInfo;
//import com.vmware.vim25.HostNetworkInfo;
//import com.vmware.vim25.HostVirtualNic;
//import com.vmware.vim25.mo.*;
import org.apache.commons.configuration.Configuration;
import org.opennms.provisioner.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class VmwareSource implements Source {

    private static final Logger logger = LoggerFactory.getLogger(VmwareSource.class);

    private final String instance;

    private final Configuration config;

    private VmwareSource(final String instance, final Configuration config) {
        this.instance = instance;
        this.config = config;
    }

    @Override
    public Object dump() throws Exception {
//        ServiceInstance serviceInstance = new ServiceInstance(new URL(getUrl()), getUsername(), getPassword(), true);
//        InventoryNavigator inventoryNavigator = new InventoryNavigator(serviceInstance.getRootFolder());
//        ManagedEntity[] managedEntities = inventoryNavigator.searchManagedEntities(getType());
//
//        HashSet<String> vmIpAddr = new HashSet<>();
//        HashSet<String> hostSystemIpAddr = new HashSet<>();
//
//        switch (getType()) {
//            case "VirtualMachine":
//                // Get all IPv4 and IPv6 addresses from  Virtual Machine
//                for (ManagedEntity managedEntity : managedEntities) {
//                    VirtualMachine virtualMachine = (VirtualMachine) managedEntity;
//                    if (virtualMachine == null) {
//                        logger.error("Unable to retrieve IP address for virtual machine. Reason: virtual machine is null");
//                    } else {
//                        vmIpAddr = getAllVmIpAddresses(virtualMachine);
//                    }
//
//                }
//                break;
//            case "HostSystem":
//                // Get all IPv4 and IPv6 addresses from Host System
//                for (ManagedEntity managedEntity : managedEntities) {
//                    HostSystem hostSystem = (HostSystem) managedEntity;
//                    if (hostSystem == null) {
//                        logger.error("Unable to retrieve IP address for virtual machine. Reason: virtual machine is null");
//                    } else {
//                        hostSystemIpAddr = getAllHostSystemIpAddresses(hostSystem);
//                    }
//                }
//                break;
//            default:
//                logger.error("No valid vmware.type in requisition.properties found. Valid types are: HostSystem or VirtualMachine");
//                break;
//        }
//
//        return new ArrayList<ManagedEntity>(Arrays.asList(managedEntities));
        return null;
    }

    public final String getUrl() {
        return this.config.getString("vmware.url");
    }

    public final String getUsername() {
        return this.config.getString("vmware.username");
    }

    public final String getPassword() {
        return this.config.getString("vmware.password");
    }

    public final String getType() {
        return this.config.getString("vmware.type");
    }

    /**
     * Get all IP addresses for a virtual machine. It includes the reported IP from
     * VMware tools Guest. Additionally networks will be retrieved and if available
     * added to the set IP addresses.
     *
     * @param virtualMachine requested IP addresses by given virtual machine
     * @return Set of IP addresses from Virtual Machine Guest and additional NICs.
     */
//    private HashSet<String> getAllVmIpAddresses(VirtualMachine virtualMachine) {
//        HashSet<String> ipAddresses = new HashSet<>();
//
//        // add the Ip address reported by VMware tools
//        if (virtualMachine.getGuest().getIpAddress() != null) {
//            ipAddresses.add(virtualMachine.getGuest().getIpAddress());
//            logger.debug("'{}':: Add IP address from VMware Guest '{}' to address list.", virtualMachine.getName(), virtualMachine.getGuest().getIpAddress());
//        } else {
//            logger.warn("'{}':: Couldn't retrieve IP address from VMware Guest. IP address is null", virtualMachine.getName());
//        }
//
//        // get all other IP addresses assigned to the VM
//        if (virtualMachine.getGuest().getNet() != null) {
//            for (GuestNicInfo guestNicInfo : virtualMachine.getGuest().getNet()) {
//                if (guestNicInfo.getIpAddress() != null) {
//                    for (String ipAddress : guestNicInfo.getIpAddress()) {
//                        ipAddresses.add(ipAddress);
//                        logger.debug("'{}':: Virtual Machine Guest NIC IP address '{}' added", virtualMachine.getName(), ipAddress);
//                    }
//                } else {
//                    logger.info("'{}':: No Virtual Machine Guest NIC information available.", virtualMachine.getName());
//                }
//            }
//        } else {
//            logger.warn("No Virtual Machine Guest networks found");
//        }
//
//        return ipAddresses;
//    }

    /**
     * Get all IP addresses for VMware Host-Systems.
     *
     * @param hostSystem requested IP addresses by given VMware Host-System
     * @return Set of IP addresses from Host-System
     */
//    private HashSet<String> getAllHostSystemIpAddresses(HostSystem hostSystem) {
//        HashSet<String> ipAddresses = new HashSet<>();
//        HostNetworkSystem hostNetworkSystem = null;
//
//        try {
//            hostNetworkSystem = hostSystem.getHostNetworkSystem();
//
//            if (hostNetworkSystem != null) {
//                HostNetworkInfo hostNetworkInfo = hostNetworkSystem.getNetworkInfo();
//                if (hostNetworkInfo != null) {
//                    HostVirtualNic[] hostVirtualNics = hostNetworkInfo.getConsoleVnic();
//                    if (hostVirtualNics != null) {
//                        for (HostVirtualNic hostVirtualNic : hostVirtualNics) {
//                            ipAddresses.add(hostVirtualNic.getSpec().getIp().getIpAddress());
//                            logger.debug("'{}':: Add Host System console virtual NIC '{}'", hostSystem.getName(), hostVirtualNic.getSpec().getIp().getIpAddress());
//                        }
//                    } else {
//                        logger.debug("No Console Virtual NIC found.");
//                    }
//                    hostVirtualNics = hostNetworkInfo.getVnic();
//                    if (hostVirtualNics != null) {
//                        for (HostVirtualNic hostVirtualNic : hostVirtualNics) {
//                            ipAddresses.add(hostVirtualNic.getSpec().getIp().getIpAddress());
//                            logger.debug("'{}':: Add Host System virtual NIC IP '{}'", hostSystem.getName(), hostVirtualNic.getSpec().getIp().getIpAddress());
//                        }
//                    }
//                } else {
//                    logger.warn("No Host Network information for Host System '{}' available.", hostSystem.getName());
//                }
//            } else {
//                logger.warn("No Host Network system for '{}' found.", hostSystem.getName());
//            }
//
//        } catch (RemoteException e) {
//            logger.error("No connection to Host System '{}' possible. Error: ", hostSystem.getName(), e.getMessage());
//            logger.trace("Stack trace: '{}'", e.getStackTrace());
//        }
//
//        return ipAddresses;
//    }

    public static class Factory implements Source.Factory {

        @Override
        public Source create(final String instance, final Configuration config) {
            return new VmwareSource(instance, config);
        }
    }
}
