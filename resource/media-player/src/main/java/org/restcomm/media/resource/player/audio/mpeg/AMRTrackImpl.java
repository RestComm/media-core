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

package org.restcomm.media.resource.player.audio.mpeg;

import java.io.IOException;
import java.net.URL;

import org.restcomm.media.resource.player.Track;
import org.restcomm.media.resource.player.video.mpeg.AudioTrack;
import org.restcomm.media.resource.player.video.mpeg.MpegPresentation;
import org.restcomm.media.resource.player.video.mpeg.RTPLocalPacket;
import org.restcomm.media.resource.player.video.mpeg.RTPSample;
import org.restcomm.media.spi.format.Format;
import org.restcomm.media.spi.memory.Frame;

/**
 * 
 * @author kulikov
 */
public class AMRTrackImpl implements Track {

    // mpeg presentation
    private MpegPresentation presentation;
    private AudioTrack track;
    private RTPLocalPacket[] packets;
    private boolean eom = false;
    private int idx;
    private boolean isEmpty = true;
    private long duration;
    private long ssrc;

    public AMRTrackImpl(URL url) throws IOException {
        presentation = new MpegPresentation(url);
        track = presentation.getAudioTrack();
    }

    public Format getFormat() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Frame process(long timestamp) throws IOException {
        return null;
/*        if (isEmpty) {
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
 *
 */
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
