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

package org.restcomm.media.codec.opus;

import org.restcomm.media.spi.dsp.Codec;
import org.restcomm.media.spi.format.Format;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

/**
 * Implements Opus decoder.
 * 
 * @author Vladimir Morosev (vladimir.morosev@telestax.com)
 * 
 */
public class Decoder implements Codec {

    private final static Format opus = FormatFactory.createAudioFormat("opus", 48000, 8, 1);
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

    private int j=0,i=0;
    private int sourceLen=0,destinationLen=0;

    /**
     * (Non Java-doc)
     * 
     * @see org.mobicents.media.server.impl.jmf.dsp.Codec#getSupportedFormat().
     */
    public Format getSupportedInputFormat() {
        return opus;
    }

    /**
     * (Non Java-doc)
     * 
     * @see org.mobicents.media.server.impl.jmf.dsp.Codec#getSupportedFormat().
     */
    public Format getSupportedOutputFormat() {
        return linear;
    }

    /**
     * (Non Java-doc)
     * 
     * @see org.mobicents.media.server.dsp.Codec#process(Frame).
     */
    public Frame process(Frame frame) {
    	sourceLen=frame.getLength();
    	destinationLen=sourceLen * 2;
        Frame res = Memory.allocate(destinationLen);
        
        byte[] data=frame.getData();
        byte[] resData=res.getData();
        
        for (i = 0,j = 0; i < sourceLen; i++) 
        {
        }
        
        res.setOffset(0);
        res.setLength(destinationLen);
        res.setTimestamp(frame.getTimestamp());
        res.setDuration(frame.getDuration());
        res.setSequenceNumber(frame.getSequenceNumber());
        res.setEOM(frame.isEOM());
        res.setFormat(linear);
        res.setHeader(frame.getHeader());
        return res;
    }
}