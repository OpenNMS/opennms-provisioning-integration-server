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
package org.opennms.opennms.pris.plugins.dns.source;

import com.google.common.base.CharMatcher;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opennms.pris.model.PrimaryType;
import org.opennms.pris.model.Requisition;
import org.opennms.pris.model.RequisitionInterface;
import org.opennms.pris.model.RequisitionNode;
import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.api.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import org.xbill.DNS.ZoneTransferException;
import org.xbill.DNS.ZoneTransferIn;

public class DNSSource implements Source {

    private static final Logger LOGGER = LoggerFactory.getLogger(DNSSource.class);

    private final InstanceConfiguration config;
    private final TSIG key = null;

    public DNSSource(final InstanceConfiguration config) {
        this.config = config;
    }

    @Override
    public Object dump() {
        final String instance = this.config.getInstanceIdentifier();

        Requisition requisition = new Requisition().withForeignSource(instance);

        ZoneTransferIn xfer = null;
        List<Record> records = null;

        try {
            xfer = ZoneTransferIn.newIXFR(new Name(this.getZone()), 0L, fallback, this.getDnsServerIp(), this.getPort(), key);
            records = xfer.run();

        // Fallbacking to AXFR
        } catch (ZoneTransferException e) {
            LOGGER.warn("IXFR not supported trying AXFR:", e);
            try {
                xfer = ZoneTransferIn.newAXFR(new Name(this.getZone()), this.getDnsServerIp(), key);
                records = xfer.run();
            } catch (TextParseException | UnknownHostException ex) {
                LOGGER.debug("some problems...", ex);
            } catch (IOException | ZoneTransferException ex) {
                LOGGER.debug("some problems...", ex);
            }
        } catch (IOException ex) {
            LOGGER.debug("some problems...", ex);
        }

        if (records != null && !records.isEmpty()) {

            for (Record rec : records) {
                if (matchingRecord(rec)) {
                    requisition.getNodes().add(createRequisitionNode(rec));
                }
            }
        }
        return requisition;
    }

    private boolean matchingRecord(Record rec) {

        LOGGER.info("matchingRecord: checking rec: " + rec + " to see if it should be imported...");

        boolean matches = false;
        if ("A".equals(Type.string(rec.getType())) || "AAAA".equals(Type.string(rec.getType()))) {
            LOGGER.debug("matchingRecord: record is an " + Type.string(rec.getType()) + " record, continuing...");

            String expression = this.getFilterExpression();

            if (expression != null) {

                Pattern p = Pattern.compile(expression);
                Matcher m = p.matcher(rec.getName().toString());

                // Try matching on host name only for backwards compatibility
                LOGGER.debug("matchingRecord: attempting to match hostname: [" + rec.getName().toString() + "] with expression: [" + expression + "]");
                if (m.matches()) {
                    matches = true;
                } else {
                    // include the IP address and try again
                    LOGGER.debug("matchingRecord: attempting to match record: [" + rec.getName().toString()
                            + " " + rec.rdataToString() + "] with expression: [" + expression + "]");
                    m = p.matcher(rec.getName().toString() + " " + rec.rdataToString());
                    if (m.matches()) {
                        matches = true;
                    }
                }

                LOGGER.debug("matchingRecord: record matches expression: " + matches);

            } else {
                LOGGER.debug("matchingRecord: no expression for this zone, returning valid match for this " + Type.string(rec.getType()) + " record...");
                matches = true;
            }

        }
        LOGGER.info("matchingRecord: record: " + rec + " matches: " + matches);
        return matches;
    }

    private RequisitionNode createRequisitionNode(Record rec) {
        String addr = null;
        if (null != Type.string(rec.getType())) {
            switch (Type.string(rec.getType())) {
                case "A":
                    ARecord arec = (ARecord) rec;
                    addr = CharMatcher.anyOf("/").trimLeadingFrom(arec.getAddress().toString());
                    break;
                case "AAAA":
                    AAAARecord aaaarec = (AAAARecord) rec;
                    addr = aaaarec.rdataToString();
                    break;
                default:
                    throw new IllegalArgumentException("Invalid record type " + Type.string(rec.getType()) + ". A or AAAA expected.");
            }
        }

        RequisitionNode node = new RequisitionNode();

        String host = rec.getName().toString();
        String nodeLabel = CharMatcher.anyOf(".").trimFrom(host);

        switch (this.getForeignIdHashMode()) {
            case "nodeLabel":
                node.setForeignId(computeHashCode(nodeLabel));
                LOGGER.debug("Generating foreignId from hash of nodelabel " + nodeLabel);
                break;
            case "address":
                node.setForeignId(computeHashCode(addr));
                LOGGER.debug("Generating foreignId from hash of ipAddress " + addr);
                break;
            case "nodeLabel+address":
                node.setForeignId(computeHashCode(nodeLabel + addr));
                LOGGER.debug("Generating foreignId from hash of nodelabel+ipAddress " + nodeLabel + addr);
                break;
            default:
                node.setForeignId(computeHashCode(nodeLabel));
                LOGGER.debug("Default case: Generating foreignId from hash of nodelabel " + nodeLabel);
                break;
        }
        node.setNodeLabel(nodeLabel);

        RequisitionInterface i = new RequisitionInterface();
        i.setDescr("DNS-" + Type.string(rec.getType()));
        i.setIpAddr(addr);
        i.setSnmpPrimary(PrimaryType.PRIMARY);
        i.setManaged(Boolean.TRUE);
        i.setStatus(1);

        node.getInterfaces().add(i);

        return node;
    }

    private final Boolean fallback = true;

    private String computeHashCode(String hashSource) {
        String hash = String.valueOf(hashSource.hashCode());
        return hash;
    }

    public String getForeignIdHashMode() {
        return this.config.getString("foreignIdHashMode", "nodeLabel");
    }

    public String getZone() {
        return this.config.getString("zone", null);
    }

    public Boolean isFallback() {
        return this.config.getBoolean("fallback", true);
    }

    public String getDnsServerIp() {
        return this.config.getString("dnsServerIp");
    }

    public int getPort() {
        return this.config.getInt("port", 53);
    }

    private String getFilterExpression() {
        return this.config.getString("filter", ".*");
    }
}
