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

package org.restcomm.media.control.mgcp.pkg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.restcomm.media.control.mgcp.exception.MgcpParseException;
import org.restcomm.media.control.mgcp.pkg.MgcpActionType;
import org.restcomm.media.control.mgcp.pkg.MgcpEventType;
import org.restcomm.media.control.mgcp.pkg.MgcpPackage;
import org.restcomm.media.control.mgcp.pkg.MgcpPackageManager;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEventsParser;
import org.restcomm.media.control.mgcp.pkg.exception.UnrecognizedMgcpActionException;
import org.restcomm.media.control.mgcp.pkg.exception.UnrecognizedMgcpEventException;
import org.restcomm.media.control.mgcp.pkg.exception.UnrecognizedMgcpPackageException;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpRequestedEventsParserTest {

    @Test
    public void testParseEndpointEventWithoutParameters() throws Exception {
        // given
        final int requestId = 16;
        final String requestedEvents = "AU/oc(N)";
        final MgcpEventType eventType = mock(MgcpEventType.class);
        final MgcpPackage mgcpPackage = mock(MgcpPackage.class);
        final MgcpPackageManager packageManager = mock(MgcpPackageManager.class);

        // when
        when(packageManager.getPackage("AU")).thenReturn(mgcpPackage);
        when(mgcpPackage.getEventDetails("oc")).thenReturn(eventType);
        when(eventType.parameterized()).thenReturn(false);

        MgcpRequestedEvent[] events = MgcpRequestedEventsParser.parse(requestId, requestedEvents, packageManager);

        // then
        assertEquals(1, events.length);

        final MgcpRequestedEvent oc = events[0];
        assertNotNull(oc);
        assertEquals(requestId, oc.getRequestId());
        assertEquals("AU", oc.getPackageName());
        assertEquals("oc", oc.getEventType());
        assertEquals(0, oc.getConnectionId());
        assertEquals(0, oc.getParameters().length);
        assertEquals(MgcpActionType.NOTIFY, oc.getAction());
    }

    @Test
    public void testParseEndpointEventWithParameters() throws Exception {
        // given
        final int requestId = 16;
        final String requestedEvents = "AU/oc(N)(param1=value1,param2=value2)";
        final MgcpEventType eventType = mock(MgcpEventType.class);
        final MgcpPackage mgcpPackage = mock(MgcpPackage.class);
        final MgcpPackageManager packageManager = mock(MgcpPackageManager.class);

        // when
        when(packageManager.getPackage("AU")).thenReturn(mgcpPackage);
        when(mgcpPackage.getEventDetails("oc")).thenReturn(eventType);
        when(eventType.parameterized()).thenReturn(true);

        MgcpRequestedEvent[] events = MgcpRequestedEventsParser.parse(requestId, requestedEvents, packageManager);

        // then
        assertEquals(1, events.length);

        final MgcpRequestedEvent event = events[0];
        assertNotNull(event);
        assertEquals(requestId, event.getRequestId());
        assertEquals("AU", event.getPackageName());
        assertEquals("oc", event.getEventType());
        assertEquals(0, event.getConnectionId());
        assertEquals(2, event.getParameters().length);
        assertEquals(MgcpActionType.NOTIFY, event.getAction());
    }

    @Test
    public void testParseConnectionEventWithoutParameters() throws Exception {
        // given
        final int requestId = 16;
        final String requestedEvents = "R/rto@364823(N)";
        final MgcpEventType eventType = mock(MgcpEventType.class);
        final MgcpPackage mgcpPackage = mock(MgcpPackage.class);
        final MgcpPackageManager packageManager = mock(MgcpPackageManager.class);

        // when
        when(packageManager.getPackage("R")).thenReturn(mgcpPackage);
        when(mgcpPackage.getEventDetails("rto")).thenReturn(eventType);
        when(eventType.parameterized()).thenReturn(false);

        MgcpRequestedEvent[] events = MgcpRequestedEventsParser.parse(requestId, requestedEvents, packageManager);

        // then
        assertEquals(1, events.length);

        final MgcpRequestedEvent event = events[0];
        assertNotNull(event);
        assertEquals(requestId, event.getRequestId());
        assertEquals("R", event.getPackageName());
        assertEquals("rto", event.getEventType());
        assertEquals(Integer.parseInt("364823", 16), event.getConnectionId());
        assertEquals(MgcpActionType.NOTIFY, event.getAction());
    }

    @Test
    public void testParseMulitpleEvents() throws Exception {
        // given
        final int requestId = 16;
        final String requestedEvents = "AU/oc(N),R/rto@AB23F(N)(100,st=im),AU/of(N)";
        final MgcpEventType audioEventType = mock(MgcpEventType.class);
        final MgcpPackage audioPackage = mock(MgcpPackage.class);
        final MgcpPackage rtpPackage = mock(MgcpPackage.class);
        final MgcpEventType rtpEventType = mock(MgcpEventType.class);
        final MgcpPackageManager packageManager = mock(MgcpPackageManager.class);

        // when
        when(packageManager.getPackage("AU")).thenReturn(audioPackage);
        when(audioPackage.getEventDetails(any(String.class))).thenReturn(audioEventType);
        when(audioEventType.parameterized()).thenReturn(false);

        when(packageManager.getPackage("R")).thenReturn(rtpPackage);
        when(rtpPackage.getEventDetails("rto")).thenReturn(rtpEventType);
        when(rtpEventType.parameterized()).thenReturn(true);

        MgcpRequestedEvent[] events = MgcpRequestedEventsParser.parse(requestId, requestedEvents, packageManager);

        // then
        assertEquals(3, events.length);

        final MgcpRequestedEvent oc = events[0];
        assertNotNull(oc);
        assertEquals(requestId, oc.getRequestId());
        assertEquals("AU", oc.getPackageName());
        assertEquals("oc", oc.getEventType());
        assertEquals(0, oc.getConnectionId());
        assertEquals(0, oc.getParameters().length);
        assertEquals(MgcpActionType.NOTIFY, oc.getAction());

        final MgcpRequestedEvent rto = events[1];
        assertNotNull(rto);
        assertEquals(requestId, rto.getRequestId());
        assertEquals("R", rto.getPackageName());
        assertEquals("rto", rto.getEventType());
        assertEquals(Integer.parseInt("AB23F", 16), rto.getConnectionId());
        assertEquals(2, rto.getParameters().length);
        assertEquals(MgcpActionType.NOTIFY, rto.getAction());

        final MgcpRequestedEvent of = events[2];
        assertNotNull(of);
        assertEquals(requestId, of.getRequestId());
        assertEquals("AU", of.getPackageName());
        assertEquals("of", of.getEventType());
        assertEquals(0, of.getConnectionId());
        assertEquals(0, of.getParameters().length);
        assertEquals(MgcpActionType.NOTIFY, of.getAction());
    }

    @Test(expected = UnrecognizedMgcpPackageException.class)
    public void testParseUnrecognizedPackage() throws Exception {
        // given
        final int requestId = 16;
        final String requestedEvents = "XYZ/oc(N),AU/of(N)";
        final MgcpPackageManager packageManager = mock(MgcpPackageManager.class);

        // when
        when(packageManager.getPackage("XYZ")).thenReturn(null);
        MgcpRequestedEventsParser.parse(requestId, requestedEvents, packageManager);
    }

    @Test(expected = UnrecognizedMgcpEventException.class)
    public void testParseUnrecognizedEvent() throws Exception {
        // given
        final int requestId = 16;
        final String requestedEvents = "AU/xyz(N),AU/of(N)";
        final MgcpPackage mgcpPackage = mock(MgcpPackage.class);
        final MgcpPackageManager packageManager = mock(MgcpPackageManager.class);

        // when
        when(packageManager.getPackage("AU")).thenReturn(mgcpPackage);
        when(mgcpPackage.getEventDetails("xyz")).thenReturn(null);

        MgcpRequestedEventsParser.parse(requestId, requestedEvents, packageManager);
    }

    @Test(expected = UnrecognizedMgcpActionException.class)
    public void testParseUnrecognizedAction() throws Exception {
        // given
        final int requestId = 16;
        final String requestedEvents = "AU/oc(XYZ),AU/of(N)";
        final MgcpPackage mgcpPackage = mock(MgcpPackage.class);
        final MgcpEventType eventType = mock(MgcpEventType.class);
        final MgcpPackageManager packageManager = mock(MgcpPackageManager.class);

        // when
        when(packageManager.getPackage("AU")).thenReturn(mgcpPackage);
        when(mgcpPackage.getEventDetails(any(String.class))).thenReturn(eventType);
        when(eventType.parameterized()).thenReturn(false);

        MgcpRequestedEventsParser.parse(requestId, requestedEvents, packageManager);
    }

    @Test(expected = MgcpParseException.class)
    public void testParseMalformedRequest() throws Exception {
        // given
        final int requestId = 16;
        final String requestedEvents = "AU/oc(XYZ";
        final MgcpPackage mgcpPackage = mock(MgcpPackage.class);
        final MgcpEventType eventType = mock(MgcpEventType.class);
        final MgcpPackageManager packageManager = mock(MgcpPackageManager.class);

        // when
        when(packageManager.getPackage("AU")).thenReturn(mgcpPackage);
        when(mgcpPackage.getEventDetails(any(String.class))).thenReturn(eventType);
        when(eventType.parameterized()).thenReturn(false);

        MgcpRequestedEventsParser.parse(requestId, requestedEvents, packageManager);
    }

}
