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

package org.restcomm.media.rtp;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.scheduler.Clock;

/**
 * Provides time conversation between RTP time and media time.
 *
 * @author kulikov
 */
public class RtpClock {
	
	public static final Logger logger = LogManager.getLogger(RtpClock.class);
	
    //absolute time clock
    private Clock wallClock;

    //the clock rate measured in Hertz.
    private int clockRate;
    private int scale;

    //the difference between media time measured by local and remote clock
    protected long drift;

    //the flag indicating the state of relation between local and remote clocks
    //the flag value is true if relation established
    private boolean isSynchronized;

    /**
     * Creates new instance of clock.
     *
     * @param absolute time clock.
     */
    public RtpClock(Clock wallClock) {
        this.wallClock = wallClock;
    }
    
    public Clock getWallClock() {
		return wallClock;
	}

    /**
     * Modifies clock rate.
     *
     * @param clockRate the new value of clock rate in Hertz.
     */
    public void setClockRate(int clockRate) {
        this.clockRate = clockRate;
        this.scale = clockRate/1000;
    }

    /**
     * Gets the clock rate.
     *
     * @return the value in Hertz
     */
    public int getClockRate() {
        return clockRate;
    }

    /**
     * Synchronizes this clock with remote clock
     *
     * @param remote the time on remote clock.
     */
    public void synchronize(long remote) {
        this.drift = remote - getLocalRtpTime();
        this.isSynchronized = true;
    }

    /**
     * The state of the relation between remote and local clock.
     *
     * @return true if time is same on both clocks.
     */
    public boolean isSynchronized() {
        return this.isSynchronized;
    }
    
    /**
     * Resets clocks.
     */
    public void reset() {
        this.drift = 0;
        this.clockRate = 0;
        this.isSynchronized = false;
    }

    /**
     * Time in RTP timestamps.
     * @return
     */
    public long getLocalRtpTime() {
        return scale * wallClock.getTime(TimeUnit.MILLISECONDS) + drift;
    }
    
    /**
     * Returns the time in milliseconds
     * 
     * @param timestamp the rtp timestamp
     * @return the time in milliseconds
     */
    public long convertToAbsoluteTime(long timestamp) {
        return timestamp * 1000 / clockRate;
    }
    
    /**
     * Calculates RTP timestamp
     * 
     * @param time the time in milliseconds
     * @return rtp timestamp.
     */
    public long convertToRtpTime(long time) {
    	return time * clockRate / 1000;
    }

}
