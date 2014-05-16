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
package org.opennms.pris;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.pris.jdbc.source.JdbcSource;
import org.opennms.pris.mapper.EchoMapper;
import org.opennms.pris.mapper.Mapper;
import org.opennms.pris.mapper.NullMapper;
import org.opennms.pris.mapper.ScriptMapper;
import org.opennms.pris.ocs.mapper.DefaultOcsComputerMapper;
import org.opennms.pris.ocs.mapper.DefaultOcsSnmpDevicesMapper;
import org.opennms.pris.ocs.source.OcsComputersReplaySource;
import org.opennms.pris.ocs.source.OcsComputersSource;
import org.opennms.pris.ocs.source.OcsSnmpDevicesReplaySource;
import org.opennms.pris.ocs.source.OcsSnmpDevicesSource;
import org.opennms.pris.source.FileRequisitionSource;
import org.opennms.pris.source.HttpRequisitionMergeSource;
import org.opennms.pris.source.HttpRequisitionSource;
import org.opennms.pris.source.Source;
import org.opennms.pris.vmware.mapper.DefaultVmwareMapper;
import org.opennms.pris.vmware.source.VmwareSource;
import org.opennms.pris.xls.source.XlsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a requisition.
 *
 * The requisition provider generates a requisition for a configured instance.
 *
 * The requisition generation is a two step process. The fist step is to load
 * the information required to generate the requisition from a source. The
 * second step is to map this data to a OpenNMS requisition.
 *
 * The source and mapping implementation to use is loaded from the configuration
 * of the instance for which the requisition is created.
 *
 * @author Dustin Frisch <fooker@lab.sh>
 */
public class RequisitionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequisitionProvider.class);

    // All known source implementations
    private static final Map<String, Source.Factory> SOURCES = ImmutableMap.<String, Source.Factory>builder()
            .put("requisition.source", new HttpRequisitionSource.Factory())
            .put("vmware.source", new VmwareSource.Factory())
            .put("xls.source", new XlsSource.Factory())
            .put("jdbc.source", new JdbcSource.Factory())
            .put("requisitionMerge.source", new HttpRequisitionMergeSource.Factory())
            .put("file.source", new FileRequisitionSource.Factory())
            .put("ocs.computers", new OcsComputersSource.Factory())
            .put("ocs.snmpDevices", new OcsSnmpDevicesSource.Factory())
            .put("ocs.computers.replay", new OcsComputersReplaySource.Factory())
            .put("ocs.snmpDevices.replay", new OcsSnmpDevicesReplaySource.Factory())
            .build();

    // All known mapper implementations
    private static final Map<String, Mapper.Factory> MAPPERS = ImmutableMap.<String, Mapper.Factory>builder()
            .put("null.mapper", new NullMapper.Factory())
            .put("echo.mapper", new EchoMapper.Factory())
            .put("default.vmware.mapper", new DefaultVmwareMapper.Factory())
            .put("default.ocs.computers", new DefaultOcsComputerMapper.Factory())
            .put("default.ocs.snmpDevices", new DefaultOcsSnmpDevicesMapper.Factory())
            .put("default.ocs.computers.replay", new DefaultOcsComputerMapper.Factory())
            .put("default.ocs.snmpDevices.replay", new DefaultOcsSnmpDevicesMapper.Factory())
            .build();

    // The global configuration
    private final Configuration config;

    // The source to use
    private final Source source;

    // The mapper to use
    private final Mapper mapper;

    /**
     * Creates a new requisition provider.
     *
     * @param instance the name of the instance
     *
     * @throws ConfigurationException
     */
    public RequisitionProvider(final String instance) throws ConfigurationException {
        // Get the configuration for the instance
        this.config = Starter.getConfigManager().getInstanceConfig(instance);

        // Create the source
        final String sourceName = this.config.getString("source");
        final Source.Factory sourceFactory = SOURCES.get(sourceName);
        if (sourceFactory != null) {
            this.source = sourceFactory.create(instance, this.config);

        } else {
            //TODO Objects.requireNonNull(T) instead this if you could use something like Objects.
            throw new IllegalArgumentException("Unknown source implementation: " + sourceName);
        }

    // Create the mapper used to map the data to a requisition. If no mapper is
        // specified, a default mapper for the configured source is used
        final String mapperName = this.config.getString("mapper", "default" + "." + this.config.getString("source"));
        final Mapper.Factory mapperFactory = MAPPERS.get(mapperName);
        if (mapperFactory != null) {
            this.mapper = mapperFactory.create(instance, this.config);

        } else {
            //TODO Objects.requireNonNull(T) instead this if you could use something like Objects.
            throw new IllegalArgumentException("Unknown mapper implementation: " + mapperName);
        }
    }

    /**
     * Generates a requisition.
     *
     * @param instance of the requisition
     * @return the generated requisition
     *
     * @throws Exception
     */
    public Requisition generate(String instance) throws Exception {

        // Get the data from the source
        final Object data = this.source.dump();
        Requisition requisition = new Requisition(instance);

        // Map the data to a requisition with the configured mapper
        requisition = this.mapper.map(data, requisition);

        if (this.config.containsKey("script")) {
            // Run the script mapper against the data and requisition
            requisition = new ScriptMapper(instance, config).map(data, requisition);
        }

        return requisition;
    }
}
