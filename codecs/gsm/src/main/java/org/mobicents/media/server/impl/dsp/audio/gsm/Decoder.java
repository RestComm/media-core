/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.mobicents.media.server.impl.dsp.audio.gsm;

import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;
import org.tritonus.lowlevel.gsm.InvalidGSMFrameException;

/**
 * 
 * @author amit bhayani
 * @kulikov
 */
public class Decoder implements Codec {

    private final static Format gsm = FormatFactory.createAudioFormat("gsm", 8000);
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

    private org.tritonus.lowlevel.gsm.GSMDecoder decoder = new org.tritonus.lowlevel.gsm.GSMDecoder();
    private static final int BUFFER_SIZE = 320;

    public Decoder() {
    }

    public Frame process(Frame frame) {
        Frame res = Memory.allocate(320);
        process(frame.getData(), res.getData());
        res.setOffset(0);
        res.setLength(320);
        res.setTimestamp(frame.getTimestamp());
        res.setDuration(frame.getDuration());
        res.setSequenceNumber(frame.getSequenceNumber());
        res.setEOM(frame.isEOM());
        res.setFormat(linear);
        return frame;
    }
    
    /**
     * Perform compression.
     * 
     * @param input
     *            media
     * @return compressed media.
     */
    public void process(byte[] media, byte[] res) {
        try {
            decoder.decode(media, 0, res, 0, false);
        } catch (InvalidGSMFrameException e) {
        }
    }

    public Format getSupportedInputFormat() {
        return gsm;
    }

    public Format getSupportedOutputFormat() {
        return linear;
    }

}
