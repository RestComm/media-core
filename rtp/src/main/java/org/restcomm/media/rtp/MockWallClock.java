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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.restcomm.media.rtp;

import java.util.concurrent.TimeUnit;

import org.restcomm.media.core.scheduler.Clock;

/**
 *
 * @author kulikov
 */
public class MockWallClock implements Clock {

    private TimeUnit unit = TimeUnit.NANOSECONDS;
    private long time = System.nanoTime();
    private long currTime = System.currentTimeMillis();

    public long getTime() {
        return time;
    }

    public long getTime(TimeUnit timeUnit) {
        return timeUnit.convert(time, unit);
    }

    public TimeUnit getTimeUnit() {
        return unit;
    }

    public void tick(long amount) {
        time += amount;
        currTime += TimeUnit.NANOSECONDS.toMillis(amount);
    }
    
	public long getCurrentTime() {
		return currTime;
	}
}
