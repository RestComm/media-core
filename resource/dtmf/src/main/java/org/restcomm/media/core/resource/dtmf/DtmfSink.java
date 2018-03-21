/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2018, Telestax Inc and individual contributors
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

package org.restcomm.media.core.resource.dtmf;

import java.io.IOException;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.core.component.AbstractSink;
import org.restcomm.media.core.component.audio.AudioOutput;
import org.restcomm.media.core.component.oob.OOBOutput;
import org.restcomm.media.core.scheduler.PriorityQueueScheduler;
import org.restcomm.media.core.scheduler.Task;
import org.restcomm.media.core.spi.ComponentType;
import org.restcomm.media.core.spi.memory.Frame;
import org.restcomm.media.core.spi.listener.Listeners;
import org.restcomm.media.core.spi.listener.TooManyListenersException;

/**
 * DTMF sink with in-band and out-of-band DTMF detector components
 *
 * @author Vladimir Morosev (vladimir.morosev@telestax.com)
 */
public class DtmfSink extends AbstractSink implements org.restcomm.media.core.spi.dtmf.DtmfDetector, DtmfDetectorListener {

    private static final long serialVersionUID = 450306501541827622L;

    private final static String[] oobEvtID = new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "#", "A", "B", "C", "D" };

    private final DtmfDetectorProvider dtmfProvider = null;

    private DtmfDetector detector;

    private final DtmfBuffer dtmfBuffer;

    private final Listeners<org.restcomm.media.core.spi.dtmf.DtmfDetectorListener> listeners;

    private final EventSender eventSender;

    private final PriorityQueueScheduler scheduler;

    private final AudioOutput output;
    private final OOBOutput oobOutput;
    private final OOBDetector oobDetector;

    private static final Logger logger = LogManager.getLogger(DtmfSink.class);

    public DtmfSink(String name, PriorityQueueScheduler scheduler) {
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
        this.dtmfBuffer.setInterdigitInterval(detector.getToneInterval());
        this.eventSender = new EventSender();
        this.listeners = new Listeners<org.restcomm.media.core.spi.dtmf.DtmfDetectorListener>();
    }
    
    public AudioOutput getAudioOutput() {
        return this.output;
    }

    public OOBOutput getOOBOutput() {
        return this.oobOutput;
    }

    @Override
    public void activate() {
        this.dtmfBuffer.clear();
        output.start();
        oobOutput.start();
    }

    @Override
    public void deactivate() {
        output.stop();
        oobOutput.stop();
    }

    @Override
    public int getVolume() {
        return 0;
    }

    @Override
    public int getInterdigitInterval() {
        return 0;
    }

    @Override
    public void flushBuffer() {
        dtmfBuffer.flush();
    }

    @Override
    public void clearDigits() {
        dtmfBuffer.clear();
    }

    @Override
    public void onMediaTransfer(Frame buffer) throws IOException {
        detector.detect(buffer.getData(), buffer.getDuration() / 1000000);
    }

    @Override
    public void onDtmfDetected(String tone) {
        dtmfBuffer.push(tone);
    }

    @Override
    public void addListener(org.restcomm.media.core.spi.dtmf.DtmfDetectorListener listener) throws TooManyListenersException {
        listeners.add(listener);
    }

    @Override
    public void removeListener(org.restcomm.media.core.spi.dtmf.DtmfDetectorListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void clearAllListeners() {
        listeners.clear();
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
}
