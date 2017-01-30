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

/**
 * List of Events supported by RTP Package.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public enum RtpEventType {

    ICMP_UNREACHABLE("iu", true),
    JITTER_BUFFER_SIZE_CHANGED("ji", true),
    MEDIA_START("ma"),
    OPERATION_COMPLETE("oc"),
    OPERATION_FAILED("of"),
    PACKET_LOSS_EXCEEDED("pl", true),
    QUALITY_ALERT("qa"),
    RTP_TIMEOUT("rto", true),
    SAMPLING_RATE_CHANGED("sr"),
    USED_CODEC_CHANGED("uc");
    
    private final String symbol;
    private final boolean parameterized;

    private RtpEventType(String symbol, boolean parameterized) {
        this.symbol = symbol;
        this.parameterized = parameterized;
    }

    private RtpEventType(String symbol) {
        this(symbol, false);
    }

    public String symbol() {
        return symbol;
    }

    public boolean parameterized() {
        return this.parameterized;
    }

    public static final RtpEventType fromSymbol(String symbol) {
        if (symbol != null && !symbol.isEmpty()) {
            for (RtpEventType eventType : values()) {
                if (eventType.symbol.equalsIgnoreCase(symbol)) {
                    return eventType;
                }
            }
        }
        return null;
    }
    
}
