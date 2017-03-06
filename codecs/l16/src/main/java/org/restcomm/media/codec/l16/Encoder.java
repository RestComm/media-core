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

package org.restcomm.media.codec.l16;

import org.restcomm.media.spi.dsp.Codec;
import org.restcomm.media.spi.format.Format;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

/**
 * 
 * @author oifa yulian
 * 
 */
public class Encoder implements Codec {

    private final static Format l16 = FormatFactory.createAudioFormat("l16", 8000, 16, 1);
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

    public Frame process(Frame frame) {
	Frame res = Memory.allocate(320);
        System.arraycopy( frame.getData(), 0, res.getData(), 0, 320 );

        res.setOffset(0);
        res.setLength(320);
        res.setTimestamp(frame.getTimestamp());
        res.setDuration(frame.getDuration());
        res.setSequenceNumber(frame.getSequenceNumber());
        res.setEOM(frame.isEOM());
        res.setFormat(l16);
        return res;
    }

    public Format getSupportedInputFormat() {
        return linear;
    }

    public Format getSupportedOutputFormat() {
        return l16;
    }    
}
