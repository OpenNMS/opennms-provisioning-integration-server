/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2011 The OpenNMS Group, Inc.
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
package org.opennms.opennms.pris.plugins.vmware.source;

import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.CustomFieldValue;
import com.vmware.vim25.mo.ManagedEntity;
import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.api.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>AbstractVmwareSource class.</p>
 * <p/>
 * This class has the generic configuration for both sources, the vmware.hostsystem.source and the vmware.guest.source.
 * It provides also the factory which is used in the RequisitionProvider to initialize the concrete source.
 *
 * @author <a href="mailto:ronny@opennms.org">Ronny Trommer</a>
 * @version $Id: $
 * @since 1.0-SNAPSHOT
 */
public abstract class AbstractVmwareSource implements Source {

    /**
     * Initialize logging framework
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(VmwareGuestSource.class);

    /**
     * Configuration property file with vCenter credentials
     */
    private final InstanceConfiguration config;

    /**
     * Initialize the VMware source with a given configuration
     *
     * @param config   Configuration property file as {@link org.opennms.pris.api.InstanceConfiguration}
     */
    public AbstractVmwareSource(final InstanceConfiguration config) {
        this.config = config;
    }

    /**
     * Get configuration property file
     *
     * @return configuration property file as {@link org.opennms.pris.api.InstanceConfiguration}
     */
    protected final InstanceConfiguration getConfig() {

        return this.config;
    }

    /**
     * Build vCenter SDK connection URL from configured vCenter IP
     *
     * @return URL to connect to VMware vCenter as {@link java.net.URL}
     */
    protected final URL getUrl() {
        URL result = null;
        try {
            result = new URL("https://" + this.config.getString("vmware.vcenter.ip") + "/sdk");
        } catch (MalformedURLException e) {
            LOGGER.error("Configuration parameter vmware.vcenter.ip is not valid. Error: 'e'", e);
        }
        return result;
    }

    /**
     * Get the user name for vCenter connection
     *
     * @return user name from configuration as {@link java.lang.String}
     */
    protected final String getUsername() {
        return this.config.getString("vmware.username");
    }

    /**
     * Get the password for vCenter connection
     *
     * @return password from configuration as {@link java.lang.String}
     */
    protected final String getPassword() {
        return this.config.getString("vmware.password");
    }

    /**
     * Get the vCenter IP address or FQDN from configuration property file
     *
     * @return vCenter IP address or FQDN as {@link java.lang.String}
     */
    protected final String getVcenterIp() {
        return this.config.getString("vmware.vcenter.ip");
    }

    /**
     * Helper method to initialize null strings with a default value
     *
     * @param string
     */
    protected String defaultStringWhenNull(String string, String defaultString) {
        if (string == null) {
            return defaultString;
        } else {
            return string;
        }
    }

    public final String getInstance() {
        return this.config.getInstanceIdentifier();
    }

    @Override
    public Object dump() throws Exception {
        return null;
    }

    /**
     * Gets the custom attributes.
     *
     * @param entity the entity
     * @return the custom attributes
     * @throws RemoteException the remote exception
     */
    private Map<String, String> getCustomAttributes(ManagedEntity entity) throws RemoteException {
        final Map<String, String> attributes = new TreeMap<>();

        // Get all custom attribute field definition
        CustomFieldDef[] defs = entity.getAvailableField();

        // Get all custom attribute values
        CustomFieldValue[] values = entity.getCustomValue();

        // Iterate through definition and value list and put them into the TreeMap
        for (int i = 0; defs != null && i < defs.length; i++) {
            String key = defs[i].getName();
            int targetIndex = defs[i].getKey();
            for (int j = 0; values != null && j < values.length; j++) {
                if (targetIndex == values[j].getKey()) {
                    attributes.put(key, ((CustomFieldStringValue) values[j]).getValue());
                }
            }
        }
        //return map
        return attributes;
    }
}
