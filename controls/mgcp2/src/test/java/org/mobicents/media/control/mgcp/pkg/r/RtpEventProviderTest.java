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

package org.mobicents.media.control.mgcp.pkg.r;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mobicents.media.control.mgcp.exception.MalformedMgcpEventRequestException;
import org.mobicents.media.control.mgcp.exception.MgcpEventNotFoundException;
import org.mobicents.media.control.mgcp.exception.MgcpException;
import org.mobicents.media.control.mgcp.exception.MgcpPackageNotFoundException;
import org.mobicents.media.control.mgcp.pkg.MgcpActionType;
import org.mobicents.media.control.mgcp.pkg.MgcpEvent;
import org.mobicents.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.mobicents.media.control.mgcp.pkg.au.AudioPackage;
import org.mobicents.media.control.mgcp.pkg.r.rto.RtpTimeoutEvent;
import org.mobicents.media.control.mgcp.pkg.r.rto.RtpTimeoutStartTime;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpEventProviderTest {

    @Test
    public void testProvideRtpTimeoutEvent() throws MgcpException {
        // given
        final String packageName = RtpPackage.PACKAGE_NAME;
        final String eventType = RtpTimeoutEvent.SYMBOL;
        final int connectionId = 1;
        final String[] parameters = new String[] { "10", "st=ra" };
        final MgcpRequestedEvent requestedEvent = new MgcpRequestedEvent(packageName, eventType, MgcpActionType.NOTIFY, connectionId, parameters);
        final RtpPackage rtpPackage = new RtpPackage();
        final RtpEventProvider eventProvider = new RtpEventProvider(rtpPackage);

        // when
        MgcpEvent event = eventProvider.provide(requestedEvent);

        // then
        assertNotNull(event);
        assertTrue(event instanceof RtpTimeoutEvent);

        RtpTimeoutEvent rtpTimeoutEvent = (RtpTimeoutEvent) event;
        assertEquals(10, rtpTimeoutEvent.getTimeout());
        assertEquals(RtpTimeoutStartTime.WAIT_RTCP, rtpTimeoutEvent.getWhen());
    }

    @Test(expected = MgcpPackageNotFoundException.class)
    public void testInvalidPackageName() throws MgcpException {
        // given
        final String packageName = AudioPackage.PACKAGE_NAME;
        final String eventType = RtpTimeoutEvent.SYMBOL;
        final int connectionId = 1;
        final String[] parameters = new String[] { "10", "st=ra" };
        final MgcpRequestedEvent requestedEvent = new MgcpRequestedEvent(packageName, eventType, MgcpActionType.NOTIFY, connectionId, parameters);
        final RtpPackage rtpPackage = new RtpPackage();
        final RtpEventProvider eventProvider = new RtpEventProvider(rtpPackage);

        // when
        eventProvider.provide(requestedEvent);
    }

    @Test(expected = MgcpEventNotFoundException.class)
    public void testInvalidEventType() throws MgcpException {
        // given
        final String packageName = RtpPackage.PACKAGE_NAME;
        final String eventType = "xxx";
        final int connectionId = 1;
        final String[] parameters = new String[] { "10", "st=ra" };
        final MgcpRequestedEvent requestedEvent = new MgcpRequestedEvent(packageName, eventType, MgcpActionType.NOTIFY, connectionId, parameters);
        final RtpPackage rtpPackage = new RtpPackage();
        final RtpEventProvider eventProvider = new RtpEventProvider(rtpPackage);

        // when
        eventProvider.provide(requestedEvent);
    }

    @Test(expected = MalformedMgcpEventRequestException.class)
    public void testInvalidEventParameters() throws MgcpException {
        // given
        final String packageName = RtpPackage.PACKAGE_NAME;
        final String eventType = RtpTimeoutEvent.SYMBOL;
        final int connectionId = 1;
        final String[] parameters = new String[] { "-5", "st=ra" };
        final MgcpRequestedEvent requestedEvent = new MgcpRequestedEvent(packageName, eventType, MgcpActionType.NOTIFY, connectionId, parameters);
        final RtpPackage rtpPackage = new RtpPackage();
        final RtpEventProvider eventProvider = new RtpEventProvider(rtpPackage);

        // when
        eventProvider.provide(requestedEvent);
    }

}
