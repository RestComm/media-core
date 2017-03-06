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

package org.restcomm.media.control.mgcp.pkg.r.rto;

import java.util.regex.Pattern;

import org.restcomm.media.control.mgcp.exception.MgcpParseException;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpTimeoutEventParser {

    private static final String TIMEOUT_REGEX = "\\d+";
    private static final Pattern TIMEOUT_PATTERN = Pattern.compile(TIMEOUT_REGEX);

    private static final String START_TIME_REGEX = RtpTimeoutEvent.START_TIME_KEY + "=\\w{2}";
    private static final Pattern START_TIME_PATTERN = Pattern.compile(START_TIME_REGEX);

    public static RtpTimeoutEvent parse(int connectionId, String... parameters) throws MgcpParseException {
        int timeout = 0;
        RtpTimeoutStartTime when = RtpTimeoutStartTime.IMMEDIATE;
        try {
            for (String parameter : parameters) {
                if (TIMEOUT_PATTERN.matcher(parameter).matches()) {
                    timeout = Integer.parseInt(parameter);
                } else if (START_TIME_PATTERN.matcher(parameter).matches()) {
                    int indexOfEqual = parameter.indexOf("=");
                    when = RtpTimeoutStartTime.fromSymbol(parameter.substring(indexOfEqual + 1));

                    if (when == null) {
                        throw new IllegalArgumentException("Unknown R/rto Start Time parameter: " + parameter);
                    }
                }
            }
            if (timeout <= 0) {
                throw new IllegalArgumentException("R/rto timeout parameter must be a positive value.");
            }
        } catch (Exception e) {
            throw new MgcpParseException("Could not parse parameters RTP Timeout event.", e);
        }
        return new RtpTimeoutEvent(connectionId, timeout, when);
    }

}
