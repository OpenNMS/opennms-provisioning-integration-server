package org.opennms.provisioner.ocs;

import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.ocs.inventory.client.request.logic.OcsInventoryClientLogic;
import org.opennms.ocs.inventory.client.request.logic.OcsInventoryClientLogicImp;
import org.opennms.ocs.inventory.client.response.Computers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcsRequisitionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcsRequisitionProvider.class);
    private final String ocsUrl;
    private final String ocsUsername;
    private final String ocsPassword;
    private final String foreignSource;
    private final String mapper;

    public OcsRequisitionProvider(String ocsUrl, String ocsUsername, String ocsPassword, String foreignSource, String mapper) {
        this.ocsUrl = ocsUrl;
        this.ocsUsername = ocsUsername;
        this.ocsPassword = ocsPassword;
        this.foreignSource = foreignSource;
        this.mapper = mapper;
    }

    public Requisition generateRequisition() {
        Requisition requisition = null;
        Computers computers = loadComputersFromOsc();
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
    
    private Computers loadComputersFromOsc() {
        OcsInventoryClientLogic ocsClient = new OcsInventoryClientLogicImp();
        Computers computers = null;
        try {
            ocsClient.init(ocsUrl, ocsUsername, ocsPassword);
            computers = ocsClient.getComputers();
        } catch (Exception ex) {
            LOGGER.error("Call to OCS had problems", ex);
        }
        return computers;
    }
}
