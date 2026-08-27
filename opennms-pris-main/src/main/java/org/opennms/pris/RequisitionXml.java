/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2026 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2026 The OpenNMS Group, Inc.
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

package org.opennms.pris;

import java.io.OutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.opennms.pris.model.Requisition;

/**
 * The one place requisitions are serialized to XML, shared by every endpoint
 * that serves them so their output cannot drift apart.
 *
 * The {@link JAXBContext} is thread-safe and expensive to build, so it is
 * created once; only the (cheap, non-thread-safe) marshaller is per call.
 */
public final class RequisitionXml {

    private static final JAXBContext CONTEXT;

    static {
        try {
            CONTEXT = JAXBContext.newInstance(Requisition.class);
        } catch (final JAXBException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private RequisitionXml() {
    }

    /**
     * Marshals a requisition as formatted XML to the given stream.
     *
     * @param requisition the requisition to serialize
     * @param out the stream to write to
     *
     * @throws JAXBException if marshalling fails
     */
    public static void marshal(final Requisition requisition, final OutputStream out) throws JAXBException {
        final Marshaller marshaller = CONTEXT.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(requisition, out);
    }
}
