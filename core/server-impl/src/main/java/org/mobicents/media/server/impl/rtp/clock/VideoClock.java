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

package org.mobicents.media.server.impl.rtp.clock;

import org.mobicents.media.Format;
import org.mobicents.media.format.VideoFormat;
import org.mobicents.media.server.impl.rtp.RtpClock;

/**
 *
 * @author kulikov
 */
public class VideoClock extends RtpClock {

    private double rtpUnit;
    private double frameDuration;
    
    @Override
    public void setFormat(Format format) {
        super.setFormat(format);
        VideoFormat fmt = (VideoFormat) format;
        frameDuration = 1000 / fmt.getFrameRate();
        rtpUnit = (int)(fmt.getClockRate()/fmt.getFrameRate());
    }
    
    @Override
    public long getTime(long timestamp) {
        return (long) (timestamp/rtpUnit * frameDuration);
    }

    @Override
    public long getTimestamp(long time) {
        return (long) (time/frameDuration * rtpUnit);
    }

}
