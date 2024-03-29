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

package org.opennms.pris.util;

import java.util.Objects;

import org.opennms.pris.model.MetaData;
import org.opennms.pris.model.Requisition;
import org.opennms.pris.model.RequisitionAsset;
import org.opennms.pris.model.RequisitionCategory;
import org.opennms.pris.model.RequisitionInterface;
import org.opennms.pris.model.RequisitionMonitoredService;
import org.opennms.pris.model.RequisitionNode;

public class RequisitionUtils {

    private RequisitionUtils() {
    }

    public static RequisitionNode findNode(final Requisition requisition,
            final String foreignId) {
        for (final RequisitionNode node : requisition.getNodes()) {
            if (Objects.equals(node.getForeignId(), foreignId)) {
                return node;
            }
        }

        return null;
    }

    public static RequisitionInterface findInterface(final RequisitionNode node,
            final String ipAddress) {
        for (final RequisitionInterface _interface : node.getInterfaces()) {
            if (Objects.equals(_interface.getIpAddr(), ipAddress)) {
                return _interface;
            }
        }

        return null;
    }

    public static RequisitionCategory findCategory(final RequisitionNode node,
            final String categoryName) {
        for (final RequisitionCategory category : node.getCategories()) {
            if (Objects.equals(category.getName(), categoryName)) {
                return category;
            }
        }

        return null;
    }

    public static Boolean hasCategory(final RequisitionNode node, final String categoryName, final Boolean ignoreCase) {
        for (final RequisitionCategory category : node.getCategories()) {
            if (Objects.equals(category.getName().toLowerCase(), categoryName.toLowerCase())) {
                if (ignoreCase) {
                    return true;
                } else {
                    if (Objects.equals(category.getName(), categoryName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static RequisitionAsset findAsset(final RequisitionNode node,
            final String assetName) {
        for (final RequisitionAsset asset : node.getAssets()) {
            if (Objects.equals(asset.getName(), assetName)) {
                return asset;
            }
        }

        return null;
    }

    public static MetaData findMetaData(final RequisitionNode node,
                                        final String context, final String key) {
        for (final MetaData metaData : node.getMetaDatas()) {
            if (Objects.equals(metaData.getContext(), context) && Objects.equals(metaData.getKey(), key)) {
                return metaData;
            }
        }

        return null;
    }

    public static RequisitionMonitoredService findMonitoredService(final RequisitionInterface _interface,
            final String serviceName) {
        for (final RequisitionMonitoredService monitoredService : _interface.getMonitoredServices()) {
            if (Objects.equals(monitoredService.getServiceName(), serviceName)) {
                return monitoredService;
            }
        }

        return null;
    }
}
