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
        
package org.mobicents.media.core.configuration;

/**
 * Enumerates supported codecs as well as their assigned Encoders and Decoders.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public enum CodecType {
    
    PCMU(0, "pcmu", "org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Encoder", "org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Decoder"), 
    PCMA(8, "pcma", "org.mobicents.media.server.impl.dsp.audio.g711.alaw.Encoder", "org.mobicents.media.server.impl.dsp.audio.g711.alaw.Decoder"), 
    GSM(3, "gsm", "org.mobicents.media.server.impl.dsp.audio.gsm.Encoder", "org.mobicents.media.server.impl.dsp.audio.gsm.Decoder"), 
    L16(97, "l16", "org.mobicents.media.server.impl.dsp.audio.l16.Encoder", "org.mobicents.media.server.impl.dsp.audio.l16.Decoder"),
    G729(18, "g729", "org.mobicents.media.server.impl.dsp.audio.g729.Encoder", "org.mobicents.media.server.impl.dsp.audio.g729.Decoder"),
    ILBC(102, "ilbc", "org.mobicents.media.server.impl.dsp.audio.ilbc.Encoder", "org.mobicents.media.server.impl.dsp.audio.ilbc.Decoder"),
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
