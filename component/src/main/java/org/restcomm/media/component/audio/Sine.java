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

package org.restcomm.media.component.audio;

import org.restcomm.media.ComponentType;
import org.restcomm.media.component.AbstractSource;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.spi.format.AudioFormat;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.format.Formats;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

/**
 *
 * @author yulian oifa
 */
public class Sine extends AbstractSource {

	private static final long serialVersionUID = -886146896423710570L;

	//the format of the output stream.
    private final static AudioFormat LINEAR_AUDIO = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
    private final static Formats formats = new Formats();

    private volatile long period = 20000000L;
    private int packetSize = (int)(period / 1000000) * LINEAR_AUDIO.getSampleRate()/1000 * LINEAR_AUDIO.getSampleSize() / 8;

    private int f;
    private short A = Short.MAX_VALUE;
    private double dt;
    private double time;

    private AudioInput input;
    
    static {
        formats.add(LINEAR_AUDIO);
    }
    
    public Sine(PriorityQueueScheduler scheduler) {
        super("sine.generator", scheduler, PriorityQueueScheduler.INPUT_QUEUE);
        //number of seconds covered by one sample
        dt = 1. / LINEAR_AUDIO.getSampleRate();
        
        this.input=new AudioInput(ComponentType.SINE.getType(),packetSize);
        this.connect(this.input); 
    }

    public AudioInput getAudioInput()
    {
    	return this.input;
    }
    
    public void setAmplitude(short A) {
        this.A = A;
    }

    public short getAmplitude() {
        return A;
    }

    public void setFrequency(int f) {
        this.f = f;
    }

    public int getFrequency() {
        return f;
    }

    private short getValue(double t) {
        return (short) (A * Math.sin(2 * Math.PI * f * t));
    }

    @Override
    public Frame evolve(long timestamp) {
        Frame frame = Memory.allocate(packetSize);
        int k = 0;

        int frameSize = packetSize / 2;

        byte[] data = frame.getData();
        for (int i = 0; i < frameSize; i++) {
            short v = getValue(time + dt * i);
            data[k++] = (byte) v;
            data[k++] = (byte) (v >> 8);
        }

        frame.setOffset(0);
        frame.setLength(packetSize);
        frame.setDuration(period);
        frame.setFormat(LINEAR_AUDIO);
        
        time += ((double) period) / 1000000000.0;
        return frame;
    }
}