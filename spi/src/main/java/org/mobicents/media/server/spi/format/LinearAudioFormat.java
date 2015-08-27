/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.server.spi.format;


/**
 * Linear format for audio mixing.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class LinearAudioFormat extends LinearFormat {

    public LinearAudioFormat() {
        super(FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1), 20000000L, computePacketSize(20000000L, 8000, 16));
    }

    private static int computePacketSize(long period, int sampleRate, int sampleSize) {
        return (int) (period / 1000000) * sampleRate / 1000 * sampleSize / 8;
    }

}
