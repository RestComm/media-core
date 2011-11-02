/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media.server.impl.dsp;

import java.io.IOException;
import java.util.ArrayList;

import java.util.Collection;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.BaseComponent;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.dsp.SignalingProcessor;

/**
 * Implements DSP features.
 * 
 * Processor has input and output and is used to perform required 
 * transcoding if needed for packets followed from source to consumer. 
 * Processor is transparent for packets with format acceptable by consumer 
 * by default. 
 * 
 * @author Oleg Kulikov
 */
public class Processor extends BaseComponent implements SignalingProcessor {

    private Input input;
    private Output output;
    private transient ArrayList<Codec> codecs = new ArrayList();
    private Codec codec;
    private Buffer buff;
    private long timestamp;

    public Processor(String name) {
        super(name);
        input = new Input(name);
        output = new Output(name);
    }

    protected void add(Codec codec) {
        codecs.add(codec);
    }

    public Codec getActiveCodec() {
        return codec;
    }
    
    /**
     * Gets the input for original media
     * 
     * @return media handler for input media.
     */
    public MediaSink getInput() {
        return input;
    }

    /**
     * Gets the output stream with transcoded media.
     * 
     * @return media stream.
     */
    public MediaSource getOutput() {
        return output;
    }

    public void connect(MediaSource source) {
        input.connect(source);
    }

    public void disconnect(MediaSource source) {
        input.disconnect(source);
    }

    public void connect(MediaSink sink) {
        output.connect(sink);
    }

