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
 * For more information contact: OpenNMS(R) Licensing &lt;license@opennms.org&gt;
 * http://www.opennms.org/ http://www.opennms.com/
 ******************************************************************************
 */
package org.opennms.pris;

import org.opennms.pris.api.Source;
import org.opennms.pris.api.Mapper;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.opennms.pris.model.Requisition;
import org.opennms.pris.api.InstanceConfiguration;
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
 * @author Dustin Frisch &lt;fooker@lab.sh&gt;
 */
public class RequisitionGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequisitionGenerator.class);

    /**
     * All known source implementations.
     */
    private static final Map<String, Source.Factory> SOURCES = new HashMap<>();
    
    /**
     * All known mapper implementations.
     */
    private static final Map<String, Mapper.Factory> MAPPERS = new HashMap<>();
    
    static {
        LOGGER.debug("Loading plugins...");
        
        for (final Source.Factory factory : ServiceLoader.load(Source.Factory.class)) {
            if (SOURCES.containsKey(factory.getIdentifier())) {
                LOGGER.warn("Duplicated source: '{}'", factory.getIdentifier());
            }
            
            LOGGER.debug("Register source: '{}'", factory.getIdentifier());
            
            SOURCES.put(factory.getIdentifier(),
                        factory);
        }
        
        for (final Mapper.Factory factory : ServiceLoader.load(Mapper.Factory.class)) {
            if (MAPPERS.containsKey(factory.getIdentifier())) {
                LOGGER.warn("Duplicated mapper: '{}'", factory.getIdentifier());
            }
            
            LOGGER.debug("Register mapper: '{}'", factory.getIdentifier());
            
            MAPPERS.put(factory.getIdentifier(),
                        factory);
        }
    }
    
    /**
     * The global configuration.
     */
    private final InstanceConfiguration config;

    /**
     * The source to use.
     */
    private final Source source;

    /**
     * The mapper to use.
     */
    private final Mapper mapper;

    /**
     * Creates a new requisition provider.
     *
     * @param instance the name of the instance
     */
    public RequisitionGenerator(final String instance) {
        // Get the configuration for the instance
//        this.config = Starter.getConfigManager().getInstanceConfig(instance);
        this.config = Starter.getConfigManager().getInstanceConfigWithGlobals(instance);

        // Create the source
        final String sourceName = this.config.getString("source");
        final Source.Factory sourceFactory = SOURCES.get(sourceName);
        if (sourceFactory != null) {
            this.source = sourceFactory.create(this.config.subset("source"));

        } else {
            //TODO Objects.requireNonNull(T) instead this if you could use something like Objects.
            throw new IllegalArgumentException("Unknown source implementation: " + sourceName);
        }

        // Create the mapper used to map the data to a requisition. If no mapper is
        // specified, a default mapper for the configured source is used
        final String mapperName = this.config.getString("mapper", "default" + "." + this.config.getString("source"));
        final Mapper.Factory mapperFactory = MAPPERS.get(mapperName);
        if (mapperFactory != null) {
            this.mapper = mapperFactory.create(this.config.subset("mapper"));

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
        
        // Create the requisition to fill
        Requisition requisition = new Requisition();
        requisition.setForeignSource(instance);

        // Map the data to a requisition with the configured mapper
        requisition = this.mapper.map(data, requisition);
        
        // If the script mapper is available, an additional run of the script
        // mapper is supported after the main mapper has finished
        // TODO: Obosolete with new config language
        if (MAPPERS.containsKey("script")) {
            final InstanceConfiguration config = this.config.subset("script");
            if (!config.isEmpty()) {
                final Mapper mapper = MAPPERS.get("script").create(config);
                requisition = mapper.map(data, requisition);
            }
        }

        return requisition;
    }
}
