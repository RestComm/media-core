/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.server.component.audio;

import org.mobicents.media.server.component.InbandComponent;
import org.mobicents.media.server.component.MediaRelay;
import org.mobicents.media.server.concurrent.ConcurrentMap;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;

/**
 * Implementation of a translator that forwards packets to all registered receivers.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @see <a href="http://tools.ietf.org/html/rfc3550#section-2.3">RFC3550 - Section 2.3 - Mixers and Translators</a>
 * @see <a href="http://tools.ietf.org/html/rfc3550#section-7">RFC3550 - Section 7 - RTP Translators and Mixers</a>
 */
public class AudioTranslator implements MediaRelay {

    // The format of the output stream
    private static final AudioFormat LINEAR_FORMAT = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
    private static final long PERIOD = 20000000L;
    private static final int PACKET_SIZE = (int) (PERIOD / 1000000) * LINEAR_FORMAT.getSampleRate() / 1000
            * LINEAR_FORMAT.getSampleSize() / 8;

    // Pool of components
    private final ConcurrentMap<InbandComponent> components;

    // Schedulers for translator job scheduling
    private final Scheduler scheduler;
    private final TranslateTask task;
    private volatile boolean started;
    private volatile int executionCount;

    public AudioTranslator(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.task = new TranslateTask();
        this.components = new ConcurrentMap<InbandComponent>();
        this.started = false;
        this.executionCount = 0;
    }

    public int getExecutionCount() {
        return executionCount;
    }

    @Override
    public void addComponent(InbandComponent component) {
        components.put(component.getComponentId(), component);
    }

    @Override
    public void removeComponent(InbandComponent component) {
        components.remove(component.getComponentId());
    }

    @Override
    public void start() {
        if (!this.started) {
            this.started = true;
            this.scheduler.submit(this.task, Scheduler.MIXER_MIX_QUEUE);
        }
    }

    @Override
    public void stop() {
        if (this.started) {
            this.started = false;
            this.task.cancel();
            this.executionCount = 0;
        }
    }

    private class TranslateTask extends Task {

        private final int currentData[] = new int[PACKET_SIZE / 2];

        @Override
        public int getQueueNumber() {
            return Scheduler.MIXER_MIX_QUEUE;
        }

        @Override
        public long perform() {
            // Execute each readable component and get its data
            for (InbandComponent component : components.values()) {
                if (component.isReadable()) {
                    component.perform();
                    if (component.hasData()) {
                        System.arraycopy(component.getData(), 0, currentData, 0, currentData.length);

                        // Offer the data of the current component to all the other writable components
                        for (InbandComponent otherComponent : components.values()) {
                            if (!component.equals(otherComponent)) {
                                if (otherComponent.isWritable()) {
                                    otherComponent.offer(currentData);
                                }
                            }
                        }
                    }
                }
            }
            executionCount++;

            // Re-submit the task to the scheduler to be continuously executed
            scheduler.submit(this, Scheduler.MIXER_MIX_QUEUE);
            return 0;
        }

    }

}
