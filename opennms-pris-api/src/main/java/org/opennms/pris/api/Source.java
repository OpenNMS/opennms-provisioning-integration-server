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

package org.opennms.pris.api;

/**
 * A source for information used to create a requisition.
 *
 * @author Dustin Frisch &lt;fooker@lab.sh&gt;
 */
public interface Source {

    /**
     * Factory interface for creating {@link Source}s.
     */
    public static interface Factory {
        
        /**
         * Returns the identifier used to created a source using this factory.
         * 
         * @return the identifier string
         */
        String getIdentifier();

        /**
         * Creates a source.
         * 
         * @param config the configuration used to build the source
         * 
         * @return the created source
         */
        Source create(final InstanceConfiguration config);
    }

    /**
     * Loads some information and returns them.
     *
     * There is no requirement to the returned object as long as there is a
     * mapper available for mapping this data.
     *
     * @return the loaded information
     *
     * @throws Exception
     */
    Object dump() throws Exception;
}
