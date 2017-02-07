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

package org.mobicents.media.control.mgcp.pkg.r.rto;

import org.mobicents.media.control.mgcp.pkg.GenericMgcpEvent;
import org.mobicents.media.control.mgcp.pkg.r.RtpPackage;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpTimeoutEvent extends GenericMgcpEvent {

    public static final String SYMBOL = "rto";
    public static final String TIMEOUT_KEY = "to";
    public static final String START_TIME_KEY = "st";

    public RtpTimeoutEvent(int timeout) {
        this(timeout, RtpTimeoutStartTime.IMMEDIATE);
    }

    public RtpTimeoutEvent(int timeout, RtpTimeoutStartTime startTime) {
        super(RtpPackage.PACKAGE_NAME, SYMBOL);

        this.setParameter(TIMEOUT_KEY, String.valueOf(timeout));
        this.setParameter(START_TIME_KEY, startTime.symbol());
    }

    public int getTimeout() {
        String timeout = getParameter(TIMEOUT_KEY);
        if (timeout == null || timeout.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(timeout);
    }

    public RtpTimeoutStartTime getWhen() {
        String when = getParameter(START_TIME_KEY);
        if (when == null || when.isEmpty()) {
            return RtpTimeoutStartTime.IMMEDIATE;
        }
        return RtpTimeoutStartTime.fromSymbol(when);
    }

    @Override
    public String toString() {
        this.builder.setLength(0);
        this.builder.append(this.pkg).append("/").append(this.symbol);
        this.builder.append("(").append(getTimeout()).append(")");
        return builder.toString();
    }

}
