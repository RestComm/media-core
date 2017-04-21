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

package org.restcomm.media.rtp.jitter.adaptive.strategy;

import org.restcomm.media.rtp.RtpPacket;

/**
 * Strategy interface for finding the playout time for received rtp packets, adapting to network variability i.e. jitter,
 * spikes, etc.
 * 
 * @author jqayyum
 *
 */
public interface PlayoutStrategy {
    /**
     * get the playout offset (msec) of input packet
     * 
     * @param RtpPacket received
     * @return Playout offset time in msec for the input packet. -1 means no specific time e.g. may be consumed automatically
     *         etc.
     */
    public long getPlayoutOffset(RtpPacket p);
}
