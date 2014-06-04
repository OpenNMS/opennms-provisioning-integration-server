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
 * *****************************************************************************
 */
package org.opennms.pris.api;

import java.nio.file.Path;

/**
 * Container for an instance configuration.
 * 
 * @author Dustin Frisch <fooker@lab.sh>
 */
public interface InstanceConfiguration extends Configuration {
   
    /**
     * Returns the identifier of the instance.
     * 
     * @return the identifier string
     */
    String getInstanceIdentifier();
    
    /**
     * Return a decorator Configuration containing every key from the current
     * Configuration that starts with the specified prefix.
     * 
     * The prefix is removed from the keys in the subset.
     * 
     * Since the subset is a decorator and not a modified copy of the initial
     * Configuration, any change made to the subset is available to the
     * Configuration, and reciprocally.
     *
     * @param prefix The prefix used to select the properties
     * 
     * @return a subset configuration
     */
    InstanceConfiguration subset(final String prefix);
}
