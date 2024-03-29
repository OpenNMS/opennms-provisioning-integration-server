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

package org.opennms.opennms.pris.plugins.ocs.source;

import org.opennms.ocs.inventory.client.response.snmp.SnmpDevices;
import org.opennms.pris.api.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import org.kohsuke.MetaInfServices;
import org.opennms.pris.api.InstanceConfiguration;

public class OcsSnmpDevicesReplaySource implements Source {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcsSnmpDevicesReplaySource.class);

    private final InstanceConfiguration config;

    public OcsSnmpDevicesReplaySource(final InstanceConfiguration config) {
        this.config = config;
    }

    @Override
    public Object dump() throws Exception {
        final File replaySource = new File(this.config.getString("file"));

        final JAXBContext jaxbContext = JAXBContext.newInstance(SnmpDevices.class);
        final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        return jaxbUnmarshaller.unmarshal(replaySource);
    }

    @MetaInfServices
    public static class Factory implements Source.Factory {

        @Override
        public String getIdentifier() {
            return "ocs.devices.replay";
        }

        @Override
        public Source create(final InstanceConfiguration config) {
            return new OcsSnmpDevicesReplaySource(config);
        }
    }
}
