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
import org.mobicents.media.server.component.MediaSplitter;
import org.mobicents.media.server.concurrent.ConcurrentMap;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;

/**
 * Audio splitter that forwards traffic between components.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AudioForwardingSplitter implements MediaSplitter {

    // Format of the output stream.
    private static final AudioFormat LINEAR_FORMAT = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
    private static final long PERIOD = 20000000L;
    private static final int PACKET_SIZE = (int) (PERIOD / 1000000) * LINEAR_FORMAT.getSampleRate() / 1000
            * LINEAR_FORMAT.getSampleSize() / 8;

    // Pools of components
    private final ConcurrentMap<InbandComponent> insideComponents;
    private final ConcurrentMap<InbandComponent> outsideComponents;

    // Media splitting jobs
    private final Scheduler scheduler;
    private final InsideForwardingTask insideTask;
    private final OutsideForwardingTask outsideTask;
    private volatile boolean started;

    public AudioForwardingSplitter(Scheduler scheduler) {
        // Pools of components
        this.insideComponents = new ConcurrentMap<InbandComponent>();
        this.outsideComponents = new ConcurrentMap<InbandComponent>();

        // Media splitting jobs
        this.scheduler = scheduler;
        this.insideTask = new InsideForwardingTask();
        this.outsideTask = new OutsideForwardingTask();
        this.started = false;
    }

    @Override
    public void addInsideComponent(InbandComponent component) {
        this.insideComponents.put(component.getComponentId(), component);
    }

    @Override
    public void removeInsideComponent(InbandComponent component) {
        this.insideComponents.remove(component.getComponentId());
    }

    @Override
    public void addOutsideComponent(InbandComponent component) {
        this.outsideComponents.put(component.getComponentId(), component);
    }

    @Override
    public void removeOutsideComponent(InbandComponent component) {
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

        private final int[] data = new int[PACKET_SIZE / 2];

        @Override
        public int getQueueNumber() {
            return Scheduler.MIXER_MIX_QUEUE;
        }

        @Override
        public long perform() {
            // Retrieve data from each readable component
            for (InbandComponent insideComponent : insideComponents.values()) {
                if (insideComponent.shouldRead) {
                    insideComponent.perform();
                    if (insideComponent.hasData()) {
                        System.arraycopy(insideComponent.getData(), 0, data, 0, data.length);
                        if (data != null && data.length > 0) {
                            // Pass the data to all outside components with write permission
                            for (InbandComponent outsideComponent : outsideComponents.values()) {
                                if (outsideComponent.shouldWrite) {
                                    outsideComponent.offer(data);
                                }
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

        private final int[] data = new int[PACKET_SIZE / 2];

        @Override
        public int getQueueNumber() {
            return Scheduler.MIXER_MIX_QUEUE;
        }

        @Override
        public long perform() {
            // Retrieve data from each readable component
            for (InbandComponent outsideComponent : outsideComponents.values()) {
                if (outsideComponent.shouldRead) {
                    outsideComponent.perform();
                    if (outsideComponent.hasData()) {
                        System.arraycopy(outsideComponent.getData(), 0, data, 0, data.length);
                        if (data != null && data.length > 0) {
                            // Pass the data to all outside components with write permission
                            for (InbandComponent insideComponent : insideComponents.values()) {
                                if (insideComponent.shouldWrite) {
                                    insideComponent.offer(data);
                                }
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
