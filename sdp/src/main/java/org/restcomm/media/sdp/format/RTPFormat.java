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

package org.restcomm.media.sdp.format;

import org.restcomm.media.spi.format.Format;

/**
 * RTP Format descriptor.
 *
 * @author kulikov
 */
public class RTPFormat implements Cloneable {
    //payload id
    private int id;
    //format descriptor
    private Format format;

    //RTP clock rate measured in Hertz.
    private int clockRate;

    /**
     * Creates new format descriptor.
     *
     * @param id the payload number
     * @param format format descriptor
     */
    public RTPFormat(int id, Format format) {
        this.id = id;
        this.format = format;
    }

    /**
     * Creates new descriptor.
     * 
     * @param id payload number
     * @param format formats descriptor
     * @param clockRate RTP clock rate
     */
    public RTPFormat(int id, Format format, int clockRate) {
        this.id = id;
        this.format = format;
        this.clockRate = clockRate;
    }

    /**
     * Gets the payload number
     *
     * @return payload number
     */
    public int getID() {
        return id;
    }

    /**
     * Modifies payload number.
     *
     * @param id the new payload number.
     */
    protected void setID(int id) {
        this.id = id;
    }

    /**
     * Gets the rtp clock rate.
     * 
     * @return the rtp clock rate in Hertz
     */
    public int getClockRate() {
        return clockRate;
    }

    /**
     * Modify rtp clock rate.
     *
     * @param clockRate the new value in Hertz.
     */
    public void setClockRate(int clockRate) {
        this.clockRate = clockRate;
    }

    /**
     * Gets format.
     *
     * @return format descriptor.
     */
    public Format getFormat() {
        return format;
    }

    /**
     * Modifies format.
     *
     * @param format the new format descriptor.
     */
    public void setFormat(Format format) {
        this.format = format;
    }

    @Override
    public RTPFormat clone() {
        Format f = (Format) format.clone();
        return new RTPFormat(id, f, clockRate);
    }
    
    @Override
    public String toString() {
        return id + " " + format;
    }
}
