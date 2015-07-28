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

import org.mobicents.media.server.concurrent.ConcurrentMap;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;

/**
 * Implementation of a translator that forwards packets to all registered receivers.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @see <a href="http://tools.ietf.org/html/rfc3550#section-2.3">RFC3550 - Section 2.3 - Mixers and Translators</a>
 * @see <a href="http://tools.ietf.org/html/rfc3550#section-7">RFC3550 - Section 7 - RTP Translators and Mixers</a>
 */
public class AudioTranslator {

    // Pool of components
    private final ConcurrentMap<AudioComponent> components;

    // Schedulers for translator job scheduling
    private final Scheduler scheduler;
    private final TranslateTask task;
    private volatile boolean started;
    private volatile int executionCount;

    public AudioTranslator(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.task = new TranslateTask();
        this.components = new ConcurrentMap<AudioComponent>();
        this.started = false;
        this.executionCount = 0;
    }

    public int getExecutionCount() {
        return executionCount;
    }

    /**
     * Register a new input stream
     * 
     * @param component the input stream
     */
    public void addComponent(AudioComponent component) {
        components.put(component.getComponentId(), component);
    }

    /**
     * Releases unused input stream
     * 
     * @param input the input stream previously created
     */
    public void release(AudioComponent component) {
        components.remove(component.getComponentId());
    }

    public void start() {
        if (!this.started) {
            this.started = true;
            this.scheduler.submit(this.task, Scheduler.MIXER_MIX_QUEUE);
        }
    }

    public void stop() {
        if (this.started) {
            this.started = false;
            this.task.cancel();
        }
    }

    private class TranslateTask extends Task {
        private int currentData[];

        @Override
        public int getQueueNumber() {
            return Scheduler.MIXER_MIX_QUEUE;
        }

        @Override
        public long perform() {
            // Execute each readable component and get its data
            for (AudioComponent component : components.values()) {
                if (component.shouldRead) {
                    component.perform();
                    this.currentData = component.getData();

                    // Offer the data of the current component to all the other writable components
                    if (this.currentData != null && this.currentData.length > 0) {
                        for (AudioComponent otherComponent : components.values()) {
                            if (!component.equals(otherComponent)) {
                                if (otherComponent.shouldWrite) {
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
