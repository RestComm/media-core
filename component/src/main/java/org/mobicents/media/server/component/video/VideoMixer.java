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

package org.mobicents.media.server.component.video;

import java.io.IOException;
import java.util.ArrayList;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.component.Mixer;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.memory.Frame;

/**
 * Overlaps several video streams into single one.
 *
 * @author kulikov
 */
public class VideoMixer implements Mixer {

    private Scheduler scheduler;
    private Output output;
    private ArrayList<Input> inputs = new ArrayList(100);

    private Formats formats = new Formats();

    public VideoMixer(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.output = new Output(scheduler);
    }

    public MediaSource getOutput() {
        return output;
    }

    public MediaSink newInput() {
        return inputs.remove(0);
    }

    public void release(MediaSink input) {
        inputs.add((Input) input);
    }

    public void start() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void stop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String report() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private class Input extends AbstractSink {

        public Input(Scheduler scheduler) {
            super("video.mixer.input", scheduler,scheduler.MIXER_INPUT_QUEUE);
        }

        @Override
        public void onMediaTransfer(Frame frame) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class Output extends AbstractSource {

        public Output(Scheduler scheduler) {
            super("video.mixer.output", scheduler,scheduler.MIXER_OUTPUT_QUEUE);
        }

        @Override
        public Frame evolve(long timestamp) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
