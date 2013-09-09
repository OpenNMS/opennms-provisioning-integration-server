package org.opennms.provisioner.ocs;

import java.util.List;

import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.ocs.inventory.client.request.logic.GetComputersLogic;
import org.opennms.ocs.inventory.client.request.logic.GetSnmpDevicesLogic;
import org.opennms.ocs.inventory.client.response.Computers;
import org.opennms.ocs.inventory.client.response.snmp.SnmpDevices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcsRequisitionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcsRequisitionProvider.class);
    private final String ocsUrl;
    private final String ocsUsername;
    private final String ocsPassword;
    private final String foreignSource;
    private final String mapper;
    private final String checksum;
    private List<String> tags;
    private final String ocsDeviceType;

    public OcsRequisitionProvider(String ocsUrl, String ocsUsername, String ocsPassword, String foreignSource, String mapper, String checksum, List<String> tags, String ocsDeviceType) {
        this.ocsUrl = ocsUrl;
        this.ocsUsername = ocsUsername;
        this.ocsPassword = ocsPassword;
        this.foreignSource = foreignSource;
        this.mapper = mapper;
        this.checksum = checksum;
        this.tags = tags;
        this.ocsDeviceType = ocsDeviceType;
    }

    public Requisition generateRequisition() {
        Requisition requisition = null;
        if ("computers".equalsIgnoreCase(this.ocsDeviceType)) {
            return generateComputersRequisition();
        } else if ("snmpDevices".equalsIgnoreCase(this.ocsDeviceType)) {
            return generateSnmpDevicesRequisition();
        } else {
            LOGGER.error("Value for ocsDeviceType must be one of { computers, snmpDevices }");
        }
        return requisition;
    }
    
    public Requisition generateComputersRequisition() {
        Requisition requisition = null;
        Computers computers = loadComputersFromOcs();
        if (computers != null) {
            if (mapper.equals("default")) {
                OcsDefaultMapper ocsMapper = new OcsDefaultMapper(ocsUrl);
                requisition = ocsMapper.mapComputersToRequisition(computers);
                requisition.setForeignSource(foreignSource);
            } else {
                OcsScriptMapper ocsMapper = new OcsScriptMapper(foreignSource, ocsUrl, mapper);
                requisition = ocsMapper.mapComputersToRequisition(computers);
            }
        }
        return requisition;
    }
    
    public Requisition generateSnmpDevicesRequisition() {
        Requisition requisition = null;
        SnmpDevices snmpDevices = loadSnmpDevicesFromOcs();
        if (snmpDevices != null) {
            if (mapper.equals("default")) {
                OcsDefaultMapper ocsMapper = new OcsDefaultMapper(ocsUrl);
                requisition = ocsMapper.mapSnmpDevicesToRequisition(snmpDevices);
                requisition.setForeignSource(foreignSource);
            } else {
                OcsScriptMapper ocsMapper = new OcsScriptMapper(foreignSource, ocsUrl, mapper);
                requisition = ocsMapper.mapSnmpDevicesToRequisition(snmpDevices);
            }
        }
        return requisition;
    }
    
    private Computers loadComputersFromOcs() {
        GetComputersLogic ocsClient = new GetComputersLogic();
        Computers computers = null;
        try {
            ocsClient.init(ocsUrl, ocsUsername, ocsPassword, checksum, tags);
            computers = ocsClient.getComputers();
        } catch (Exception ex) {
            LOGGER.error("Call to OCS had problems", ex);
        }
        return computers;
    }
    
    private SnmpDevices loadSnmpDevicesFromOcs() {
        GetSnmpDevicesLogic ocsClient = new GetSnmpDevicesLogic();
        SnmpDevices devices = null;
        try {
            ocsClient.init(ocsUrl,  ocsUsername,  ocsPassword, checksum, tags);
            devices=ocsClient.getSnmpDevices();
        } catch (Exception ex) {
            LOGGER.error("Call to OCS had problems", ex);
        }
        return devices;
    }
}
