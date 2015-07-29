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

import org.mobicents.media.server.component.MediaSplitter;
import org.mobicents.media.server.concurrent.ConcurrentMap;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;

/**
 * Audio splitter that forwards traffic between components.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AudioForwardingSplitter implements MediaSplitter {

    // Pools of components
    private final ConcurrentMap<AudioComponent> insideComponents;
    private final ConcurrentMap<AudioComponent> outsideComponents;

    // Media splitting jobs
    private final Scheduler scheduler;
    private final InsideForwardingTask insideTask;
    private final OutsideForwardingTask outsideTask;
    private volatile boolean started;

    public AudioForwardingSplitter(Scheduler scheduler) {
        // Pools of components
        this.insideComponents = new ConcurrentMap<AudioComponent>();
        this.outsideComponents = new ConcurrentMap<AudioComponent>();

        // Media splitting jobs
        this.scheduler = scheduler;
        this.insideTask = new InsideForwardingTask();
        this.outsideTask = new OutsideForwardingTask();
        this.started = false;
    }

    @Override
    public void addInsideComponent(AudioComponent component) {
        this.insideComponents.put(component.getComponentId(), component);
    }

    @Override
    public void removeInsideComponent(AudioComponent component) {
        this.insideComponents.remove(component.getComponentId());
    }

    @Override
    public void addOutsideComponent(AudioComponent component) {
        this.outsideComponents.put(component.getComponentId(), component);
    }

    @Override
    public void removeOutsideComponent(AudioComponent component) {
        this.outsideComponents.remove(component.getComponentId());
    }

    @Override
    public void start() {
        if (!started) {
            this.started = true;
            this.scheduler.submit(this.insideTask, Scheduler.MIXER_MIX_QUEUE);
            this.scheduler.submit(this.outsideTask, Scheduler.MIXER_MIX_QUEUE);
        }
    }

    @Override
    public void stop() {
        if (started) {
            this.started = false;
            this.insideTask.cancel();
            this.outsideTask.cancel();
        }
    }

    private final class InsideForwardingTask extends Task {

        @Override
        public int getQueueNumber() {
            return Scheduler.MIXER_MIX_QUEUE;
        }

        @Override
        public long perform() {
            // Retrieve data from each readable component
            for (AudioComponent insideComponent : insideComponents.values()) {
                if (insideComponent.shouldRead) {
                    insideComponent.perform();
                    int[] data = insideComponent.getData();

                    if (data != null && data.length > 0) {
                        // Pass the data to all outside components with write permission
                        for (AudioComponent outsideComponent : outsideComponents.values()) {
                            if (outsideComponent.shouldWrite) {
                                outsideComponent.offer(data);
                            }
                        }
                    }
                }
            }
            // Submit task to schedule for continuous execution
            scheduler.submit(this, Scheduler.MIXER_MIX_QUEUE);
            return 0;
        }

    }

    private final class OutsideForwardingTask extends Task {

        @Override
        public int getQueueNumber() {
            return Scheduler.MIXER_MIX_QUEUE;
        }

        @Override
        public long perform() {
            // Retrieve data from each readable component
            for (AudioComponent outsideComponent : outsideComponents.values()) {
                if (outsideComponent.shouldRead) {
                    outsideComponent.perform();
                    int[] data = outsideComponent.getData();

                    if (data != null && data.length > 0) {
                        // Pass the data to all outside components with write permission
                        for (AudioComponent insideComponent : insideComponents.values()) {
                            if (insideComponent.shouldWrite) {
                                insideComponent.offer(data);
                            }
                        }
                    }
                }
            }
            // Submit task to schedule for continuous execution
            scheduler.submit(this, Scheduler.MIXER_MIX_QUEUE);
            return 0;
        }

    }

}
