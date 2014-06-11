/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R). If not, see:
 * http://www.gnu.org/licenses/
 *
 * For more information contact:
 * OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/
 * http://www.opennms.com/
 *******************************************************************************/
package org.opennms.opennms.pris.plugins.vmware.source;

import com.vmware.vim25.GuestNicInfo;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import org.kohsuke.MetaInfServices;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.model.PrimaryType;
import org.opennms.netmgt.provision.persist.requisition.*;
import org.opennms.pris.api.AssetField;
import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.api.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of a the vmware.guest.source
 * <p/>
 * This class will connect to VMware vCenter with a given configuration with vCenter
 * IP address and user credentials.
 * <p/>
 * The dump() method establish the connection and will fetch only virtual machines from
 * the vCenter database.
 * <p/>
 * Because of the nature of the VMware API we build a OpenNMS requisition directly for
 * each virtual machine. The following asset fields has to be populated to be able to
 * collect performance data and monitor the power status:
 * <p/>
 * - Node label
 * - Foreign ID
 * - Surveillance category with with the scheme VMware{Major-API-Version}, e.g. Vmware5, Vmware4
 * - VMware managed object ID
 * - VMware management IP
 */
public class VmwareGuestSource extends AbstractVmwareSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(VmwareGuestSource.class);

    /**
     * Constant for searching in VI Java API only for "VirtualMachine"
     */
    private static final String VMWARE_GUEST_SYSTEM = "VirtualMachine";

    private VmwareGuestSource(final InstanceConfiguration config) {
        super(config);
    }

    /**
     * Method reads all data from vCenter and fetches just Guest systems from vCenter. The method creates a complete
     * OpenNMS node requisition which can be imported. Required asset fields are mapped. Additionally a few system
     * information are also mapped to the OpenNMS requisition node.
     *
     * @return {@link org.opennms.netmgt.provision.persist.requisition.Requisition} OpenNMS Requisition which can be imported
     * @throws Exception
     */
    @Override
    public Object dump() throws Exception {

        Requisition requisition = new Requisition(this.getInstance());

        // Connection to vCenter
        ServiceInstance serviceInstance = new ServiceInstance(getUrl(), getUsername(), getPassword(), true);
        InventoryNavigator inventoryNavigator = new InventoryNavigator(serviceInstance.getRootFolder());

        // get all virtual machines
        ManagedEntity[] managedEntities = inventoryNavigator.searchManagedEntities(VMWARE_GUEST_SYSTEM);
        LOGGER.info("Found '{}' virtual machines from '{}'", managedEntities.length, getUrl());

        // Get all IPv4 and IPv6 addresses from each virtual machine
        for (ManagedEntity managedEntity : managedEntities) {
            VirtualMachine virtualMachine = (VirtualMachine) managedEntity;

            if (virtualMachine == null) {
                LOGGER.error("Unable to retrieve IP address for virtual machine. Reason: virtual machine is null");
            } else {
                LOGGER.debug("--- START :: Building requisition node for: " + virtualMachine.getName());
                RequisitionNode requisitionNode = new RequisitionNode();

                /*
                 * Mapping required fields for performing data collection or status monitoring
                 *
                 * Set node and foreignId
                 */
                LOGGER.debug("Try to assign virtual machine name");
                requisitionNode.setNodeLabel(defaultStringWhenNull(virtualMachine.getName(), "UNKNOWN"));

                LOGGER.debug("Try to assign foreignId");
                requisitionNode.setForeignId(defaultStringWhenNull(virtualMachine.getMOR().getVal(), "UNKNOWN"));

                // Assign category for type and VMware major version, e.g. 5.1 -> "VMware5"
                LOGGER.debug("Try to assign node categories");
                requisitionNode.getCategories().add(new RequisitionCategory("VMware" + getMajorApiVersion(serviceInstance)));

                LOGGER.debug("Try to assign asset VMware managed object ID");
                requisitionNode.getAssets().add(new RequisitionAsset(AssetField.vmwareManagementServer.name, getVcenterIp()));

                /*
                 * Mapping just additional information, no functionality bind for monitoring purposes
                 *
                 * Asset field mapping, just for information, no monitoring functionality bound
                 */
                LOGGER.debug("Try to assign asset operating system");
                requisitionNode.getAssets().add(new RequisitionAsset(AssetField.operatingSystem.name, defaultStringWhenNull(virtualMachine.getGuest().getGuestFullName(), "UNKNOWN")));

                LOGGER.debug("Try to assign asset CPU");
                requisitionNode.getAssets().add(new RequisitionAsset(AssetField.cpu.name, virtualMachine.getConfig().getHardware().getNumCPU() + " vCPU"));

                LOGGER.debug("Try to assign asset RAM");
                requisitionNode.getAssets().add(new RequisitionAsset(AssetField.ram.name, virtualMachine.getConfig().getHardware().getMemoryMB() + " MB"));

                LOGGER.debug("Try to assign asset VMware power state");
                requisitionNode.getAssets().add(new RequisitionAsset(AssetField.vmwareState.name, virtualMachine.getRuntime().getPowerState().toString()));

                LOGGER.debug("Try to assign asset comment field with VMware getAboutInfo()");
                requisitionNode.getAssets().add(new RequisitionAsset(AssetField.comment.name, "Full name: " + serviceInstance.getAboutInfo().getFullName() + ", vCenter OS: " + serviceInstance.getAboutInfo().getOsType()));

                // Get all IP addresses from virtual machine and assign to OpenNMS node for import
                Set<InetAddress> vmwareIpAddresses = getVirtualMachineIpAddresses(virtualMachine);

                if (vmwareIpAddresses != null) {
                    for (InetAddress vmwareIpAddress : vmwareIpAddresses) {

                        // Assign IP address to OpenNMS node
                        RequisitionInterface requisitionInterface = new RequisitionInterface();

                        if (!vmwareIpAddress.isLoopbackAddress() && !vmwareIpAddress.isLinkLocalAddress()) {
                            requisitionInterface.setIpAddr(vmwareIpAddress.getHostAddress());

                            // Set to 3 (not monitored) - 1 (is monitored)
                            requisitionInterface.setStatus(1);

                            // Set the interface as non SNMP primary interface
                            requisitionInterface.setSnmpPrimary(PrimaryType.NOT_ELIGIBLE);

                            requisitionNode.getInterfaces().add(requisitionInterface);
                        }
                    }
                } else {
                    LOGGER.warn("No IP interface reported from VMware tools on '{}", virtualMachine.getName());
                }

                requisition.getNodes().add(requisitionNode);
            }
            LOGGER.debug("------ E N D :: building IP interfaces for VM Guest: '{}'", virtualMachine.getName());
            LOGGER.debug("--- E N D :: building requisition for VM Guest: '{}'", virtualMachine.getName());
        }

        return requisition;
    }

    /**
     * Factory used in the RequisitionProvider to initialize this Source.
     */
    @MetaInfServices
    public static class Factory implements Source.Factory {
        @Override
        public String getIdentifier() {
            return "vmware.guest";
        }

        @Override
        public Source create(final InstanceConfiguration config) {
            return new VmwareGuestSource(config);
        }
    }

    /**
     * Return the major API version for this management server
     *
     * @return the major API version
     */
    private int getMajorApiVersion(ServiceInstance serviceInstance) {
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

    /**
     * Searches for all ip addresses of a virtual machine
     *
     * @param virtualMachine the virtual machine to query
     * @return the ip addresses of the virtual machine, the first one is the primary
     * @throws RemoteException
     */
    private Set<InetAddress> getVirtualMachineIpAddresses(VirtualMachine virtualMachine) throws RemoteException, UnknownHostException {
        Set<InetAddress> ipAddresses = new HashSet<>();

        // add the Ip address reported by VMware tools, this should be primary
        String vmwareToolsIp = virtualMachine.getGuest().getIpAddress();

        if (vmwareToolsIp != null) {
            ipAddresses.add(InetAddress.getByName(vmwareToolsIp));
        }

        // if possible, iterate over all virtual networks networks and add interface Ip addresses
        if (virtualMachine.getGuest().getNet() != null) {
            for (GuestNicInfo guestNicInfo : virtualMachine.getGuest().getNet()) {
                if (guestNicInfo.getIpAddress() != null) {
                    for (String ipAddress : guestNicInfo.getIpAddress()) {
                        ipAddresses.add(InetAddressUtils.getInetAddress(ipAddress));
                    }
                }
            }
        }

        return ipAddresses;
    }
}

