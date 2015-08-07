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

package org.mobicents.media.server.component.audio;

import java.util.Iterator;

import org.mobicents.media.server.component.MediaRelay;
import org.mobicents.media.server.concurrent.ConcurrentMap;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;

/**
 * Implements compound audio mixer , one of core components of mms 3.0
 * 
 * @author Yulian Oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class AudioMixer implements MediaRelay {

    // The format of the output stream
    private static final AudioFormat LINEAR_FORMAT = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
    private static final long PERIOD = 20000000L;
    private static final int PACKET_SIZE = (int) (PERIOD / 1000000) * LINEAR_FORMAT.getSampleRate() / 1000
            * LINEAR_FORMAT.getSampleSize() / 8;

    // The pool of components
    private final ConcurrentMap<AudioComponent> components = new ConcurrentMap<AudioComponent>();
    private Iterator<AudioComponent> activeComponents;

    // scheduler for mixer job scheduling
    private final Scheduler scheduler;
    private MixTask mixer;
    private volatile boolean started = false;
    private long mixCount = 0;

    // Gain value
    private double gain = 1.0;

    public AudioMixer(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.mixer = new MixTask();
    }

    @Override
    public void addComponent(AudioComponent component) {
        components.put(component.getComponentId(), component);
    }

    @Override
    public void removeComponent(AudioComponent component) {
        components.remove(component.getComponentId());
    }

    protected int getPacketSize() {
        return PACKET_SIZE;
    }

    public long getMixCount() {
        return mixCount;
    }

    /**
     * Modify gain of the output stream.
     * 
     * @param gain the new value of the gain in dBm.
     */
    public void setGain(double gain) {
        this.gain = gain > 0 ? gain * 1.26 : gain == 0 ? 1 : 1 / (gain * 1.26);
    }

    @Override
    public void start() {
        if (!started) {
            started = true;
            mixCount = 0;
            scheduler.submit(mixer, Scheduler.MIXER_MIX_QUEUE);
        }
    }

    @Override
    public void stop() {
        if (started) {
            started = false;
            mixer.cancel();
        }
    }

    private class MixTask extends Task {
        int sourcesCount = 0;
        private int i;
        private int minValue = 0;
        private int maxValue = 0;
        private double currGain = 0;
        private int[] total = new int[PACKET_SIZE / 2];
        private int[] current;

        public MixTask() {
            super();
        }

        @Override
        public int getQueueNumber() {
            return Scheduler.MIXER_MIX_QUEUE;
        }

        @Override
        public long perform() {
            // summarize all
            sourcesCount = 0;
            activeComponents = components.valuesIterator();
            while (activeComponents.hasNext()) {
                AudioComponent component = activeComponents.next();
                current = component.retrieveData();
                if (current != null && current.length > 0) {
                    if (sourcesCount == 0) {
                        System.arraycopy(current, 0, total, 0, total.length);
                    } else {
                        for (i = 0; i < total.length; i++) {
                            total[i] += current[i];
                        }
                    }
                    sourcesCount++;
                }
            }

            if (sourcesCount == 0) {
                scheduler.submit(this, Scheduler.MIXER_MIX_QUEUE);
                mixCount++;
                return 0;
            }

            minValue = 0;
            maxValue = 0;
            for (i = 0; i < total.length; i++) {
                if (total[i] > maxValue) {
                    maxValue = total[i];
                } else if (total[i] < minValue) {
                    minValue = total[i];
                }
            }

            if (minValue > 0) {
                minValue = 0 - minValue;
            }

            if (minValue > maxValue) {
                maxValue = minValue;
            }

            currGain = gain;
            if (maxValue > Short.MAX_VALUE) {
                currGain = (currGain * (double) Short.MAX_VALUE) / (double) maxValue;
            }

            for (i = 0; i < total.length; i++) {
                total[i] = (short) ((double) total[i] * currGain);
            }

            // get data for each component
            activeComponents = components.valuesIterator();
            while (activeComponents.hasNext()) {
                AudioComponent component = activeComponents.next();
                current = component.retrieveData();
                if (current != null && current.length > 0 && sourcesCount > 1) {
                    for (i = 0; i < total.length; i++) {
                        current[i] = total[i] - (short) ((double) current[i] * currGain);
                    }
                    component.offerData(current);
                } else if (current == null) {
                    component.offerData(total);
                }
            }

            scheduler.submit(this, Scheduler.MIXER_MIX_QUEUE);
            mixCount++;
            return 0;
        }
    }
}
