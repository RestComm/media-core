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

import org.mobicents.media.control.mgcp.exception.AbstractSubMgcpEventProvider;
import org.mobicents.media.control.mgcp.exception.MalformedMgcpEventRequestException;
import org.mobicents.media.control.mgcp.exception.MgcpParseException;
import org.mobicents.media.control.mgcp.pkg.MgcpEvent;
import org.mobicents.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.mobicents.media.control.mgcp.pkg.r.rto.RtpTimeoutEventParser;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpEventProvider extends AbstractSubMgcpEventProvider {

    public RtpEventProvider(RtpPackage pkg) {
        super(pkg);
    }

    @Override
    protected MgcpEvent parse(MgcpRequestedEvent requestedEvent) throws MalformedMgcpEventRequestException {
        RtpEventType eventType = RtpEventType.fromSymbol(requestedEvent.getEventType());
        if (eventType != null) {
            switch (eventType) {
                case RTP_TIMEOUT:
                    try {
                        return RtpTimeoutEventParser.parse(requestedEvent.getConnectionId(), requestedEvent.getParameters());
                    } catch (MgcpParseException e) {
                        throw new MalformedMgcpEventRequestException("Could not parse " + requestedEvent.toString() + "event request.", e);
                    }

                default:
                    throw new MalformedMgcpEventRequestException("Unsupported event type " + requestedEvent.toString());
            }
        }
        throw new MalformedMgcpEventRequestException("Unrecognizable event type " + requestedEvent.toString());
    }

}