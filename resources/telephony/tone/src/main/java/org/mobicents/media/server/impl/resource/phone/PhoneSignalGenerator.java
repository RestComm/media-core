/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media.server.impl.resource.phone;

import org.mobicents.media.ComponentType;
import org.mobicents.media.server.component.audio.CompoundInput;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.impl.AbstractSource;

import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;

/**
 * Generates sine wave signal with specified Amplitude and frequence.
 *
 * The format of output signal is Linear, 16bit, 8kHz.
 * 
 * @author Oifa Yulian
 */
public class PhoneSignalGenerator extends AbstractSource  {
	private AudioFormat LINEAR_AUDIO = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);    
	int frameSize = (int)(20.0f*LINEAR_AUDIO.getSampleRate()/1000.0);
	
    private int[] f;
    private short A = Short.MAX_VALUE;
    
    private double dt;
    private int pSize;
    
    private double time;
    private double elapsed;
    private double duration;
    private double value = 1;
    private int seqNumber=1;
    
    private int[] T=new int[] {1,1};
    
    private CompoundInput input;
    
    public PhoneSignalGenerator(String name,Scheduler scheduler) {
        super(name,scheduler,scheduler.INPUT_QUEUE);
        init();
        
        this.input=new CompoundInput(ComponentType.SIGNAL_GENERATOR.getType(),frameSize);
        this.connect(this.input);   
    }
    
    public CompoundInput getCompoundInput()
    {
    	return this.input;
    }
    
    private void init() {
        //number of seconds covered by one sample
        dt = 1.0f/(float)LINEAR_AUDIO.getSampleRate();
    }
    
    public void setAmplitude(short A) {
        this.A = A;
    }
    
    public short getAmplitude() {
        return A;
    }
    
    public void setFrequency(int[] f) {
        this.f = f;
    }
    
    public int[] getFrequency() {
        return f;
    }
    
    public void setPeriods(int[] T) {
        this.T = T;
        duration = T[0];
    }
    
    public int[] getPeriods() {
        return T;
    }
    
    private short getValue(double t) {
        elapsed += dt;
        if (elapsed > duration) {
            if (value == 0) {
                value = 1;
                duration = T[0];
            } else {
                value = 0;
                duration = T[1];
            }
            elapsed = 0;
        }
        if (value == 0) {
            return 0;
        }
        
        double v = 0;
        for (int i = 0; i < f.length; i++) {
            v += Math.sin(2.0f * Math.PI * ((double)f[i]) * t);
        }
        return (short)(v * A);
    }

    public Frame evolve(long timestamp) {
    		Frame currFrame=Memory.allocate(frameSize*2);
            byte[] data = currFrame.getData();
        
            int k = 0;            
            
            //packet size in samples
            pSize = (int)((double)20/1000.0/dt);
            for (int i = 0; i < frameSize; i++) {
                short v = getValue(time + dt * i);
                data[k++] = (byte) v;
                data[k++] = (byte) (v >> 8);
            }
                        
            
            //put packet into buffer irrespective of its sequence number
    		currFrame.setHeader(null);
    		currFrame.setSequenceNumber(seqNumber++);
    		//here time is in milliseconds
    		currFrame.setTimestamp(System.currentTimeMillis());
    		currFrame.setOffset(0);
    		currFrame.setLength(data.length);
    		currFrame.setDuration(20000000L);
    		
    		//set format
    		currFrame.setFormat(this.LINEAR_AUDIO);    		                    
    		time += ((double)20)/1000.0;
    		
    		return currFrame;
    }    
}
