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

package org.restcomm.media.network.deprecated;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link PortManager} that helps to acquire an even port for an RTP channel. The odd port will be reserved for RTCP.
 * 
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class RtpPortManager implements PortManager {

    public static final int MIN_PORT = 1024;
    public static final int MAX_PORT = 65534;

    private final int minimum;
    private final int maximum;
    private final int step;
    private final AtomicInteger current;

    /**
     * Creates a new Port Manager.
     * 
     * @param minimum The lowest available port.
     * @param maximum The highest available port.
     */
    public RtpPortManager(int minimum, int maximum) {
        this.minimum = (minimum % 2 == 0) ? minimum : minimum + 1;
        this.maximum = (maximum % 2 == 0) ? maximum : maximum - 1;
        this.step = (this.maximum - this.minimum) / 2;
        this.current = new AtomicInteger(0);
    }

    /**
     * Create a new Port Manager with port range between {@link RtpPortManager#MIN_PORT} and {@link RtpPortManager#MAX_PORT}
     */
    public RtpPortManager() {
        this(MIN_PORT, MAX_PORT);
    }

    @Override
    public int getLowest() {
        return this.minimum;
    }

    @Override
    public int getHighest() {
        return this.maximum;
    }

    @Override
    public int next() {
        return this.maximum - (this.current.getAndAdd(1) % step) * 2;
    }

    public int peek() {
        return this.maximum - ((this.current.get() + 1) % step) * 2;
    }

    public int current() {
        return this.maximum - (this.current.get() % step) * 2;
    }

}
