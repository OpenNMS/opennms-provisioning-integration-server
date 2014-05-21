/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R). If not, see:
 * http://www.gnu.org/licenses/
 *
 * For more information contact:
 * OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/
 * http://www.opennms.com/
 *******************************************************************************/
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
    public Source create(final String instance, final Configuration config) {
      return new OcsComputersSource(instance, config);
    }
  }

  public OcsComputersSource(final String instance, final Configuration config) {
    super(instance, config);
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
