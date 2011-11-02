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
package org.mobicents.media.server.impl.rtp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.resource.dtmf.DtmfEventImpl;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.rtp.AVProfile;

/**
 * 
 * @author kulikov
 */
public class SendStream extends AbstractSink {

    // sequence number
    private int seq = 0;
    // source synchronization
    private final long ssrc = System.currentTimeMillis();
    private RtpSocketImpl rtpSocket;
    protected RtpClock clock;
    private ArrayList<Format> formats = new ArrayList();
    protected byte mainstream;
    private byte dtmf;
    private long time;
    private Codec codec;
    private AVProfile formatConfig;
    private int eventDuration;
    private boolean endOfEvent;

    protected long byteCount;
    
    private SilenceDetector silenceDetector;
    
    public SendStream(RtpSocketImpl rtpSocket, AVProfile avProfile) {
        super("SendStream");
        this.rtpSocket = rtpSocket;
        this.formatConfig = avProfile;
        this.clock = rtpSocket.getClock();
        this.silenceDetector = new SilenceDetector();
    }

    @Override
    public void connect(MediaSource source) {
        if (this.rtpSocket.getFormat() == null) {
            throw new IllegalStateException("RTP has no negotiated formats");
        }
        super.connect(source);
    }

    public void setDtmf(int dtmf) {
        this.dtmf = (byte) dtmf;
    }
    
    @Override
    protected Format selectPreffered(Collection<Format> set) {
        for (Format f : set) {
            if (f.matches(rtpSocket.getFormat())) {
                codec = null;
                return f;
            }
        }

        for (Format f : set) {
            for (Codec c : rtpSocket.codecs) {
                if (f.matches(c.getSupportedInputFormat()) && 
                        c.getSupportedOutputFormat().matches(rtpSocket.getFormat())) {
                    codec = c;
                    return f;
                }
            }
        }

        //should never happen
        return null;
    }

    public void onMediaTransfer(Buffer buffer) throws IOException {
        //TODO this block must be removed
        //it does not depend from rtp clock
        RtpPacket packet = null;
        if (buffer.getFormat() == Format.RAW_RTP) {
            rtpSocket.send(buffer.getData());
            return;
        }
        
        //check for silence
        if (this.rtpSocket.isSilenceSuppressed() && silenceDetector.isSilenceDetected(buffer)) {
            //suppress silence.
            return;
        }
        
        if (codec != null) {
            codec.process(buffer);
        }
        boolean marker = (buffer.getFlags() & Buffer.FLAG_KEY_FRAME) == Buffer.FLAG_KEY_FRAME;

        int timestamp = (int) clock.getTimestamp(buffer.getTimeStamp());
        
        if (buffer.getHeader() != null && buffer.getHeader() instanceof DtmfEventImpl && dtmf > 0) {
            DtmfEventImpl evt = (DtmfEventImpl) buffer.getHeader();
            int digit = Integer.valueOf(evt.getTone());
            int volume = evt.getVolume();

            byte[] data = new byte[4];
            data[0] = (byte) digit;
            data[1] = endOfEvent ? (byte) (volume | 0x80) : (byte) (volume & 0x7f);

            data[2] = (byte) (eventDuration >> 8);
            data[3] = (byte) (eventDuration);

            eventDuration = eventDuration + 160;
            packet = new RtpPacket(false, dtmf, seq++, timestamp, ssrc, data, 0, 4); 
        } else {
            packet = new RtpPacket(marker, mainstream, seq++, timestamp, ssrc,
                    buffer.getData(), buffer.getOffset(), buffer.getLength());
        }
        
        byteCount += (buffer.getLength() + 12); 
        
        rtpSocket.send(packet);
        if (logger.isTraceEnabled()) {
            logger.trace("Sending " + packet);
        } 
    }

    /**
     * Resets this stream.
     */
    protected void reset() {
        mainstream = -1;
        byteCount = 0;
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.MediaSink.isAcceptable(Format).
     */
    public boolean isAcceptable(Format fmt) {
        return true;
    }

    /**
     * Configures supported formats.
     * 
     * @param payloadID the payload number of format used by rtp socket
     * @param format the format used by rtp socket.
     */
    protected void setFormat(int payloadID, Format format) {
        this.mainstream = (byte) payloadID;
        if (format != Format.ANY) {
            clock.setFormat(format);
        }

        //supported formats are combination of
        //specified format and possible transcodings
        formats.clear();
        formats.add(format);

        //looking for possible transcodings
        for (Codec c : rtpSocket.codecs) {
            if (c.getSupportedOutputFormat().matches(format)) {
                formats.add(c.getSupportedInputFormat());
            }
        }
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.MediaSink#getFormats() 
     */
    public Format[] getFormats() {
        //if RTP socket is not configured send stream can 
        //not send something
        if (rtpSocket.getFormat() == null) {
            return new Format[0];
        }
        //return supported formats
        Format[] fmts = new Format[formats.size()];
        formats.toArray(fmts);

        return fmts;
    }
}
