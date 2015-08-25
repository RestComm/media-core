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

package org.mobicents.media.server.io.network;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The utility class that helps to acquire port for conversation.
 *
 * The range of available port is identified by a pair of integer constants. Method <code>next</code> consequently peeks up even
 * port either from beginning or from the end of range.
 *
 * @author yulian oifa
 */
public class PortManager {

    private static final int DEFAULT_LOW_PORT = 1024;
    private static final int DEFAULT_HIGH_PORT = 65534;

    private int lowestPort;
    private int highestPort;
    private int step;

    private AtomicInteger currPort = new AtomicInteger(0);

    public PortManager(int lowestPort, int highestPort) {
        this.lowestPort = lowestPort;
        this.highestPort = highestPort;
        this.step = (highestPort - lowestPort) / 2;
    }

    public PortManager() {
        this(DEFAULT_LOW_PORT, DEFAULT_HIGH_PORT);
    }

    /**
     * Modify the low boundary.
     * 
     * @param low port number
     */
    public void setLowestPort(int low) {
        this.lowestPort = (low % 2 == 0) ? low : low + 1;
        this.step = (this.highestPort - low) / 2;
    }

    /**
     * Gets the low boundary of available range.
     * 
     * @return low min port number
     */
    public int getLowestPort() {
        return lowestPort;
    }

    /**
     * Modify the upper boundary.
     * 
     * @param high port number
     */
    public void setHighestPort(int high) {
        this.highestPort = high % 2 == 0 ? high : high - 1;
        this.step = (high - this.lowestPort) / 2 + 1;
    }

    /**
     * Gets the upper boundary of available range.
     * 
     * @retun min port number
     */
    public int getHighestPort() {
        return this.highestPort;
    }

    /**
     * Select the next port probably available.
     *
     * @return even port number
     */
    public int next() {
        return highestPort - (currPort.getAndAdd(1) % step) * 2;
    }

    public int peek() {
        return highestPort - ((currPort.get() + 1) % step) * 2;
    }

    public int current() {
        return highestPort - (currPort.get() % step) * 2;
    }

}