    public void disconnect(MediaSink sink) {
        output.disconnect(sink);
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.Component#start() 
     */
    public void start() {
        //start input and output channel
        if (!input.isStarted()) {
            input.start();
        }
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.Component#stop() 
     */
    public void stop() {
        if (input.isStarted()) {
            input.stop();
        }
    }

    private boolean contains(Format[] list, int count, Format item) {
        for (int i = 0; i < count; i++) {
            if (list[i].matches(item)) {
                return true;
            }
        }
        return false;
    }
    /**
     * Implements input of the processor.
     */
    private class Input extends AbstractSink {

        private volatile boolean started = false;

        public Input(String name) {
            super(name + ".input");
        }

        @Override
        public Format selectPreffered(Collection<Format> set) {
            if (set == null) {
                codec = null;
                return null;
            }
            
            //input and output should be connected to termine preffred format
            if (!output.isConnected()) {
                return null;
            }
            
            Format outFormat = output.getFormat();
            //the next case when output is connected but format is not selected yet
            //in this case format will be choosen later
            if (outFormat == null) {
                return null;
            }
            
            //possible that output format is presented in suggesed set
            for (Format f : set) {
                if (f.matches(outFormat)) {
                    return f;
                }
            }
            
            //transcoding required. we need select codec.
            codec = null;
            for (Codec c : codecs) {
                if (c.getSupportedOutputFormat().matches(outFormat)) {
                    for (Format f: set) {
                        if (c.getSupportedInputFormat().matches(f)) {
                            codec = c;
                            return f;
                        }
                    }
                }
            }

            return null;
        }
        
        @Override
        public boolean isStarted() {
            return this.started;
        }

        @Override
        public void start() {
            this.started = true;
            if (!output.isStarted()) {
                output.start();
            }
            if (otherParty != null && !otherParty.isStarted()) {
                otherParty.start();
            }
        }

        @Override
        public void stop() {
            this.started = false;
            if (output.isStarted()) {
                output.stop();
            }
            if (otherParty != null && otherParty.isStarted()) {
                otherParty.stop();
            }
        }

        /**
         * (Non Java-doc.)
         * 
         * @see org.mobicents.media.server.impl.AbstractSink#onMediaTransfer(org.mobicents.media.Buffer) 
         */
        public void onMediaTransfer(Buffer buffer) throws IOException {
            timestamp = buffer.getTimeStamp();
            output.transmit(buffer);
        }

        /**
         * Gets list of formats supported by connected other party
         * 
         * @return the array of format objects.
         */
        protected Format[] getOtherPartyFormats() {
            return otherParty != null ? otherParty.getFormats() : new Format[0];
        }

        /**
         * (Non Java-doc.)
         * 
         * @see org.mobicents.media.MediaSink#getFormats() 
         */
        public Format[] getFormats() {
            //if output is not connected then format can be ANY
            if (!output.isConnected()) {
                return new Format[]{Format.ANY};
            }
            
            //Formats supported by component connected on output 
            //if preffered format is already know we are using it
            Format[] formats = output.getFormat() != null ? 
                new Format[]{output.getFormat()} :
                output.getOtherPartyFormats();
            
            //Case #1. The component connected on output supports format ANY.
            //In this case the input of this processor also should return ANY also.
            if (formats.length == 1 && formats[0] == Format.ANY) {
                return new Format[]{Format.ANY};
            }
            
            //Case #2. The componentconnected to output supports set of concrete formats.
            //In this case the input is same set of formats plus possible transcodings.
            Format[] extended = new Format[20];
            int count = 0;
            
            for (Format f : formats) {
                //copy original format first
                extended[count++] = f;
                //search transcodings
                for (Codec codec : codecs) { 
                    //add formats which are results of transcoding if such formats
                    //are not in the list yet
                    if (codec.getSupportedOutputFormat().matches(f) && 
                            !contains(extended, count, codec.getSupportedInputFormat())) {
                        extended[count++] = codec.getSupportedInputFormat();
                    }
                }
            }
            
            //resize array
            Format[] res = new Format[count];
            System.arraycopy(extended, 0, res, 0, count);
            
            return res;
        }


        @Override
        public String toString() {
            return "Processor.Input[" + getName() + "]";
        }

        public long getTimestamp() {
            return timestamp;
        }

    }

    /**
     * Implements output of the processor.
     */
    private class Output extends AbstractSource {
        private volatile boolean started = false;

        /**
         * Creates new instance of processor's output.
         * 
         * @param name - the name of the processor;
         */
        public Output(String name) {
            super(name + ".output");
        }

        /**
         * Gets list of formats supported by connected other party
         * 
         * @return the array of format objects.
         */
        protected Format[] getOtherPartyFormats() {
            return otherParty != null ? otherParty.getFormats() : new Format[0];
        }

        @Override
        public boolean isStarted() {
            return this.started;
        }

        @Override
        public void start() {
            this.started = true;
            if (!input.isStarted()) {
                input.start();
            }
            if (otherParty != null && !otherParty.isStarted()) {
                otherParty.start();
            }
        }

        @Override
        public void stop() {
            this.started = false;
            if (input.isStarted()) {
                input.stop();
            }
            if (otherParty != null && otherParty.isStarted()) {
                otherParty.stop();
            }
        }

        /**
         * Asks the sink connected to this channel to determine preffred format
         * 
         * @param set the set of format to choose preffred from
         * @return the selected preffred format
         */
        public Format getOtherPartyPreffered(Collection<Format> set) {
            return ((AbstractSink)otherParty).getPreffered(set);
        }
        
        @Override
        public void setPreffered(Format fmt) {
            super.setPreffered(fmt);
            
            //if input is connected we have to update its soource
            if (!input.isConnected()) {
                return;
            }
            
            //if selected format matches to one input format we have to chose it as preffred
            Format[] supported = input.getOtherPartyFormats();
            for (Format f : supported) {
                if (f.matches(fmt)) {
                    input.assignPreffered(fmt);
                    return;
                }
            }
            
            //at this point we have to select codec
            for (Codec c: codecs) {
                if (c.getSupportedOutputFormat().matches(fmt)) {
                    for (Format f : supported) {
                        if (f.matches(c.getSupportedInputFormat())) {
                            System.out.println("Assigned codec=" + codec);
                            codec = c;
                            input.assignPreffered(f);
                            return;
                        }
                    }
                }
            }
            codec = null;
        }

        /**
         * (Non Java-doc.)
         * 
         * @see org.mobicents.media.MediaSource#getFormats() 
         */
        public Format[] getFormats() {
            //if input is not connected then format can be ANY
            if (!input.isConnected()) {
                return new Format[]{Format.ANY};
            }
            
            //Formats supported by component connected on input            
            //if preffered format is already know we are using it
            Format[] formats = input.getFormat() != null ? 
                new Format[]{input.getFormat()} :
                input.getOtherPartyFormats();
            
            //Case #1. The component connected on input supports format ANY.
            //In this case the output of this processor also should return ANY also.
            if (formats.length == 1 && formats[0] == Format.ANY) {
                return new Format[]{Format.ANY};
            }
            
            //Case #2. The componentconnected to input supports set of concrete formats.
            //In this case the output is same set of formats plus possible transcodings.
            Format[] extended = new Format[20];
            int count = 0;
            
            for (Format f : formats) {
                //copy original format first
                extended[count++] = f;
                //search transcodings
                for (Codec codec : codecs) { 
                    //add formats which are results of transcoding if such formats
                    //are not in the list yet
                    if (codec.getSupportedInputFormat().matches(f) && 
                            !contains(extended, count, codec.getSupportedOutputFormat())) {
                        extended[count++] = codec.getSupportedOutputFormat();
                    }
                }
            }
            
            //resize array
            Format[] res = new Format[count];
            System.arraycopy(extended, 0, res, 0, count);
            
            return res;
        }

        /**
         * Transmits buffer to the output handler.
         * 
         * @param buffer the buffer to transmit
         */
        protected void transmit(Buffer buffer) {
            if (!started) {
                buffer.dispose();
                return;
            }
            //Here we work in ReceiveStream.run method, which runs in local ReceiveStreamTimer
            // Discard packet silently if output handler is not assigned yet
            if (otherParty == null) {
                buffer.dispose();
                return;
            }

            if (codec != null) {
                codec.process(buffer);
            }
            // Codec can delay media transition if it has not enouph media
            // to perform its job. 
            // It means that Processor should check FLAGS after codec's 
            // work and discard packet if required
            if (buffer.getFlags() == Buffer.FLAG_DISCARD) {
                buffer.dispose();
                return;
            }

            //may be a situation when original format can not be trancoded to 
            //one of the required output. In this case codec map will have no 
            //entry for this format. also codec may has no entry in case of when 
            //transcoding is not required. to differentiate these two cases check
            //if this format is acceptable by the consumer.

            //deliver packet to the consumer
            buff = buffer;
            perform();
        }

        @Override
        public void evolve(Buffer buffer, long timestamp) {
            buffer.copy(buff);
        }

        @Override
        public String toString() {
            return "Processor.Output[" + getName() + "]";
        }
    }
}
