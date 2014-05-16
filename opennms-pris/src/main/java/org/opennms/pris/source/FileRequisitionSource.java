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
 * *****************************************************************************
 */
package org.opennms.pris.source;

import java.io.File;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.configuration.Configuration;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.pris.Starter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A OpenNMS requisition provided as a file
 */
public class FileRequisitionSource implements Source {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileRequisitionSource.class);

  /**
     * the name of the resulting requisition
     */
    private final String instance;
    private final Configuration config;

    private FileRequisitionSource(final String instance, final Configuration config) {
        this.instance = instance;
        this.config = config;
    }

    @Override
    public Object dump() throws Exception {
        LOGGER.info("FileRequisitionSource started for requisition '{}'", instance);
        Requisition requisition = null;

        if (getFile() != null) {
            File requisitionFile = Starter.getConfigManager().getInstancePath(this.instance).resolve(getFile()).toFile();
            LOGGER.debug("working with file '{}'", requisitionFile.getAbsolutePath());
            
            if (requisitionFile.canRead()) {

                try {
                    JAXBContext jaxbContext = JAXBContext.newInstance(Requisition.class);
                    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                    requisition = (Requisition) jaxbUnmarshaller.unmarshal(requisitionFile);

                } catch (JAXBException e) {
                    LOGGER.error("The file did not contain a valid requisition as xml.", e);
                }
            } else {
                LOGGER.error("Can not read requisition file '{}'", requisitionFile.getAbsolutePath());
            }

        } else {
            LOGGER.error("Parameter 'file' is missing in requisition.properties");
        }
        if (requisition == null) {
            LOGGER.error("Requisition is null for unkown reasons");
            return null;
        }
        LOGGER.info("FileRequisitionSource delivered for requisition '{}' '{}'", instance, requisition.getNodes().size());
        return requisition;
    }

    public final String getFile() {
        return this.config.getString("file");
    }

    public static class Factory implements Source.Factory {

        @Override
        public Source create(final String instance, final Configuration config) {
            return new FileRequisitionSource(instance, config);
        }
    }
}
