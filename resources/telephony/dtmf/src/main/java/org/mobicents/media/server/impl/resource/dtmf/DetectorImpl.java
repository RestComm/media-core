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

package org.mobicents.media.server.impl.resource.dtmf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.component.audio.GoertzelFilter;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.listener.Listeners;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.spi.memory.Frame;

/**
 * Implements inband DTMF detector.
 * 
 * Inband means that DTMF is transmitted within the audio of the phone
 * conversation, i.e. it is audible to the conversation partners. Therefore only
 * uncompressed codecs like g711 alaw or ulaw can carry inband DTMF reliably.
 * Female voice are known to once in a while trigger the recognition of a DTMF
 * tone. For analog lines inband is the only possible means to transmit DTMF.
 * 
 * Though Inband DTMF detection may work for other codecs like SPEEX, GSM, G729
 * as DtmfDetector is using DSP in front of InbandDetector there is no guarantee
 * that it will always work. In future MMS may not have DSP in front of
 * InbandDetector and hence Inband detection for codecs like SPEEX, GSM, G729
 * may completely stop
 * 
 * @author Oleg Kulikov
 * @author amit bhayani
 */
public class DetectorImpl extends AbstractSink implements DtmfDetector {
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
    private final static Formats formats = new Formats();
    static {
        formats.add(linear);
    }
    
    /**
     * The default duration of the DTMF tone.
     */
    private final static int TONE_DURATION = 50;
    public final static String[][] events = new String[][]{
        {"1", "2", "3", "A"},
        {"4", "5", "6", "B"},
        {"7", "8", "9", "C"},
        {"*", "0", "#", "D"}
    };
    private final static String[] evtID = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "#", "*"};
    
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
    
    private DtmfBuffer dtmfBuffer;
    
    
    private Listeners<DtmfDetectorListener> listeners = new Listeners();

    private EventSender eventSender;
    private Scheduler scheduler;
        
    private Logger logger = Logger.getLogger(DetectorImpl.class) ;
    /**
     * Creates new instance of Detector.
     */
    public DetectorImpl(String name, Scheduler scheduler) {
        super(name, scheduler,scheduler.MIXER_INPUT_QUEUE);
        
        this.scheduler = scheduler;
        
        dtmfBuffer = new DtmfBuffer(this);
        eventSender = new EventSender(scheduler);
        
        signal = new double[N];
        for (int i = 0; i < 4; i++) {
            lowFreqFilters[i] = new GoertzelFilter(lowFreq[i], N, scale);
            highFreqFilters[i] = new GoertzelFilter(highFreq[i], N, scale);
        }
        this.level = DEFAULT_SIGNAL_LEVEL;
    }

    @Override
    public void start() {
        this.offset = 0;
        this.maxAmpl = 0;
        
        this.dtmfBuffer.clear();        
        super.start();
    }
    public void setDuration(int duartion) {
        this.toneDuration = duartion;        
    }

    public int getDuration() {
        return this.toneDuration;
    }

    public void setVolume(int level) {
        this.level = level;
        threshold = Math.pow(Math.pow(10, level), 0.1) * Short.MAX_VALUE;
    }

    public void setLasy(boolean isLazy) {
    }

    public int getVolume() {
        return level;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.protocol.BufferTransferHandler.transferData().
     */
    public void onMediaTransfer(Frame buffer) throws IOException {
        byte[] data = buffer.getData();

        int M = buffer.getLength();
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

                    String tone = getTone(p, P);
                    
                    if (tone != null)
                    	dtmfBuffer.push(tone);                    	
                }
            }
        }
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
    private String getTone(double f[], double F[]) {
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
            return null;
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
            return null;
        }

        return events[fm][Fm];
    }

    public Formats getNativeFormats() {
        return formats;
    }

    /* (non-Javadoc)
     * @see org.mobicents.media.server.impl.AbstractSink#getInterface(java.lang.Class)
     */
    @Override
    public <T> T getInterface(Class<T> interfaceType) {
        if (interfaceType.equals(DtmfDetector.class)) {
            return (T) this;
        } else {
            return null;
        }
    }

    public String getMask() {
        return null;
    }

    public void setMask(String mask) {
    }

    public void setInterdigitInterval(int interval) {
        dtmfBuffer.setInterdigitInterval(interval);
    }

    public int getInterdigitInterval() {
        return dtmfBuffer.getInterdigitInterval();
    }
    
    protected void fireEvent(String tone) {
        eventSender.events.add(new DtmfEventImpl(this, tone, 0));        
        //schedule event delivery
        scheduler.submit(eventSender,scheduler.MIXER_OUTPUT_QUEUE);
    }

    protected void fireEvent(Collection<DtmfEventImpl> evts) {
        eventSender.events.addAll(evts);                
        
        //schedule event delivery
        scheduler.submit(eventSender,scheduler.MIXER_OUTPUT_QUEUE);
    }
    
    public void flushBuffer() {
        dtmfBuffer.flush();
    }
    
    public void clearBuffer() {
        dtmfBuffer.clear();
    }
    
    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.Component#addListener(NotificationListener). 
     */
    public void addListener(DtmfDetectorListener listener) throws TooManyListenersException {
        listeners.add(listener);
    }

    public void removeListener(DtmfDetectorListener listener) {
        listeners.remove(listener);
    }

    public void clearDigits() {
        dtmfBuffer.clear();
    }
    
    public class EventSender extends Task {

        protected ArrayList<DtmfEventImpl> events = new ArrayList();
        
        public EventSender(Scheduler scheduler) {
            super(scheduler);
        }
        
        public int getQueueNumber()
        {
        	return scheduler.MIXER_OUTPUT_QUEUE;
        }       

        @Override
        public long perform() {
            for (DtmfEventImpl evt : events) {
                //try to deliver or queue to buffer if not delivered
                if (!listeners.dispatch(evt)) {
                    dtmfBuffer.queue(evt);
                    logger.info(String.format("(%s) Buffered '%s' tone", getName(), evt.getTone()));
                } else {
                    logger.info(String.format("(%s) Delivered '%s' tone", getName(), evt.getTone()));
                }
            }
            events.clear();
            return 0;
        }
        
    }
    
}
