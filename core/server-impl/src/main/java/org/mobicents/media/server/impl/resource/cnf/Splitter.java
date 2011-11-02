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
package org.mobicents.media.server.impl.resource.cnf;

import java.io.IOException;
import java.util.Collection;

import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.Inlet;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.AbstractSourceSet;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.dsp.Codec;

/**
 * Splits the media stream and distributes between several destinations, and outputs the resulting tracks. It has one
 * input and multiple outputs.
 * 
 * @author Oleg Kulikov
 */
public class Splitter extends AbstractSourceSet implements Inlet {

    private Input input = null;
    private Buffer buff;
    private long timestamp;
    private Format[] formats = new Format[]{Codec.LINEAR_AUDIO};

    /**
     * Creates new instance of the demultiplexer.
     * 
     * @param name
     */
    public Splitter(String name) {
        super(name);
        input = new Input(name);
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.Inlet#getInput().
     */
    public AbstractSink getInput() {
        return input;
    }

    public void connect(MediaSource source) {
        input.connect(source);
    }

    public void disconnect(MediaSource source) {
        input.disconnect(source);
    }

    @Override
    public AbstractSource createSource(MediaSink otherParty) {
        Output output = new Output(getName() + "[output]");
        output.setEndpoint(getEndpoint());
        output.setConnection(getConnection());

        return output;
    }

    @Override
    public void setConnection(Connection connection) {
        super.setConnection(connection);
        input.setConnection(connection);

        Collection<AbstractSource> list = getStreams();
        for (AbstractSource stream : list) {
            stream.setConnection(connection);
        }
    }

    @Override
    public void setEndpoint(Endpoint endpoint) {
        super.setEndpoint(endpoint);
        input.setEndpoint(endpoint);

        Collection<AbstractSource> list = getStreams();
        for (AbstractSource stream : list) {
            stream.setEndpoint(endpoint);
        }
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#getFormats().
     */
    public Format[] getFormats() {
        return formats;
    }

    @Override
    public void start() {
        input.start();
    }

    @Override
    public void stop() {
        input.stop();
    /*
     * Collection<AbstractSource> streams = getStreams(); for (AbstractSource stream : streams) { stream.stop(); }
     */
    }

    /**
     * Implements input stream of the Demultiplxer.
     * 
     */
    private class Input extends AbstractSink {

        /**
         * Creates new instance of input stream.
         * 
         * The name of the demultiplxer.
         */
        public Input(String name) {
            super(name + "[input]");
        }

        @Override
        protected Format selectPreffered(Collection<Format> set) {
            return Codec.LINEAR_AUDIO;
        }

        /**
         * (Non Java-doc).
         * 
         * @see org.mobicents.media.server.impl.AbstractSink#onMediaTransfer(org.mobicents.media.Buffer).
         */
        public void onMediaTransfer(Buffer buffer) throws IOException {
            buff = buffer;
            timestamp = buffer.getTimeStamp();
            Collection<AbstractSource> streams = getStreams();
            for (AbstractSource stream : streams) {
                if (stream.isStarted()) {
                    ((Output) stream).perform();
                }
            }
            buffer.dispose();
        }

        /**
         * (Non Java-doc).
         * 
         * @see org.mobicents.media.MediaSink#getFormats()
         */
        public Format[] getFormats() {
            return formats;
        }

    }

    /**
     * Implements output stream.
     */
    private class Output extends AbstractSource {

        public Output(String parent) {
            super(parent);
        }

        @Override
        public void start() {
            setStarted(true);
        }

        @Override
        public void stop() {
            setStarted(false);
        }

        @Override
        public void connect(MediaSink sink) {
            super.connect(sink);
        }

        /**
         * Asks the sink connected to this channel to determine preffred format
         * 
         * @param set
         *            the set of format to choose preffred from
         * @return the selected preffred format
         */
        public Format getOtherPartyPreffered(Collection<Format> set) {
            return isConnected() ? ((AbstractSink) otherParty).getPreffered(set) : null;
        }

        /**
         * (Non Java-doc).
         * 
         * @see org.mobicents.media.MediaSource#getFormats()
         */
        public Format[] getFormats() {
            return formats;
        }

        @Override
        public void evolve(Buffer buffer, long timestamp) {
            buffer.copy(buff);
        }
    }

    @Override
    public void evolve(Buffer buffer, long timestamp) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void destroySource(AbstractSource source) {
    }
}
