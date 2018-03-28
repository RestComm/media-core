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
public class DtmfSink extends AbstractSink implements org.restcomm.media.core.spi.dtmf.DtmfDetector, DtmfEventObserver {

    private static final long serialVersionUID = 450306501541827622L;

    private final DtmfDetectorProvider inbandDetectorProvider;

    private DtmfDetector inbandDetector;
    private DtmfDetector oobDetector;

    private final Listeners<org.restcomm.media.core.spi.dtmf.DtmfDetectorListener> listeners;

    private final EventSender eventSender;
    private final PriorityQueueScheduler scheduler;

    private final AudioOutput output;

    private static final Logger logger = LogManager.getLogger(DtmfSink.class);

    public DtmfSink(String name, PriorityQueueScheduler scheduler) {
        super(name);
        
        this.inbandDetectorProvider = null;
        this.inbandDetector = inbandDetectorProvider.provide();
        this.oobDetector = new Rfc2833DtmfDetector(500);
        this.eventSender = new EventSender();
        this.scheduler = scheduler;
        
        this.output = new AudioOutput(scheduler, ComponentType.DTMF_DETECTOR.getType());
        this.output.join(this);
        
        this.listeners = new Listeners<org.restcomm.media.core.spi.dtmf.DtmfDetectorListener>();
    }
    
    public AudioOutput getAudioOutput() {
        return this.output;
    }

    @Override
    public void activate() {
        output.start();
    }

    @Override
    public void deactivate() {
        output.stop();
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
    }

    @Override
    public void clearDigits() {
    }

    @Override
    public void onMediaTransfer(Frame buffer) throws IOException {
        inbandDetector.detect(buffer.getData(), buffer.getDuration() / 1000000, 0);
        oobDetector.detect(buffer.getData(), buffer.getDuration() / 1000000, buffer.getSequenceNumber());
    }

    @Override
    public void onDtmfEvent(DtmfEvent event) {
        eventSender.events.add(new DtmfEventImpl(this, event.getTone(), 0));
        // schedule event delivery
        scheduler.submit(eventSender, PriorityQueueScheduler.INPUT_QUEUE);
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
                    if (logger.isInfoEnabled()) {
                        logger.info(String.format("(%s) Not Delivered '%s' tone", getName(), evt.getTone()));
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

}
