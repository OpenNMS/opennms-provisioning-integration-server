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
package org.opennms.opennms.pris.plugins.ocs.source;

import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.api.Source;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractOcsSource implements Source {
  
  private final InstanceConfiguration config;

  public AbstractOcsSource(final InstanceConfiguration config) {
    this.config = config;
  }

  public final InstanceConfiguration getConfig() {
    return this.config;
  }
          
  public final String  getUrl() {
    return this.config.getString("url");
  }
  
  public final String  getUsername() {
    return this.config.getString("username");
  }
  
  public final String  getPassword() {
    return this.config.getString("password");
  }
  
  public final String  getChecksum() {
    return this.config.getString("checksum");
  }
  
  public final List<String>  getTags() {
    return Arrays.asList(this.config.getStringArray("tags"));
  }
}
