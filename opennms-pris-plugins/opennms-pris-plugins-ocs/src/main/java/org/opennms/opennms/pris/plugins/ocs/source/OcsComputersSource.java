/**
 * *****************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc. OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with OpenNMS(R). If not, see: http://www.gnu.org/licenses/
 *
 * For more information contact: OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/ http://www.opennms.com/
 ******************************************************************************
 */
package org.opennms.opennms.pris.plugins.ocs.source;

import java.nio.file.Path;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.kohsuke.MetaInfServices;
import org.opennms.ocs.inventory.client.request.logic.GetComputersLogic;
import org.opennms.ocs.inventory.client.response.Computers;
import org.opennms.pris.api.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opennms.pris.api.InstanceConfiguration;

public class OcsComputersSource extends AbstractOcsSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcsComputersSource.class);

    public OcsComputersSource(final InstanceConfiguration config) {
        super(config);
    }

    @Override
    public Object dump() throws Exception {
        final GetComputersLogic ocsClient = new GetComputersLogic();

        ocsClient.init(this.getUrl(),
                       this.getUsername(),
                       this.getPassword(),
                       this.getChecksum(),
                       this.getTags());

        Computers computers = ocsClient.getComputers();

        //** dump the computers into a file as xml
        if (this.getConfig().containsKey("target")) {
            Path dumpComputers = this.getConfig().getPath("target");
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(Computers.class);
                Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                jaxbMarshaller.marshal(computers, dumpComputers.toFile());
            } catch (JAXBException ex) {
                LOGGER.error("Dumping computers to file '{}' has failed.", dumpComputers, ex);
            }
        }
        return computers;
    }

    @MetaInfServices
    public static class Factory implements Source.Factory {

        @Override
        public String getIdentifier() {
            return "ocs.computers";
        }

        @Override
        public Source create(final InstanceConfiguration config) {
            return new OcsComputersSource(config);
        }
    }
}
