/**
 * *****************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc. OpenNMS(R) is Copyright (C)
 * 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OpenNMS(R). If not, see: http://www.gnu.org/licenses/
 *
 * For more information contact: OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/ http://www.opennms.com/
 ******************************************************************************
 */
package org.opennms.opennms.pris.plugins.vmware.source;

import com.vmware.vim25.HostNetworkInfo;
import com.vmware.vim25.HostVirtualNic;
import com.vmware.vim25.mo.*;
import org.kohsuke.MetaInfServices;
import org.opennms.pris.model.*;
import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.api.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.HashSet;
import org.opennms.pris.util.InterfaceUtils;

/**
 *
 */
public class VmwareHostSystemSource extends AbstractVmwareSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(VmwareHostSystemSource.class);

    private Requisition requisition;
    private InterfaceUtils interfaceUtils;
    
    /**
     * Constant for searching in VI Java API only for "HostSystem"
     */
    private static final String VMWARE_HOST_SYSTEM = "HostSystem";

    private VmwareHostSystemSource(final InstanceConfiguration config) {
        super(config);
    }

    @Override
    public Object dump() throws Exception {
        
        interfaceUtils = new InterfaceUtils(this.getConfig());
        interfaceUtils.initListsFromConfigs();
        
        requisition = new Requisition().withForeignSource(this.getInstance());

        // Connection to vCenter
        ServiceInstance serviceInstance = new ServiceInstance(getUrl(), getUsername(), getPassword(), true);
        InventoryNavigator inventoryNavigator = new InventoryNavigator(serviceInstance.getRootFolder());

        // get all ESX HostSystems
        ManagedEntity[] managedEntities = inventoryNavigator.searchManagedEntities(VMWARE_HOST_SYSTEM);
        LOGGER.info("Found '{}' ESX host systems from '{}'", managedEntities.length, getUrl());

        // Get all IPv4 and IPv6 addresses from each Host System
        for (ManagedEntity managedEntity : managedEntities) {
            HostSystem hostSystem = (HostSystem) managedEntity;

            if (hostSystem == null) {
                LOGGER.error("Unable to retrieve IP address for host system. Reason: host system is null");
            } else {
                LOGGER.debug("--- START :: Building requisition node for: '{}'", hostSystem.getName());
                RequisitionNode requisitionNode = new RequisitionNode();

                // Set node and foreignId
                requisitionNode.setNodeLabel(defaultStringWhenNull(hostSystem.getName(), "UNKNOWN"));

                LOGGER.debug("Try to assign foreignId");
                requisitionNode.setForeignId(defaultStringWhenNull(hostSystem.getMOR().getVal(), "UNKNOWN"));

                // Assign category for type and VMware major version, e.g. 5.1 -> "VMware5"
                LOGGER.debug("Try to assign major API version");
                requisitionNode.getCategories().add(new RequisitionCategory("VMware" + getMajorApiVersion(serviceInstance)));

                // Asset field mapping
                LOGGER.debug("Try to assign asset CPU");
                requisitionNode.getAssets().add(new RequisitionAsset(AssetField.cpu.name, hostSystem.getSummary().getHardware().getCpuModel() + " (" + hostSystem.getSummary().getHardware().getNumCpuCores() + " cores)"));

                LOGGER.debug("Try to assign asset VMware managed object ID");
                requisitionNode.getAssets().add(new RequisitionAsset(AssetField.vmwareManagedObjectId.name, defaultStringWhenNull(hostSystem.getMOR().getVal(), "UNKNOWN")));

                LOGGER.debug("Try to assign asset VMware managed entity type");
                requisitionNode.getAssets().add(new RequisitionAsset(AssetField.vmwareManagedEntityType.name, VMWARE_HOST_SYSTEM));

                LOGGER.debug("Try to assign asset VMware management server");
                requisitionNode.getAssets().add(new RequisitionAsset(AssetField.vmwareManagementServer.name, getVcenterIp()));

                LOGGER.debug("Try to assign asset VMware power state");
                requisitionNode.getAssets().add(new RequisitionAsset(AssetField.vmwareState.name, hostSystem.getRuntime().getPowerState().toString()));

                LOGGER.debug("Try to assign asset comment field with VMware getAboutInfo()");
                requisitionNode.getAssets().add(new RequisitionAsset(AssetField.comment.name, "Management Server IP: " + getVcenterIp() + ", Full name: " + serviceInstance.getAboutInfo().getFullName() + ", vCenter OS: " + serviceInstance.getAboutInfo().getOsType()));

                addInterfacesToHostSystem(hostSystem, requisitionNode);

            }
            LOGGER.debug("--- E N D :: building requisition for node: '{}'", hostSystem.getName());
        }

        return requisition;
    }

    /**
     * Get all virtual NICs from a VMware Host-Systems.
     *
     * @param hostSystem requested IP addresses by given VMware Host-System
     * @return Set of virtual NICS from the Host-System
     */
    private HashSet<HostVirtualNic> getAllHostSystemIpAddresses(HostSystem hostSystem) {
        HashSet<HostVirtualNic> hostVirtualNicsResult = new HashSet<>();
        HostNetworkSystem hostNetworkSystem = null;

        try {
            hostNetworkSystem = hostSystem.getHostNetworkSystem();

            if (hostNetworkSystem != null) {
                HostNetworkInfo hostNetworkInfo = hostNetworkSystem.getNetworkInfo();
                if (hostNetworkInfo != null) {
                    HostVirtualNic[] hostVirtualNics = hostNetworkInfo.getConsoleVnic();
                    if (hostVirtualNics != null) {
                        for (HostVirtualNic hostVirtualNic : hostVirtualNics) {
                            hostVirtualNicsResult.add(hostVirtualNic);
                            LOGGER.debug("'{}':: Add Host System console virtual NIC '{}'", hostSystem.getName(), hostVirtualNic.getSpec().getIp().getIpAddress());
                        }
                    } else {
                        LOGGER.debug("No Console Virtual NIC found.");
                    }
                    hostVirtualNics = hostNetworkInfo.getVnic();
                    if (hostVirtualNics != null) {
                        for (HostVirtualNic hostVirtualNic : hostVirtualNics) {
                            hostVirtualNicsResult.add(hostVirtualNic);
                            LOGGER.debug("'{}':: Add Host System virtual NIC IP '{}'", hostSystem.getName(), hostVirtualNic.getSpec().getIp().getIpAddress());
                        }
                    }
                } else {
                    LOGGER.warn("No Host Network information for Host System '{}' available.", hostSystem.getName());
                }
            } else {
                LOGGER.warn("No Host Network system for '{}' found.", hostSystem.getName());
            }

        } catch (RemoteException e) {
            LOGGER.error("No connection to Host System '{}' possible. Error message: '{}'", hostSystem.getName(), e);
        }

        return hostVirtualNicsResult;
    }

    private void addInterfacesToHostSystem(HostSystem hostSystem, RequisitionNode requisitionNode) {
        // Get all IP addresses from HostSystem
        LOGGER.debug("------ START :: building IP interfaces for node: '{}'", hostSystem.getName());
        HashSet<HostVirtualNic> allHostVirtualNics = getAllHostSystemIpAddresses(hostSystem);
        LOGGER.debug("Virtual NICs found: '{}'", allHostVirtualNics.size());

        // Go through all virtual nics and assign for each IP address a IP interface in the OpenNMS node
        for (HostVirtualNic hostVirtualNic : allHostVirtualNics) {
            // Assign all ip interfaces to an requisition
            RequisitionInterface requisitionInterface = new RequisitionInterface();
            requisitionInterface.setIpAddr(hostVirtualNic.getSpec().getIp().getIpAddress());

            // Assign VMware network port group as interface description
            requisitionInterface.setDescr(hostVirtualNic.getPortgroup());

            // Set SNMP primary interface to N
            requisitionInterface.setSnmpPrimary(PrimaryType.SECONDARY);

            // 3 = not monitored / 1 = monitored
            requisitionInterface.setStatus(1);

            requisitionInterface.setManaged(true);
            
            if (interfaceUtils.isIpWhiteListed(requisitionInterface.getIpAddr()) && !interfaceUtils.isIpBlackListed(requisitionInterface.getIpAddr())) {
                requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY);
                requisitionInterface.getMonitoredServices().add(new RequisitionMonitoredService(null, "VMwareCim-HostSystem"));
            }
            requisitionNode.getInterfaces().add(requisitionInterface);
        }
        LOGGER.debug("------ E N D :: building IP interfaces for node: '{}'", hostSystem.getName());
        requisition.getNodes().add(requisitionNode);
    }

    /**
     * Factory used in the RequisitionProvider to initialize this Source.
     */
    @MetaInfServices
    public static class Factory implements Source.Factory {

        @Override
        public String getIdentifier() {
            return "vmware.hostsystem";
        }

        @Override
        public Source create(final InstanceConfiguration config) {
            return new VmwareHostSystemSource(config);
        }
    }

    /**
     * Return the major API version for this management server
     *
     * @return the major API version
     */
    public int getMajorApiVersion(ServiceInstance serviceInstance) {
        if (serviceInstance != null) {
            String apiVersion = serviceInstance.getAboutInfo().getApiVersion();

            String[] arr = apiVersion.split("\\.");

            if (arr.length > 1) {
                int apiMajorVersion = Integer.valueOf(arr[0]);

                if (apiMajorVersion < 4) {
                    apiMajorVersion = 3;
                }

                return apiMajorVersion;
            } else {
                LOGGER.error("Cannot parse vCenter API version '{}'", apiVersion);

                return 0;
            }
        } else {
            return 0;
        }
    }
}
