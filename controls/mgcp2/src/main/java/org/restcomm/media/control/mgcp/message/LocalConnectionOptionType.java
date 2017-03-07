/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software, you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY, without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software, if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
        
package org.restcomm.media.control.mgcp.message;

/**
 * Enumeration of Local Connection options.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public enum LocalConnectionOptionType {

    CODECS("a"),
    BANDWIDTH("b"),
    PACKETIZATION_PERIOD("p"),
    TYPE_OF_NETWORK("nt"),
    TYPE_OF_SERVICE("t"),
    ECHO_CANCELATION("e"),
    GAIN_CONTROL("gc"),
    SILENCE_SUPPRESSION("s"),
    RESOURCE_RESERVATION("r"),
    ENCRYPTION_KEY("k"),
    DTMF_CLAMP("x-dc"),    
    WEBRTC("webrtc"),
    LOCAL_NETWORK("LOCAL");

    private final String code;

    private LocalConnectionOptionType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static final LocalConnectionOptionType fromCode(String code) {
        if (code != null && !code.isEmpty()) {
            for (LocalConnectionOptionType option : values()) {
                if (option.code.equalsIgnoreCase(code)) {
                    return option;
                }
            }
        }
        throw new IllegalArgumentException("No LocalConnectionOption matching " + code);
    }

}
