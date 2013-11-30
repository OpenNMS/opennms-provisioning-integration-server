package org.opennms.provisioner.ocs;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.provisioner.ocs.mapper.DefaultOcsComputerMapper;
import org.opennms.provisioner.ocs.mapper.DefaultOcsSnmpDevicesMapper;
import org.opennms.provisioner.ocs.mapper.Mapper;
import org.opennms.provisioner.ocs.mapper.ScriptMapper;
import org.opennms.provisioner.ocs.source.OcsComputersReplaySource;
import org.opennms.provisioner.ocs.source.OcsComputersSource;
import org.opennms.provisioner.ocs.source.OcsSnmpDevicesReplaySource;
import org.opennms.provisioner.ocs.source.OcsSnmpDevicesSource;
import org.opennms.provisioner.ocs.source.Source;
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
          .put("ocs.computers", new OcsComputersSource.Factory())
          .put("ocs.snmpDevices", new OcsSnmpDevicesSource.Factory())
          .put("ocs.computers.replay", new OcsComputersReplaySource.Factory())
          .put("ocs.snmpDevices.replay", new OcsSnmpDevicesReplaySource.Factory())
          .build();

  // All known mapper implementations
  private static final Map<String, Mapper.Factory> MAPPERS = ImmutableMap.<String, Mapper.Factory>builder()
          .put("default.ocs.computers", new DefaultOcsComputerMapper.Factory())
          .put("default.ocs.snmpDevices", new DefaultOcsSnmpDevicesMapper.Factory())
          .put("default.ocs.computers.replay", new DefaultOcsComputerMapper.Factory())
          .put("default.ocs.snmpDevices.replay", new DefaultOcsSnmpDevicesMapper.Factory())
          .put("script", new ScriptMapper.Factory())
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
    this.config = Starter.getConfigManager().getInstances(instance);

    // Create the source
    final String sourceName = this.config.getString("source");
    final Source.Factory sourceFactory = SOURCES.get(sourceName);
    if (sourceFactory != null) {
      this.source = sourceFactory.create(instance,
                                         this.config);

    } else {
      throw new IllegalArgumentException("Unknown source implementation: " + sourceName);
    }

    //TODO the script mapper should run in addition after a mapper with the prepared requisition. 
    //If no mapper is selected a empty requisition should be send to the script (null mapper).
    
    // Create the mapper used to map the data to a requisition. If no mapper is
    // specified, a default mapper for the configured source is used
    final String mapperName = this.config.getString("mapper", "default" + "." + this.config.getString("source"));
    final Mapper.Factory mapperFactory = MAPPERS.get(mapperName);
    if (mapperFactory != null) {
      this.mapper = mapperFactory.create(instance,
                                         this.config);
      
    } else {
      throw new IllegalArgumentException("Unknown mapper imaplementation: " + mapperName);
    }
  }

  /**
   * Generates a requisition.
   * 
   * @return the generated requisition
   * 
   * @throws Exception 
   */
  public Requisition generate() throws Exception {
    // Get the data from the source
    final Object data = this.source.dump();

    // Map the data to a requisition
    final Requisition requisition = this.mapper.map(data);

    // Set the foreign source on the requisition
    if (this.config.containsKey("foreignSource")) {
      requisition.setForeignSource(this.config.getString("foreignSource"));
    }

    return requisition;
  }
}
