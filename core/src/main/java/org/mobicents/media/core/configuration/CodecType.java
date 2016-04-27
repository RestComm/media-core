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
    
    L16("l16", "org.mobicents.media.server.impl.dsp.audio.l16.Encoder", "org.mobicents.media.server.impl.dsp.audio.l16.Decoder"),
    PCMU("pcmu", "org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Encoder", "org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Decoder"), 
    PCMA("pcma", "org.mobicents.media.server.impl.dsp.audio.g711.alaw.Encoder", "org.mobicents.media.server.impl.dsp.audio.g711.alaw.Decoder"), 
    GSM("gsm", "org.mobicents.media.server.impl.dsp.audio.gsm.Encoder", "org.mobicents.media.server.impl.dsp.audio.gsm.Decoder"), 
    G729("g729", "org.mobicents.media.server.impl.dsp.audio.g729.Encoder", "org.mobicents.media.server.impl.dsp.audio.g729.Decoder");

    private final String name;
    private final String encoder;
    private final String decoder;

    private CodecType(String name, String encoder, String decoder) {
        this.name = name;
        this.encoder = encoder;
        this.decoder = decoder;
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
