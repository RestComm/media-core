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

package org.restcomm.media.core.control.mgcp.pkg.au;

import static org.junit.Assert.*;

import org.junit.Test;
import org.restcomm.media.core.control.mgcp.pkg.au.Playlist;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlaylistTest {

    @Test
    public void testPlaylistWithSingleIteration() {
        // given
        final String[] segments = new String[] { "a", "b", "c" };
        final int iterations = 1;
        final Playlist playlist = new Playlist(segments, iterations);

        // when
        String segmentA = playlist.next();
        String segmentB = playlist.next();
        String segmentC = playlist.next();
        String segmentD = playlist.next();

        // then
        assertEquals(segments[0], segmentA);
        assertEquals(segments[1], segmentB);
        assertEquals(segments[2], segmentC);
        assertEquals("", segmentD);
    }

    @Test
    public void testPlaylistWithMultipleIterations() {
        // given
        final String[] segments = new String[] { "a", "b" };
        final int iterations = 2;
        final Playlist playlist = new Playlist(segments, iterations);

        // when
        String segmentA = playlist.next();
        String segmentB = playlist.next();
        String segmentC = playlist.next();
        String segmentD = playlist.next();
        String segmentE = playlist.next();

        // then
        assertEquals(segments[0], segmentA);
        assertEquals(segments[1], segmentB);
        assertEquals(segments[0], segmentC);
        assertEquals(segments[1], segmentD);
        assertEquals("", segmentE);
    }
}
