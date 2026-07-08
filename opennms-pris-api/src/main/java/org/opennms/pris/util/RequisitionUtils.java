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

/**
 * Utility methods for looking up elements within a requisition or its nodes.
 */
public class RequisitionUtils {

    private RequisitionUtils() {
    }

    /**
     * Finds the node with the given foreign ID.
     *
     * @param requisition the requisition to search
     * @param foreignId the foreign ID to match
     * @return the matching node, or {@code null} if none matches
     */
    public static RequisitionNode findNode(final Requisition requisition,
            final String foreignId) {
        for (final RequisitionNode node : requisition.getNodes()) {
            if (Objects.equals(node.getForeignId(), foreignId)) {
                return node;
            }
        }

        return null;
    }

    /**
     * Finds the interface on the given node with the given IP address.
     *
     * @param node the node to search
     * @param ipAddress the IP address to match
     * @return the matching interface, or {@code null} if none matches
     */
    public static RequisitionInterface findInterface(final RequisitionNode node,
            final String ipAddress) {
        for (final RequisitionInterface _interface : node.getInterfaces()) {
            if (Objects.equals(_interface.getIpAddr(), ipAddress)) {
                return _interface;
            }
        }

        return null;
    }

    /**
     * Finds the category on the given node with the given name.
     *
     * @param node the node to search
     * @param categoryName the category name to match
     * @return the matching category, or {@code null} if none matches
     */
    public static RequisitionCategory findCategory(final RequisitionNode node,
            final String categoryName) {
        for (final RequisitionCategory category : node.getCategories()) {
            if (Objects.equals(category.getName(), categoryName)) {
                return category;
            }
        }

        return null;
    }

    /**
     * Determines whether the given node carries the given category.
     *
     * @param node the node to search
     * @param categoryName the category name to match
     * @param ignoreCase whether a case-insensitive match is sufficient; when
     *        {@code false} the names must match exactly
     * @return {@code true} if a matching category exists
     */
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

    /**
     * Finds the asset on the given node with the given name.
     *
     * @param node the node to search
     * @param assetName the asset name to match
     * @return the matching asset, or {@code null} if none matches
     */
    public static RequisitionAsset findAsset(final RequisitionNode node,
            final String assetName) {
        for (final RequisitionAsset asset : node.getAssets()) {
            if (Objects.equals(asset.getName(), assetName)) {
                return asset;
            }
        }

        return null;
    }

    /**
     * Finds the meta-data entry on the given node with the given context and key.
     *
     * @param node the node to search
     * @param context the meta-data context to match
     * @param key the meta-data key to match
     * @return the matching meta-data entry, or {@code null} if none matches
     */
    public static MetaData findMetaData(final RequisitionNode node,
                                        final String context, final String key) {
        for (final MetaData metaData : node.getMetaDatas()) {
            if (Objects.equals(metaData.getContext(), context) && Objects.equals(metaData.getKey(), key)) {
                return metaData;
            }
        }

        return null;
    }

    /**
     * Finds the monitored service on the given interface with the given name.
     *
     * @param _interface the interface to search
     * @param serviceName the service name to match
     * @return the matching monitored service, or {@code null} if none matches
     */
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
