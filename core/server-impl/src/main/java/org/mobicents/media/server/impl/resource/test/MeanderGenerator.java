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

package org.mobicents.media.server.impl.resource.test;

import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.spi.dsp.Codec;

/**
 *
 * @author kulikov
 */
public class MeanderGenerator extends AbstractSource {

    private final static Format[] FORMATS = new Format[]{Codec.LINEAR_AUDIO};
    
    private double time;
    
    private double dt;
    private int pSize;
    
    private double T;
    private short A;
    
    public MeanderGenerator(String name) {
        super(name);
        //number of seconds covered by one sample
        dt = 1/Codec.LINEAR_AUDIO.getSampleRate();
    }
    
    public void setPeriod(double T) {
        this.T = T/2;
    }
    
    public void setAmplitude(short A) {
        this.A = A;
    }
    
    public Format[] getFormats() {
        return FORMATS;
    }

    private short getValue(double t) {
        return ((long)Math.floor(t/T)) % 2 == 0 ? A : 0;
    }
    
    public void evolve(Buffer buffer, long timestamp) {
        int k = 0;
        
        //packet size in samples
        pSize = (int)((double)20/1000.0/dt);   
        byte[] data = new byte[2* pSize];
        for (int i = 0; i < pSize; i++) {
            short v = getValue(time + dt * i);
            data[k++] = (byte) v;
            data[k++] = (byte) (v >> 8);
        }
    
        buffer.setData(data);
        buffer.setOffset(0);
        buffer.setLength(data.length);
        buffer.setDuration(20);
        buffer.setFormat(Codec.LINEAR_AUDIO);
        buffer.setTimeStamp(getMediaTime());
        buffer.setDiscard(false);
        buffer.setFlags(Buffer.FLAG_LIVE_DATA);
        time += ((double)20)/1000.0;
    }
    
}
