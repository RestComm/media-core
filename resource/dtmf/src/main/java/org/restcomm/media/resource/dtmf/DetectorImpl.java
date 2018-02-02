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

package org.restcomm.media.resource.dtmf;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.ComponentType;
import org.restcomm.media.component.AbstractSink;
import org.restcomm.media.component.audio.AudioOutput;
import org.restcomm.media.component.audio.GoertzelFilter;
import org.restcomm.media.component.oob.OOBOutput;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.Task;
import org.restcomm.media.spi.dtmf.DtmfDetector;
import org.restcomm.media.spi.dtmf.DtmfDetectorListener;
import org.restcomm.media.spi.format.Format;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.format.Formats;
import org.restcomm.media.spi.listener.Listeners;
import org.restcomm.media.spi.listener.TooManyListenersException;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.pooling.PooledObject;

/**
 * Implements inband DTMF detector.
 * 
 * Inband means that DTMF is transmitted within the audio of the phone conversation, i.e. it is audible to the conversation
 * partners. Therefore only uncompressed codecs like g711 alaw or ulaw can carry inband DTMF reliably. Female voice are known to
 * once in a while trigger the recognition of a DTMF tone. For analog lines inband is the only possible means to transmit DTMF.
 * 
 * Though Inband DTMF detection may work for other codecs like SPEEX, GSM, G729 as DtmfDetector is using DSP in front of
 * InbandDetector there is no guarantee that it will always work. In future MMS may not have DSP in front of InbandDetector and
 * hence Inband detection for codecs like SPEEX, GSM, G729 may completely stop
 * 
 * @author yulian oifa
 * @author amit bhayani
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class DetectorImpl extends AbstractSink implements DtmfDetector, PooledObject {

    private static final long serialVersionUID = 450306501541827622L;

    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
    private final static Formats formats = new Formats();

    static {
        formats.add(linear);
    }

    public final static String[][] events = new String[][] { { "1", "2", "3", "A" }, { "4", "5", "6", "B" }, { "7", "8", "9", "C" }, { "*", "0", "#", "D" } };
    private final static String[] oobEvtID = new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "#", "A", "B", "C", "D" };

    private final static int[] lowFreq = new int[] { 697, 770, 852, 941 };
    private final static int[] highFreq = new int[] { 1209, 1336, 1477, 1633 };

    private final GoertzelFilter[] lowFreqFilters = new GoertzelFilter[4];
    private final GoertzelFilter[] highFreqFilters = new GoertzelFilter[4];

    private final double threshold;

    private final int level;
    private int offset;

    private final int toneDuration;
    private final int toneInterval;
    private final int N;
    private final double scale;

    private final double p[];
    private final double P[];

    private final double[] signal;
    private double maxAmpl;
    private String lastTone;
    private long elapsedTime;
    private volatile boolean waiting;

    private final DtmfBuffer dtmfBuffer;

    private final Listeners<DtmfDetectorListener> listeners;

    private final EventSender eventSender;
    private final PriorityQueueScheduler scheduler;

    private final AudioOutput output;
    private final OOBOutput oobOutput;
    private final OOBDetector oobDetector;

    private static final Logger logger = LogManager.getLogger(DetectorImpl.class);

    public DetectorImpl(String name, int toneVolume, int toneDuration, int toneInterval, PriorityQueueScheduler scheduler) {
        super(name);
        
        // Media Components
        this.scheduler = scheduler;
        
        this.output = new AudioOutput(scheduler, ComponentType.DTMF_DETECTOR.getType());
        this.oobOutput = new OOBOutput(scheduler, ComponentType.DTMF_DETECTOR.getType());
        this.output.join(this);

        this.oobDetector = new OOBDetector();
        this.oobOutput.join(oobDetector);
        
        // DTMF Components
        this.dtmfBuffer = new DtmfBuffer(this);
        this.dtmfBuffer.setInterdigitInterval(toneInterval);
        this.eventSender = new EventSender();
        this.listeners = new Listeners<DtmfDetectorListener>();
        
        // Detector Configuration
        this.level = toneVolume;
        this.threshold = Math.pow(Math.pow(10, this.level), 0.1) * Short.MAX_VALUE;
        this.toneDuration = toneDuration;
        this.toneInterval = toneInterval;
        this.scale = toneDuration / 1000.0;
        this.N = 8 * toneDuration;
        this.signal = new double[N];
        for (int i = 0; i < 4; i++) {
            this.lowFreqFilters[i] = new GoertzelFilter(lowFreq[i], N, scale);
            this.highFreqFilters[i] = new GoertzelFilter(highFreq[i], N, scale);
        }
        
        // Runtime Detection
        this.p = new double[4];
        this.P = new double[4];
        this.offset = 0;
        this.lastTone = "";
        this.elapsedTime = 0;
        this.waiting = false;
    }
    
    public DetectorImpl(String name, PriorityQueueScheduler scheduler) {
        this(name, DEFAULT_SIGNAL_LEVEL, DEFAULT_SIGNAL_DURATION, DEFAULT_INTERDIGIT_INTERVAL, scheduler);
    }

    public AudioOutput getAudioOutput() {
        return this.output;
    }

    public OOBOutput getOOBOutput() {
        return this.oobOutput;
    }

    @Override
    public void activate() {
        this.offset = 0;
        this.maxAmpl = 0;
        this.lastTone = "";
        this.elapsedTime = 0;
        this.waiting = false;

        this.dtmfBuffer.clear();
        output.start();
        oobOutput.start();
    }

    @Override
    public void deactivate() {
        output.stop();
        oobOutput.stop();
    }

    public int getDuration() {
        return this.toneDuration;
    }

    @Override
    public int getVolume() {
        return level;
    }

    @Override
    public void onMediaTransfer(Frame buffer) throws IOException {
        // If Detector is in WAITING state, then drop packets
        // until a period of data (based on frame duration accumulation) elapses.
        if (waiting) {
            this.elapsedTime += buffer.getDuration();
            this.waiting = (this.elapsedTime < this.toneInterval * 1000000);

            if (waiting) {
                return;
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace(
                            "Waiting: " + waiting + " [last tone=" + this.lastTone + ", elapsed time=" + elapsedTime + "]");
                }
            }
        }
        
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

            // if dtmf buffer full check signal
            if (offset == N) {
                offset = 0;

                // and if max amplitude of signal is greater threshold
                // try to detect tone.
                if (maxAmpl >= threshold) {
                    maxAmpl = 0;

                    getPower(lowFreqFilters, signal, 0, p);
                    getPower(highFreqFilters, signal, 0, P);

                    String tone = getTone(p, P);

                    if (tone != null) {
                        // Keep reference to latest identified tone
                        this.elapsedTime = 0;
                        this.lastTone = tone;
                        this.waiting = true;

                        if (logger.isTraceEnabled()) {
                            logger.trace("Waiting: " + waiting + " [last tone=" + this.lastTone + ", elapsed time=" + elapsedTime + "]");
                        }

                        // Push tone into DTMF buffer
                        dtmfBuffer.push(tone);
                    }
                }
            }
        }
    }

    private void getPower(GoertzelFilter[] filters, double[] data, int offset, double[] power) {
        for (int i = 0; i < 4; i++) {
            // power[i] = filter.getPower(freq[i], data, 0, data.length, (double) TONE_DURATION / (double) 1000);
            power[i] = filters[i].getPower(data, offset);
        }
    }

    /**
     * Searches maximum value in the specified array.
     * 
     * @param data[] input data.
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
     * @param f the low frequency array
     * @param F the high frequency array.
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

    @Override
    public int getInterdigitInterval() {
        return this.toneInterval;
    }

    protected void fireEvent(String tone) {
        eventSender.events.add(new DtmfEventImpl(this, tone, 0));
        // schedule event delivery
        scheduler.submit(eventSender, PriorityQueueScheduler.INPUT_QUEUE);
    }

    protected void fireEvent(DtmfEventImpl evt) {
        eventSender.events.add(evt);
        // schedule event delivery
        scheduler.submit(eventSender, PriorityQueueScheduler.INPUT_QUEUE);
    }

    protected void fireEvent(Collection<DtmfEventImpl> evts) {
        eventSender.events.addAll(evts);

        // schedule event delivery
        scheduler.submit(eventSender, PriorityQueueScheduler.INPUT_QUEUE);
    }

    @Override
    public void flushBuffer() {
        dtmfBuffer.flush();
    }

    public void clearBuffer() {
        dtmfBuffer.clear();
    }

    @Override
    public void addListener(DtmfDetectorListener listener) throws TooManyListenersException {
        listeners.add(listener);
    }

    @Override
    public void removeListener(DtmfDetectorListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void clearAllListeners() {
        listeners.clear();
    }

    @Override
    public void clearDigits() {
        dtmfBuffer.clear();
    }

    public class EventSender extends Task {

        private final Queue<DtmfEventImpl> events = new ConcurrentLinkedQueue<>();

        public EventSender() {
            super();
        }

        @Override
        public int getQueueNumber() {
            return PriorityQueueScheduler.INPUT_QUEUE;
        }

        @Override
        public long perform() {
            final Iterator<DtmfEventImpl> iterator = this.events.iterator();
            while (iterator.hasNext()) {
                DtmfEventImpl evt = iterator.next();

                // try to deliver or queue to buffer if not delivered
                if (!listeners.dispatch(evt)) {
                    dtmfBuffer.queue(evt);
                    if (logger.isInfoEnabled()) {
                        logger.info(String.format("(%s) Buffered '%s' tone", getName(), evt.getTone()));
                    }
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info(String.format("(%s) Delivered '%s' tone", getName(), evt.getTone()));
                    }
                }

                // Remove event from collection
                iterator.remove();
            }
            return 0;
        }

    }

    private class OOBDetector extends AbstractSink {
        private static final long serialVersionUID = -1065228790095547415L;

        private byte currTone = (byte) 0xFF;
        private long latestSeq = 0;

        private boolean hasEndOfEvent = false;
        private long endSeq = 0;

        public OOBDetector() {
            super("oob detector");
        }

        @Override
        public void onMediaTransfer(Frame buffer) throws IOException {
            byte[] data = buffer.getData();
            if (data.length != 4)
                return;

            boolean endOfEvent = false;
            endOfEvent = (data[1] & 0X80) != 0;

            // lets ignore end of event packets
            if (endOfEvent) {
                hasEndOfEvent = true;
                endSeq = buffer.getSequenceNumber();
                return;
            }

            // lets update sync data , allowing same tone come after 160ms from previous tone , not including end of tone
            if (currTone == data[0]) {
                if (hasEndOfEvent) {
                    if (buffer.getSequenceNumber() <= endSeq && buffer.getSequenceNumber() > (endSeq - 8)) {
                        // out of order , belongs to same event
                        // if comes after end of event then its new one
                        return;
                    }
                } else if ((buffer.getSequenceNumber() < (latestSeq + 8)) && buffer.getSequenceNumber() > (latestSeq - 8)) {
                    if (buffer.getSequenceNumber() > latestSeq)
                        latestSeq = buffer.getSequenceNumber();

                    return;
                }
            }

            hasEndOfEvent = false;
            endSeq = 0;

            latestSeq = buffer.getSequenceNumber();
            currTone = data[0];
            dtmfBuffer.push(oobEvtID[currTone]);
        }

        @Override
        public void activate() {
        }

        @Override
        public void deactivate() {

        }
    }

    @Override
    public void checkIn() {
        // TODO Auto-generated method stub

    }

    @Override
    public void checkOut() {
        // TODO Auto-generated method stub

    }
}
