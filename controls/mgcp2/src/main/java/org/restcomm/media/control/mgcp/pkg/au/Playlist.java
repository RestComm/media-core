/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
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

package org.restcomm.media.control.mgcp.pkg.au;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class Playlist {

    private final String[] segments;
    private final int segmentCount;
    private final int iterations;
    private int index;
    private int counter;

    public Playlist(String[] segments, int iterations) {
        this.segments = segments;
        this.segmentCount = segments.length;
        this.iterations = iterations;
        this.index = -1;
        this.counter = iterations == -1 ? Integer.MAX_VALUE : iterations * segmentCount;
    }

    public String current() {
        return this.segments[this.index % this.segmentCount];
    }

    public boolean isOngoing() {
        return this.index != -1;
    }

    public boolean isEmpty() {
        return this.segmentCount == 0;
    }

    public String next() {
        if (isEmpty()) {
            return "";
        } else {
            this.counter--;
            this.index++;
            return this.counter == -1 ? "" : this.segments[this.index % this.segmentCount];
        }
    }
    
    public void rewind() {
        this.index = -1;
        this.counter = iterations == -1 ? Integer.MAX_VALUE : iterations * segmentCount;
    }
    
}
