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

import org.mobicents.media.server.component.InbandComponent;
import org.mobicents.media.server.component.MediaSplitter;
import org.mobicents.media.server.concurrent.ConcurrentMap;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

/**
 * Implements compound audio splitter , one of core components of mms 3.0
 * 
 * @author Yulian Oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class AudioMixingSplitter implements MediaSplitter {

    // Format of the output stream.
    private static final AudioFormat LINEAR_FORMAT = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
    private static final long PERIOD = 20000000L;
    private static final int PACKET_SIZE = (int) (PERIOD / 1000000) * LINEAR_FORMAT.getSampleRate() / 1000
            * LINEAR_FORMAT.getSampleSize() / 8;

    // Pools of components
    private final ConcurrentMap<InbandComponent> insideComponents;
    private final ConcurrentMap<InbandComponent> outsideComponents;

    private Iterator<InbandComponent> insideRIterator;
    private Iterator<InbandComponent> insideSIterator;

    private Iterator<InbandComponent> outsideRIterator;
    private Iterator<InbandComponent> outsideSIterator;

    // Media splitting jobs
    private final Scheduler scheduler;
    private final InsideMixTask insideMixer;
    private final OutsideMixTask outsideMixer;
    private volatile boolean started = false;
    protected long mixCount = 0;

    // gain value
    private double gain = 1.0;

    public AudioMixingSplitter(Scheduler scheduler) {
        // Pools of components
        this.insideComponents = new ConcurrentMap<InbandComponent>();
        this.outsideComponents = new ConcurrentMap<InbandComponent>();

        // Media splitting jobs
        this.scheduler = scheduler;
        this.insideMixer = new InsideMixTask();
        this.outsideMixer = new OutsideMixTask();
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

    protected int getPacketSize() {
        return PACKET_SIZE;
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
            this.mixCount = 0;
            this.started = true;
            this.scheduler.submit(insideMixer, Scheduler.MIXER_MIX_QUEUE);
            this.scheduler.submit(outsideMixer, Scheduler.MIXER_MIX_QUEUE);
        }
    }

    @Override
    public void stop() {
        if (started) {
            this.started = false;
            this.insideMixer.cancel();
            this.outsideMixer.cancel();
        }
    }

    private int[] depacketize(Frame... frames) {
        int[] data = new int[PACKET_SIZE / 2];
        boolean first = true;

        for (Frame frame : frames) {
            byte[] dataArray = frame.getData();
            int inputIndex = 0;

            if (first) {
                for (int inputCount = 0; inputCount < dataArray.length; inputCount += 2) {
                    data[inputIndex++] = (short) (((dataArray[inputCount + 1]) << 8) | (dataArray[inputCount] & 0xff));
                }
                first = false;
            } else {
                for (int inputCount = 0; inputCount < dataArray.length; inputCount += 2) {
                    data[inputIndex++] += (short) (((dataArray[inputCount + 1]) << 8) | (dataArray[inputCount] & 0xff));
                }
            }
            frame.recycle();
        }
        return data;
    }

    private Frame packetize(int[] data) {
        // Allocate new media frame
        Frame frame = Memory.allocate(PACKET_SIZE);
        frame.setOffset(0);
        frame.setLength(PACKET_SIZE);
        frame.setDuration(PERIOD);
        frame.setFormat(LINEAR_FORMAT);

        // Fill payload with mixed data
        int index = 0;
        byte[] payload = frame.getData();
        for (int count = 0; count < data.length;) {
            payload[index++] = (byte) (data[count]);
            payload[index++] = (byte) (data[count++] >> 8);
        }
        return frame;
    }

    private class InsideMixTask extends Task {
        Boolean first = false;
        private int i;
        private int minValue = 0;
        private int maxValue = 0;
        private double currGain = 0;
        private int[] total = new int[PACKET_SIZE / 2];
        private int[] current;

        public InsideMixTask() {
            super();
        }

        @Override
        public int getQueueNumber() {
            return Scheduler.MIXER_MIX_QUEUE;
        }

        @Override
        public long perform() {
            // summarize all
            first = true;
            insideRIterator = insideComponents.valuesIterator();

            while (insideRIterator.hasNext()) {
                InbandComponent component = insideRIterator.next();
                Frame[] frames = component.retrieveData(LINEAR_FORMAT);

                if (frames.length > 0) {
                    current = depacketize(frames);
                    if (first) {
                        System.arraycopy(current, 0, total, 0, total.length);
                        first = false;
                    } else {
                        for (i = 0; i < total.length; i++) {
                            total[i] += current[i];
                        }
                    }

                    // Recycle frames for reuse
                    for (Frame frame : frames) {
                        frame.recycle();
                    }
                    frames = null;
                }
            }

            if (first) {
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
                total[i] = (short) Math.round((double) total[i] * currGain);
            }

            // get data for each component
            outsideSIterator = outsideComponents.valuesIterator();
            while (outsideSIterator.hasNext()) {
                InbandComponent component = outsideSIterator.next();
                component.submitData(packetize(total));
            }

            scheduler.submit(this, Scheduler.MIXER_MIX_QUEUE);
            mixCount++;
            return 0;
        }
    }

    private class OutsideMixTask extends Task {
        Boolean first = false;
        private int i;
        private int minValue = 0;
        private int maxValue = 0;
        private double currGain = 0;
        private int[] total = new int[PACKET_SIZE / 2];
        private int[] current;

        public OutsideMixTask() {
            super();
        }

        @Override
        public int getQueueNumber() {
            return Scheduler.MIXER_MIX_QUEUE;
        }

        @Override
        public long perform() {
            // summarize all
            first = true;
            outsideRIterator = outsideComponents.valuesIterator();

            while (outsideRIterator.hasNext()) {
                InbandComponent component = outsideRIterator.next();
                Frame[] frames = component.retrieveData(LINEAR_FORMAT);

                if (frames.length > 0) {
                    current = depacketize(frames);
                    if (first) {
                        System.arraycopy(current, 0, total, 0, total.length);
                        first = false;
                    } else {
                        for (i = 0; i < total.length; i++) {
                            total[i] += current[i];
                        }
                    }
                }
            }

            if (first) {
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

            minValue = 0 - minValue;
            if (minValue > maxValue) {
                maxValue = minValue;
            }

            currGain = gain;
            if (maxValue > Short.MAX_VALUE) {
                currGain = (currGain * Short.MAX_VALUE) / maxValue;
            }

            for (i = 0; i < total.length; i++) {
                total[i] = (short) Math.round((double) total[i] * currGain);
            }

            // get data for each component
            insideSIterator = insideComponents.valuesIterator();
            while (insideSIterator.hasNext()) {
                InbandComponent component = insideSIterator.next();
                component.submitData(packetize(total));
            }

            scheduler.submit(this, Scheduler.MIXER_MIX_QUEUE);
            mixCount++;
            return 0;
        }
    }
}
