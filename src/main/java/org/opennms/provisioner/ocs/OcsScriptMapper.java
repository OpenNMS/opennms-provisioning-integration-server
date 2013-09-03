package org.opennms.provisioner.ocs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.apache.commons.codec.Charsets;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.ocs.inventory.client.response.Computers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcsScriptMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcsScriptMapper.class);
    private String foreignSource;
    private String ocsUrl;
    private String mapper;

    public OcsScriptMapper(String foreignSource, String ocsUrl, String mapper) {
        this.foreignSource = foreignSource;
        this.ocsUrl = ocsUrl;
        this.mapper = mapper;
    }

    public Requisition mapComputersToRequisition(Computers computers) {
        Requisition requisition = null;
        try {
            List<String> allScriptLines = Files.readAllLines(new File(mapper).toPath(), Charsets.UTF_8);
            String mapperScript = "";
            for (String line : allScriptLines) {
                mapperScript = mapperScript.concat(line + "\n");
            }

            BSFManager manager = new BSFManager();
            manager.declareBean("computers", computers, Computers.class);
            manager.declareBean("foreignSource", foreignSource, String.class);
            manager.declareBean("ocsUrl", ocsUrl, String.class);
            manager.declareBean("mapper", mapper, String.class);
            requisition = (Requisition) manager.eval(mapper.substring(mapper.indexOf(".") + 1), mapper, 0, 0, mapperScript);
        } catch (IOException ex) {
            LOGGER.error("Problems reading script " + mapper, ex);
        } catch (BSFException ex) {
            LOGGER.error("Problem running script " + mapper, ex);
        }
        return requisition;
    }
}