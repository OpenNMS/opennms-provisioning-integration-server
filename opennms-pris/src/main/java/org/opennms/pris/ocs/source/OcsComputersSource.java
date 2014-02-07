package org.opennms.pris.ocs.source;

import org.apache.commons.configuration.Configuration;
import org.opennms.ocs.inventory.client.request.logic.GetComputersLogic;
import org.opennms.pris.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcsComputersSource extends AbstractOcsSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(OcsComputersSource.class);

  public static class Factory implements Source.Factory {

    @Override
    public Source create(final String instance,
                         final Configuration config) {
      return new OcsComputersSource(instance, config);
    }
  }

  public OcsComputersSource(final String instance,
                            final Configuration config) {
    super(instance,
          config);
  }

  @Override
  public Object dump() throws Exception {
    final GetComputersLogic ocsClient = new GetComputersLogic();

    ocsClient.init(this.getUrl(),
                   this.getUsername(),
                   this.getPassword(),
                   this.getChecksum(),
                   this.getTags());

    return ocsClient.getComputers();

  }
}
