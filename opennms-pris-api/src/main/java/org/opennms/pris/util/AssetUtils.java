/**
 * *****************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc. OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with OpenNMS(R). If not, see: http://www.gnu.org/licenses/
 *
 * For more information contact: OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/ http://www.opennms.com/ *****************************************************************************
 */
package org.opennms.pris.util;

public class AssetUtils {

    private AssetUtils() {
    }

    public static String assetStringCleaner(final String assetString,
                                            final Integer maxSize) {
        String result = "";
        if (assetString != null) {
            result = assetString;
            result = result.replace("\u00ae", "");
            result = result.replace("(R)", "");
            result = result.replace("(tm)", "");
            result = result.replace("Microsoft", "MS");
            result = result.replace("Service Pack", "SP");
            result = result.replace("CentOS release", "CentOS");
            result = result.replace("Red Hat Enterprise Linux Server release", "Red Hat Linux");
            result = result.replaceAll("\\s+", " ");
            if (result.length() > maxSize) {
                result = result.substring(0, maxSize);
            }
        }
        return result;
    }

}
