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

import org.mobicents.media.control.mgcp.pkg.MgcpEventType;
import org.mobicents.media.control.mgcp.pkg.MgcpPackage;

/**
 * The events in this package all refer to media streams (connections), i.e., they cannot be detected on an endpoint.
 * <p>
 * Furthermore, with the exception of the "iu" event, which is defined for any type of media, all other events in this package
 * are defined for RTP media streams only (i.e., if they are used on connections that do not use RTP, the behavior is not
 * defined).
 * </p>
 * <p>
 * Full package specification: <a href="https://tools.ietf.org/html/rfc3660#section-2.10">RFC 3660</a>
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpPackage implements MgcpPackage {

    public static final String PACKAGE_NAME = "R";

    @Override
    public String getPackageName() {
        return PACKAGE_NAME;
    }

    @Override
    public boolean isEventSupported(String event) {
        if (event == null || event.isEmpty()) {
            return false;
        }
        return (getEventDetails(event) != null);
    }

    @Override
    public MgcpEventType getEventDetails(String event) {
        return RtpEventType.fromSymbol(event);
    }

}
