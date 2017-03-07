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
        
package org.restcomm.media.core.configuration;

/**
 * Enumerates supported codecs as well as their assigned Encoders and Decoders.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public enum CodecType {
    
    PCMU(0, "pcmu", org.restcomm.media.codec.g711.ulaw.Encoder.class.getName(), org.restcomm.media.codec.g711.ulaw.Decoder.class.getName()), 
    PCMA(8, "pcma", org.restcomm.media.codec.g711.alaw.Encoder.class.getName(), org.restcomm.media.codec.g711.alaw.Decoder.class.getName()), 
    GSM(3, "gsm", org.restcomm.media.codec.gsm.Encoder.class.getName(), org.restcomm.media.codec.gsm.Decoder.class.getName()), 
    L16(97, "l16", org.restcomm.media.codec.l16.Encoder.class.getName(), org.restcomm.media.codec.l16.Decoder.class.getName()),
    G729(18, "g729", org.restcomm.media.codec.g729.Encoder.class.getName(), org.restcomm.media.codec.g729.Decoder.class.getName()),
    ILBC(102, "ilbc", org.restcomm.media.codec.ilbc.Encoder.class.getName(), org.restcomm.media.codec.ilbc.Decoder.class.getName()),
    DTMF(101, "telephone-event", "", "");

    private final int payloadType;
    private final String name;
    private final String encoder;
    private final String decoder;

    private CodecType(int payloadType, String name, String encoder, String decoder) {
        this.payloadType = payloadType;
        this.name = name;
        this.encoder = encoder;
        this.decoder = decoder;
    }
    
    public int getPayloadType() {
        return payloadType;
    }

    public String getName() {
        return name;
    }

    public String getEncoder() {
        return encoder;
    }

    public String getDecoder() {
        return decoder;
    }

    public static final CodecType fromName(String name) {
        if (name != null && !name.isEmpty()) {
            for (CodecType codec : values()) {
                if (codec.name.equalsIgnoreCase(name)) {
                    return codec;
                }
            }
        }
        return null;
    }

}
