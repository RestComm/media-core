/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.rtp.format;

import org.restcomm.media.spi.format.AudioFormat;
import org.restcomm.media.spi.format.FormatFactory;

/**
 * Representation of linear format used by core media components like Media Mixers.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class LinearFormat {

    public static final AudioFormat FORMAT = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
    public static final long PERIOD = 20000000L;
    public static final int PACKET_SIZE = (int) (PERIOD / 1000000) * FORMAT.getSampleRate() / 1000 * FORMAT.getSampleSize() / 8;

    private LinearFormat() {
        super();
    }
}
