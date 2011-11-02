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
package org.mobicents.media.server.impl.resource.mediaplayer.video;

import java.io.IOException;
import java.net.URL;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.server.impl.resource.mediaplayer.Track;
import org.mobicents.media.server.impl.resource.mediaplayer.mpeg.MpegPresentation;
import org.mobicents.media.server.impl.resource.mediaplayer.mpeg.RTPLocalPacket;
import org.mobicents.media.server.impl.resource.mediaplayer.mpeg.RTPSample;
import org.mobicents.media.server.impl.resource.mediaplayer.mpeg.VideoTrack;

/**
 *
 * @author kulikov
 */
public class MpegVideoTrackImpl implements Track {

    private MpegPresentation presentation;
    private VideoTrack track;

    private RTPLocalPacket[] packets;
    private boolean eom = false;
    
    private int idx;
    private boolean isEmpty = true;
    private long duration;
    
    private long ssrc;
    
    public MpegVideoTrackImpl(URL url) throws IOException {
        presentation = new MpegPresentation(url);
        track = presentation.getVideoTrack();
    }
    
    public Format getFormat() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void process(Buffer buffer) throws IOException {
        if (isEmpty) {
            try {
                RTPSample rtpSample = track.process();
                if (rtpSample == null) {
                    buffer.setEOM(eom);
                    return;
                }
                packets = rtpSample.getRtpLocalPackets();
                duration = rtpSample.getSamplePeriod();
                if (packets.length == 0) {
                    buffer.setLength(0);
                    buffer.setDuration(duration);
                    return;
                }
                idx = 0;
                isEmpty = false;
            } catch (Exception e) {
                // TODO we have to rework this part
                throw new IllegalArgumentException(e);
            }
        }
        byte[] data = packets[idx++].toByteArray(this.ssrc);
        isEmpty = idx == packets.length;

        buffer.setData(data);
        buffer.setLength(data.length);
        buffer.setTimeStamp(0);
        buffer.setOffset(0);
        buffer.setSequenceNumber(0);
        buffer.setEOM(eom);
        buffer.setFlags(buffer.getFlags() | Buffer.FLAG_RTP_BINARY);
        buffer.setDuration(isEmpty ? duration : -1);
    }

    public void close() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getMediaTime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setMediaTime(long timestamp) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getDuration() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
