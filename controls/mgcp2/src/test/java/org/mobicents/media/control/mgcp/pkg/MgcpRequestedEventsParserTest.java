/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media.control.mgcp.pkg;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mobicents.media.control.mgcp.exception.MgcpParseException;
import org.mobicents.media.control.mgcp.pkg.exception.UnrecognizedMgcpActionException;
import org.mobicents.media.control.mgcp.pkg.exception.UnrecognizedMgcpEventException;
import org.mobicents.media.control.mgcp.pkg.exception.UnrecognizedMgcpPackageException;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpRequestedEventsParserTest {

    @Test
    public void testParseSingleEvent() throws UnrecognizedMgcpPackageException, UnrecognizedMgcpEventException,
    UnrecognizedMgcpActionException, MgcpParseException {
        // given
        final String requestedEvents = "AU/oc(N)";
        
        // when
        MgcpRequestedEvent[] events = MgcpRequestedEventsParser.parse(requestedEvents);
        
        // then
        assertEquals(1, events.length);
        
        final MgcpRequestedEvent oc = events[0];
        assertNotNull(oc);
        assertEquals("AU", oc.getPackageName());
        assertEquals("oc", oc.getEventType());
        assertEquals(MgcpActionType.NOTIFY, oc.getAction());
    }

    @Test
    public void testParseMulitpleEvents() throws UnrecognizedMgcpPackageException, UnrecognizedMgcpEventException,
            UnrecognizedMgcpActionException, MgcpParseException {
        // given
        final String requestedEvents = "AU/oc(N),AU/of(N)";

        // when
        MgcpRequestedEvent[] events = MgcpRequestedEventsParser.parse(requestedEvents);

        // then
        assertEquals(2, events.length);

        final MgcpRequestedEvent oc = events[0];
        assertNotNull(oc);
        assertEquals("AU", oc.getPackageName());
        assertEquals("oc", oc.getEventType());
        assertEquals(MgcpActionType.NOTIFY, oc.getAction());

        final MgcpRequestedEvent of = events[1];
        assertNotNull(of);
        assertEquals("AU", of.getPackageName());
        assertEquals("of", of.getEventType());
        assertEquals(MgcpActionType.NOTIFY, of.getAction());
    }

    @Test(expected = UnrecognizedMgcpPackageException.class)
    public void testParseUnrecognizedPackage() throws UnrecognizedMgcpPackageException, UnrecognizedMgcpEventException,
            UnrecognizedMgcpActionException, MgcpParseException {
        // given
        final String requestedEvents = "XYZ/oc(N),AU/of(N)";

        // when
        MgcpRequestedEventsParser.parse(requestedEvents);
    }

    @Test(expected = UnrecognizedMgcpEventException.class)
    public void testParseUnrecognizedEvent() throws UnrecognizedMgcpPackageException, UnrecognizedMgcpEventException,
            UnrecognizedMgcpActionException, MgcpParseException {
        // given
        final String requestedEvents = "AU/xyz(N),AU/of(N)";

        // when
        MgcpRequestedEventsParser.parse(requestedEvents);
    }

    @Test(expected = UnrecognizedMgcpActionException.class)
    public void testParseUnrecognizedAction() throws UnrecognizedMgcpPackageException, UnrecognizedMgcpEventException,
            UnrecognizedMgcpActionException, MgcpParseException {
        // given
        final String requestedEvents = "AU/oc(XYZ),AU/of(N)";

        // when
        MgcpRequestedEventsParser.parse(requestedEvents);
    }

    @Test(expected = MgcpParseException.class)
    public void testParseMalformedRequest() throws UnrecognizedMgcpPackageException, UnrecognizedMgcpEventException,
    UnrecognizedMgcpActionException, MgcpParseException {
        // given
        final String requestedEvents = "AU/oc(XYZ";
        
        // when
        MgcpRequestedEventsParser.parse(requestedEvents);
    }

}
