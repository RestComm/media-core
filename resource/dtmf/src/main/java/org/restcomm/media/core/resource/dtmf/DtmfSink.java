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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.core.component.AbstractSink;
import org.restcomm.media.core.component.audio.AudioOutput;
import org.restcomm.media.core.scheduler.PriorityQueueScheduler;
import org.restcomm.media.core.scheduler.Task;
import org.restcomm.media.core.spi.ComponentType;
import org.restcomm.media.core.spi.memory.Frame;

/**
 * DTMF sink with in-band and out-of-band DTMF detector components
 *
 * @author Vladimir Morosev (vladimir.morosev@telestax.com)
 */
public class DtmfSink extends AbstractSink implements DtmfEventObserver, DtmfEventSubject {

    private static final long serialVersionUID = 450306501541827622L;

    private final DtmfDetectorProvider inbandDetectorProvider;

    private DtmfDetector inbandDetector;
    private DtmfDetector oobDetector;

    private Set<DtmfEventObserver> observers = ConcurrentHashMap.newKeySet();

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
    public void onMediaTransfer(Frame buffer) throws IOException {
        inbandDetector.detect(buffer.getData(), buffer.getDuration() / 1000000, 0);
        oobDetector.detect(buffer.getData(), buffer.getDuration() / 1000000, buffer.getSequenceNumber());
    }

    @Override
    public void onDtmfEvent(DtmfEvent event) {
        eventSender.events.add(new DtmfEvent(event.getTone()));
        // schedule event delivery
        scheduler.submit(eventSender, PriorityQueueScheduler.INPUT_QUEUE);
    }

    @Override
    public void notify(DtmfEvent event) {
        // Inform observers about DTMF tone detection
        for (DtmfEventObserver observer : observers) {
            observer.onDtmfEvent(event);
        }
    }

    @Override
    public void observe(DtmfEventObserver observer) {
       final boolean added = this.observers.add(observer);
        if (added && logger.isDebugEnabled()) {
            logger.debug("Registered observer DtmfEventObserver@" + observer.hashCode() + ". Count: " + observers.size());
        }
    }

    @Override
    public void forget(DtmfEventObserver observer) {
        final boolean removed = observers.remove(observer);
        if (removed && logger.isDebugEnabled()) {
            logger.debug("Unregistered observer DtmfEventObserver@" + observer.hashCode() + ". Count: " + observers.size());
        }
    }

    public class EventSender extends Task {

        private final Queue<DtmfEvent> events = new ConcurrentLinkedQueue<>();

        public EventSender() {
            super();
        }

        @Override
        public int getQueueNumber() {
            return PriorityQueueScheduler.INPUT_QUEUE;
        }

        @Override
        public long perform() {
            final Iterator<DtmfEvent> iterator = this.events.iterator();
            while (iterator.hasNext()) {
                DtmfEvent evt = iterator.next();

                // try to deliver or queue to buffer if not delivered
                DtmfSink.this.notify(evt);
                if (logger.isInfoEnabled()) {
                    logger.info(String.format("(%s) Delivered '%s' tone", getName(), evt.getTone()));
                }

                // Remove event from collection
                iterator.remove();
            }
            return 0;
        }
    }

}
