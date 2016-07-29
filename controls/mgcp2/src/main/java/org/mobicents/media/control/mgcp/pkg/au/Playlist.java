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

package org.mobicents.media.control.mgcp.pkg.au;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class Playlist {

    private final String[] segments;
    private final int segmentCount;
    private int index;
    private int counter;

    public Playlist(String[] segments, int iterations) {
        this.segments = segments;
        this.segmentCount = segments.length;
        this.index = 0;
        this.counter = iterations == -1 ? Integer.MAX_VALUE : iterations * segmentCount;
    }

    public String current() {
        return this.segments[this.index++ % this.segmentCount];
    }

    public String next() {
        this.counter--;
        return this.counter == -1 ? "" : this.segments[this.index++ % this.segmentCount];
    }
}
