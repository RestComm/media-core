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

import java.util.Collection;

import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.Outlet;
import org.mobicents.media.format.AudioFormat;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSinkSet;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;

/**
 * 
 * @author Oleg Kulikov
 */
public class AudioMixer extends AbstractSinkSet implements Outlet {

    protected final static AudioFormat LINEAR = new AudioFormat(
            AudioFormat.LINEAR, 8000, 16, 1,
            AudioFormat.LITTLE_ENDIAN,
            AudioFormat.SIGNED);
    protected final static Format[] formats = new Format[]{LINEAR};
    protected int packetSize;
    protected int packetPeriod = 20;
    protected MixerOutput mixerOutput;
    protected int channelCount;
    protected byte[][] frames;
    
    private Object header;

    /**
     * Creates a new instance of AudioMixer.
     * 
     * @param packetPeriod
     *            packetization period in milliseconds.
     * @param fmt
     *            format of the output stream.
     */
    public AudioMixer(String name) {
        super(name);
        mixerOutput = new MixerOutput(this);
        init();
    }

    /**
     * Initializes audio mixer.
     * 
     * @throws javax.media.format.UnsupportedFormatException
     */
    protected void init() {
        this.packetSize = 16 * packetPeriod;
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.Outlet#getOutput(). 
     */
    public MediaSource getOutput() {
        return mixerOutput;
    }

    @Override
    public void start() {
        mixerOutput.start();
    }

    @Override
    public void stop() {
        mixerOutput.stop();
    }

    @Override
    public void setEndpoint(Endpoint endpoint) {
        super.setEndpoint(endpoint);
        mixerOutput.setEndpoint(endpoint);
        Collection<AbstractSink> streams = getStreams();
        for (AbstractSink stream : streams) {
            stream.setEndpoint(endpoint);
        }
    }

    @Override
    public void setConnection(Connection connection) {
        super.setConnection(connection);
        mixerOutput.setConnection(connection);
        Collection<AbstractSink> streams = getStreams();
        for (AbstractSink stream : streams) {
            stream.setConnection(connection);
        }
    }

    /**
     * Converts inner byte representation of the signal into 
     * 16bit per sample array
     * 
     * @param input the array where sample takes two elements.
     * @return array where sample takes one element.
     */
    public short[] byteToShortArray(byte[] input) {
        short[] output = new short[input.length >> 1];
        for (int q = 0; q < input.length; q += 2) {
            short f = (short) (((input[q + 1]) << 8) | (input[q] & 0xff));
            output[q >> 1] = f;
        }
        return output;
    }

    /**
     * Mixes input packets.
     * 
     * @param input collection of arras of samples of same length
     * @return array of result array of samples.
     */
    public byte[] mix(byte[][] frames) {
        int numSamples = packetSize >> 1;

        byte[] data = new byte[packetSize];
        int k = 0;

        for (int j = 0; j < packetSize; j += 2) {
            short s = 0;
            for (int i = 0; i < channelCount; i++) {
                s += (short) (((frames[i][j + 1]) << 8) | (frames[i][j] & 0xff));
            }
            data[k++] = (byte) (s);
            data[k++] = (byte) (s >> 8);
        }

        return data;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.impl.AbstractSource#evolve(org.mobicents.media.Buffer, long). 
     */
    public void evolve(Buffer buffer, long timestamp) {
        //clean header
        this.header = null;
        channelCount = this.getStreams().size();
        int i = 0;
        frames = new byte[channelCount][320];
        Collection<AbstractSink> streams = getStreams();
        for (AbstractSink stream : streams) {
            MixerInputStream input = (MixerInputStream) stream;
            frames[i++] = input.read(packetPeriod);
            if (input.header != null) {
                this.header = input.header;
            }
        }

        byte[] data = mix(frames);
        buffer.setHeader(header);
        buffer.setData(data);
        buffer.setOffset(0);
        buffer.setLength(data.length);
        buffer.setTimeStamp(timestamp);
        buffer.setDuration(20);
        buffer.setFormat(LINEAR);
        buffer.setEOM(false);
        buffer.setDiscard(false);
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.MediaSink#isAcceptable(org.mobicents.media.Format) 
     */
    public boolean isAcceptable(Format fmt) {
        return fmt.matches(LINEAR);
    }

    /**
     * (Non Java-doc.)
     * @see org.mobicents.media.server.impl.AbstractSink#onMediaTransfer(org.mobicents.media.Buffer) 
     */
    public void onMediaTransfer(Buffer buffer) {
        throw new UnsupportedOperationException();
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.MediaSink#getFormats() 
     */
    public Format[] getFormats() {
        return formats;
    }

    public void connect(MediaSink sink) {
        getOutput().connect(sink);
    }

    public void disconnect(MediaSink sink) {
        getOutput().disconnect(sink);
    }

    @Override
    public AbstractSink createSink(MediaSource otherParty) {
        MixerInputStream input = new MixerInputStream(this);
        return input;
    }

    @Override
    public void destroySink(AbstractSink sink) {
        ((MixerInputStream) sink).mixer = null;
    }
}
