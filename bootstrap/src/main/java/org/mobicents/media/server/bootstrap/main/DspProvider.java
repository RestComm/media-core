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

package org.mobicents.media.server.bootstrap.main;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mobicents.media.core.configuration.CodecType;
import org.mobicents.media.core.configuration.MediaServerConfiguration;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.spi.dsp.DspFactory;

/**
 * Providers DSP Factory
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class DspProvider {

    public static DspFactory build(MediaServerConfiguration config) {
        // Get codecs list
        List<String> codecs = new ArrayList<>(config.getMediaConfiguration().countCodecs());
        Iterator<String> codecNames = config.getMediaConfiguration().getCodecs();

        // Get encoder/decoder classes for each codec
        while (codecNames.hasNext()) {
            CodecType codec = CodecType.fromName(codecNames.next());
            codecs.add(codec.getDecoder());
            codecs.add(codec.getEncoder());
        }

        // Build DSP factory
        DspFactoryImpl obj = new DspFactoryImpl();
        obj.setCodecs(codecs);
        return obj;
    }

}
