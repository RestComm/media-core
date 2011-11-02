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

package org.mobicents.media.server.component;

import java.util.ArrayList;

import org.mobicents.media.server.spi.dsp.Processor;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.component.audio.GoertzelFilter;

/**
 * Digital signaling processor.
 *
 * DSP transforms media from its original format to one of the specified
 * output format. Output formats are specified as array where order of the
 * formats defines format's priority. If frame has format matching to output
 * format the frame won't be changed.
 *
 * @author kulikov
 */
public class DtmfClamp {
	//active formats
	private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
    private final static Formats formats = new Formats();
    static {
        formats.add(linear);
    }
    
    /**
    * The default duration of the DTMF tone.
    */
    private final static int TONE_DURATION = 40;
    private final static int[] lowFreq = new int[]{697, 770, 852, 941};
    private final static int[] highFreq = new int[]{1209, 1336, 1477, 1633};
   
    private GoertzelFilter[] lowFreqFilters = new GoertzelFilter[4];
    private GoertzelFilter[] highFreqFilters = new GoertzelFilter[4];
   
    private double threshold;
   
    private int level;
    private int offset = 0;
   
    private int toneDuration = TONE_DURATION;
    private int N = 8 * toneDuration;
   
    private double scale = (double) toneDuration / (double) 1000;
   
    private double p[] = new double[4];
    private double P[] = new double[4];
   
    private double[] signal;
    private double maxAmpl;
    
    private Processor dsp;
    
    private ArrayList<Frame> processedData;
    private ArrayList<Frame> inProgressData;
    
    public DtmfClamp()
    {
	    signal = new double[N];
        for (int i = 0; i < 4; i++) {
            lowFreqFilters[i] = new GoertzelFilter(lowFreq[i], N, scale);
            highFreqFilters[i] = new GoertzelFilter(highFreq[i], N, scale);
        }
        this.level = DtmfDetector.DEFAULT_SIGNAL_LEVEL;
        processedData=new ArrayList<Frame>(10);
        inProgressData=new ArrayList<Frame>(10);
    }
    
    public void setDsp(Processor dsp)
    {
    	this.dsp=dsp;
    	this.dsp.setFormats(formats);
    }
    
    public void recycle()
    {
    	while(inProgressData.size()>0)
    		inProgressData.remove(0).recycle();
    	
    	while(processedData.size()>0)
    		processedData.remove(0).recycle();
    	
    	offset=0;
    }
    
    public Frame process(Frame source)
    {
    	inProgressData.add(source);
    	
    	Frame activeFrame=(Frame)source.clone();
    	//do transcoding
    	if (dsp != null) {
    		activeFrame = dsp.process(activeFrame);
    	}
    	
    	byte[] data = activeFrame.getData();

        int M = activeFrame.getLength();
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
                    
                    getPower(lowFreqFilters, signal, 0, p);
                    getPower(highFreqFilters, signal, 0, P);

                    if (hasTone(p, P))
                    	while(inProgressData.size()>0)
                    		inProgressData.remove(0).recycle();                    	
                    else
                    {
                    	while(inProgressData.size()>0)
                    		processedData.add(inProgressData.remove(0));
                    }
                }
            }
        }
        
        if(processedData.size()>0)
        	return processedData.remove(0);
        
        return null;
    }
    
    private void getPower(GoertzelFilter[] filters, double[] data, int offset, double[] power) {
        for (int i = 0; i < 4; i++) {
            //power[i] = filter.getPower(freq[i], data, 0, data.length, (double) TONE_DURATION / (double) 1000);
            power[i] = filters[i].getPower(data, offset);
        }
    }
    
    /**
     * Searches maximum value in the specified array.
     * 
     * @param data[]
     *            input data.
     * @return the index of the maximum value in the data array.
     */
    private int getMax(double data[]) {
        int idx = 0;
        double max = data[0];
        for (int i = 1; i < data.length; i++) {
            if (max < data[i]) {
                max = data[i];
                idx = i;
            }
        }
        return idx;
    }

    /**
     * Searches DTMF tone.
     * 
     * @param f
     *            the low frequency array
     * @param F
     *            the high frequency array.
     * @return DTMF tone.
     */
    private Boolean hasTone(double f[], double F[]) {
        int fm = getMax(f);
        boolean fd = true;

        for (int i = 0; i < f.length; i++) {
            if (fm == i) {
                continue;
            }
            double r = f[fm] / (f[i] + 1E-15);
            if (r < threshold) {
                fd = false;
                break;
            }
        }

        if (!fd) {
            return false;
        }

        int Fm = getMax(F);
        boolean Fd = true;

        for (int i = 0; i < F.length; i++) {
            if (Fm == i) {
                continue;
            }
            double r = F[Fm] / (F[i] + 1E-15);
            if (r < threshold) {
                Fd = false;
                break;
            }
        }

        if (!Fd) {
            return false;
        }

        return true;
    }
}
