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

import java.io.IOException;
import java.util.ArrayList;

import org.mobicents.media.MediaSource;

import org.mobicents.media.ComponentType;
import org.mobicents.media.server.component.audio.AudioOutput;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;

import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.FormatNotSupportedException;

import org.mobicents.media.server.spi.listener.Event;
import org.mobicents.media.server.spi.listener.Listeners;
import org.mobicents.media.server.spi.listener.TooManyListenersException;

import org.mobicents.media.server.spi.tone.ToneEvent;
import org.mobicents.media.server.spi.tone.ToneDetector;
import org.mobicents.media.server.spi.tone.ToneDetectorListener;

import org.mobicents.media.server.component.audio.GoertzelFilter;
import org.mobicents.media.server.impl.AbstractSink;

/**
 *
 * @author Oifa Yulian
 */
public class PhoneSignalDetector extends AbstractSink implements ToneDetector {

    private double POWER = 100000;
    private final static int PACKET_DURATION = 50;
    private AudioFormat LINEAR_AUDIO = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);    
    private int[] f;
    private int offset;
    private int toneDuration = PACKET_DURATION;
    private int N = 8 * toneDuration;
    private double scale = (double) toneDuration / (double) 1000;
    private GoertzelFilter[] freqFilters;
    private double[] signal;
    private double maxAmpl;
    private double threshold;
    private int level;
    private double p[];
    private long startTime;
    private int count;    

    private AudioOutput output;
        
    private Listeners<ToneDetectorListener> listeners = new Listeners<ToneDetectorListener>();    
    
    public PhoneSignalDetector(String name,PriorityQueueScheduler scheduler) {
        super(name);
        signal = new double[N];  

        output=new AudioOutput(scheduler,ComponentType.SIGNAL_DETECTOR.getType());
        output.join(this);    
    }

    public AudioOutput getAudioOutput()
    {
    	return this.output;
    }
    
    public void setFrequency(int[] f) {
        this.f = f;
        freqFilters = new GoertzelFilter[f.length];
        p = new double[f.length];

        for (int i = 0; i < f.length; i++) {
            freqFilters[i] = new GoertzelFilter(f[i], N, scale);
        }
    }

    public int[] getFrequency() {
        return f;
    }

    public void setVolume(int level) {
        this.level = level;
        threshold = Math.pow(Math.pow(10, level), 0.1) * Short.MAX_VALUE;
    }

    public int getVolume() {
        return level;
    }

    public void activate() {
    	output.start();
    }
    
    public void deactivate() {
    	output.stop();
    }
    
    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
    	byte[] data = frame.getData();

        int M = data.length;
        int k = 0;
        while (k < M) {
            while (offset < N && k < M - 1) {
                double s = ((data[k++] & 0xff) | (data[k++] << 8));
                double sa = Math.abs(s);
                if (sa > maxAmpl) {
                    maxAmpl = sa;
                }
                signal[offset++] = s;
            }

            //if dtmf buffer full check signal
            if (offset == N) {
                offset = 0;
                //and if max amplitude of signal is greater theshold
                //try to detect tone.
                if (maxAmpl >= threshold) {                	
                    maxAmpl = 0;
                    getPower(freqFilters, signal, 0, p);
                    int detectedValue=isDetected();
                    if (detectedValue>=0)
                    	sendEvent(new ToneEventImpl(this,getFrequency()[detectedValue]));
                }                
            }
        }
    }    

    private int isDetected() {
    	for (int i = 0; i < p.length; i++) {
            if (p[i] >= POWER) {
                return i;
            }
        }
        return -1;
    }

    private void getPower(GoertzelFilter[] filters, double[] data, int offset, double[] power) {
        for (int i = 0; i < filters.length; i++) {
            power[i] = filters[i].getPower(data, offset);            
        }
    }

    /**
     * (Non Java-doc.)
     *
     *
     * @see org.mobicents.media.MediaSink#setFormats(org.mobicents.media.server.spi.format.Formats)
     */
    public void setFormats(Formats formats) throws FormatNotSupportedException {
    		
    }
    
    public void addListener(ToneDetectorListener listener) throws TooManyListenersException
    {
    	listeners.add(listener);
    }
    
    public void removeListener(ToneDetectorListener listener)
    {    	
    	listeners.remove(listener);
    }
    
    public void clearAllListeners() {
    	listeners.clear();
    }
    
    private void sendEvent(Event event)
    {
    	listeners.dispatch(event);    	
    }    
}
