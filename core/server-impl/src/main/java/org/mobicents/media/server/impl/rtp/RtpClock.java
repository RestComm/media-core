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

import org.mobicents.media.Format;

/**
 *
 * @author kulikov
 */
public abstract class RtpClock {
    private Format format;
    
    protected long now;
    private boolean isSynchronized;
    
    public RtpClock() {
    }
    
    public Format getFormat() {
        return format;
    }
    
    public void setFormat(Format format) {
        this.format = format;
    }
    
    public void synchronize(long initial) {
        now = initial;
        this.isSynchronized = true;
    }
    
    public boolean isSynchronized() {
        return this.isSynchronized();
    }
    
    protected long now() {
        return now;
    }
    
    public void reset() {
        now = 0;
        this.isSynchronized = false;
        this.format = null;
    }
    
    /**
     * Returns the time in milliseconds
     * 
     * @param timestamp the rtp timestamp
     * @return the time in milliseconds
     */
    public abstract long getTime(long timestamp);
    
    /**
     * Calculates RTP timestamp
     * 
     * @param time the time in milliseconds
     * @return rtp timestamp.
     */
    public abstract long getTimestamp(long time);
}
