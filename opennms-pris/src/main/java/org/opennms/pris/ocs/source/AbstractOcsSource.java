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
import org.opennms.pris.source.Source;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractOcsSource implements Source {
  
  private final String instance;
  private final Configuration config;

  public AbstractOcsSource(final String instance,
                           final Configuration config) {
    this.instance = instance;
    this.config = config;
  }

  public final Configuration getConfig() {
    return this.config;
  }
          
  public final String  getUrl() {
    return this.config.getString("ocs.url");
  };
  
  public final String  getUsername() {
    return this.config.getString("ocs.username");
  };
  
  public final String  getPassword() {
    return this.config.getString("ocs.password");
  };
  
  public final String  getChecksum() {
    return this.config.getString("ocs.checksum");
  };
  
  public final List<String>  getTags() {
    return Arrays.asList(this.config.getStringArray("ocs.tags"));
  };
}
