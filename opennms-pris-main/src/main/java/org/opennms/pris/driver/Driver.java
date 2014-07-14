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
 ******************************************************************************
 */
package org.opennms.pris.driver;

import org.opennms.pris.api.Configuration;

/**
 * A driver to handle a working mode. A working mode describes how the
 * requisition is provided to the OpenNMS system.
 *
 * The driver runs a working mode.
 *
 * @author Dustin Frisch <fooker@lab.sh>
 */
public interface Driver {

    interface Factory {

        Driver create(final Configuration config);
    }

    /**
     * Runs the working mode implementation.
     *
     * @throws Exception
     */
    void run() throws Exception;
}
