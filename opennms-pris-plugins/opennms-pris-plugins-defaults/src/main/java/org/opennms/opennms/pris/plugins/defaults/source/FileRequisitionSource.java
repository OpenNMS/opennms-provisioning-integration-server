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
package org.opennms.opennms.pris.plugins.defaults.source;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.kohsuke.MetaInfServices;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.pris.api.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opennms.pris.api.InstanceConfiguration;

/**
 * A OpenNMS requisition provided as a file
 */
public class FileRequisitionSource implements Source {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileRequisitionSource.class);

    private final InstanceConfiguration config;

    private FileRequisitionSource(final InstanceConfiguration config) {
        this.config = config;
    }

    @Override
    public Object dump() throws Exception {
        LOGGER.debug("FileRequisitionSource started for requisition '{}'", config.getInstanceIdentifier());
        
        if (getFile() != null) {
            final Path requisitionFile = getFile().toAbsolutePath();
            LOGGER.debug("working with file '{}'", requisitionFile);
            
            if (Files.isReadable(requisitionFile)) {
                try (final BufferedReader r = Files.newBufferedReader(requisitionFile,
                                                                      StandardCharsets.UTF_8)) {
                    final JAXBContext jaxbContext = JAXBContext.newInstance(Requisition.class);
                    final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                    
                    return (Requisition) jaxbUnmarshaller.unmarshal(r);

                } catch (final JAXBException e) {
                    LOGGER.error("The file did not contain a valid requisition as xml.", e);
                    throw e;
                }
                
            } else {
                LOGGER.error("Can not read requisition file '{}'", requisitionFile);
            }

        } else {
            LOGGER.error("Parameter 'file' is missing in requisition.properties");
        }
        
        return null;
    }

    public final Path getFile() {
        return this.config.getPath("file");
    }

    @MetaInfServices
    public static class Factory implements Source.Factory {

        @Override
        public String getIdentifier() {
            return "file";
        }

        @Override
        public Source create(final InstanceConfiguration config) {
            return new FileRequisitionSource(config);
        }
    }
}
