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
package org.opennms.pris.mapper;

import org.apache.commons.configuration.Configuration;
import org.opennms.netmgt.provision.persist.requisition.Requisition;

/**
 * A mapper to map information to a requisition.
 *
 * The data loaded by a source implementation are passed the the map function
 * and a new requisition must be returned.
 *
 * @author Dustin Frisch <fooker@lab.sh>
 */
public interface Mapper {

    interface Factory {

        Mapper create(final String instance,
                final Configuration config);
    }

    /**
     * Maps information to a requisition.
     *
     * The passed data is the data returned by the source implementation.
     *
     * @param data the information used to create the requisition.
     *
     * @param requisition Requisition already filled by a mapper, or an empty
     * requistion.
     *
     * @return the mapped requisition
     *
     * @throws Exception
     */
    Requisition map(final Object data, Requisition requisition) throws Exception;
}
