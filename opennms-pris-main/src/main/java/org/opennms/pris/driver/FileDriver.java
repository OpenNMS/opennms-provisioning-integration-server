/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.pris.driver;

import org.opennms.pris.model.Requisition;
import org.opennms.pris.RequisitionGenerator;
import org.opennms.pris.Starter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import org.opennms.pris.api.Configuration;

/**
 * A driver used to create a single XML serialized requisition in a file.
 *
 * The instance name is passed as an parameter. The created requisition is
 * printed to standard output or to a given file name.
 *
 * @author Dustin Frisch &lt;fooker@lab.sh&gt;
 */
public class FileDriver implements Driver {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDriver.class);

    public static final class Factory implements Driver.Factory {

        @Override
        public Driver create(final Configuration config) {
            return new FileDriver(config);
        }
    }

    // The global configuration
    private final Configuration config;

    private FileDriver(final Configuration config) {
        this.config = config;
    }

    @Override
    public void run() throws Exception {
        // Get the instance matching glob and find all matching instances
        final String instanceGlob = this.config.getString("requisitions", "*");
        final Collection<String> instances = Starter.getConfigManager().getInstances(instanceGlob);

        // Get the target directory and ensure it exist
        final Path targetBase = Paths.get(this.config.getString("target"));
        Files.createDirectories(targetBase);

        // Loop over all instances
        for (final String instance : instances) {
            // Get the target path for this instance
            final Path target = targetBase.resolve(instance + ".xml");

            // Generate the requisition
            final RequisitionGenerator requisitionProvider = new RequisitionGenerator(instance);
            final Requisition requisition = requisitionProvider.generate(instance);

            // Create a XML serializer
            final JAXBContext jaxbContext = JAXBContext.newInstance(Requisition.class);

            final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            // Serialize the generated requisition to a the configured target
            try (final OutputStream os = Files.newOutputStream(target)) {
                jaxbMarshaller.marshal(requisition, os);
            }
        }
    }
}
