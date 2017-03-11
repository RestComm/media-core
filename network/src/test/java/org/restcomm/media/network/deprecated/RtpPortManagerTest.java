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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.restcomm.media.network.deprecated.PortManager;
import org.restcomm.media.network.deprecated.RtpPortManager;

/**
 *
 * @author kulikov
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class RtpPortManagerTest {

    @Test
    public void testEvenLowestPort() {
        // given
        final int minimum = 2;
        final int maximum = 10;

        // when
        final PortManager portManager = new RtpPortManager(minimum, maximum);

        // then
        assertEquals(minimum, portManager.getLowest());
    }

    @Test
    public void testOddLowestPort() {
        // given
        final int minimum = 1;
        final int minimumEven = 2;
        final int maximum = 10;

        // when
        final PortManager portManager = new RtpPortManager(minimum, maximum);

        // then
        assertEquals(minimumEven, portManager.getLowest());
    }

    @Test
    public void testEvenHighestPort() {
        // given
        final int minimum = 2;
        final int maximum = 10;

        // when
        final PortManager portManager = new RtpPortManager(minimum, maximum);

        // then
        assertEquals(maximum, portManager.getHighest());
    }

    @Test
    public void testOddHighestPort() {
        // given
        final int minimum = 2;
        final int maximum = 11;
        final int maximumEven = 10;

        // when
        final PortManager portManager = new RtpPortManager(minimum, maximum);

        // then
        assertEquals(maximumEven, portManager.getHighest());
    }

    @Test
    public void testNext() {
        // given
        final int minimum = 2;
        final int maximum = 10;

        // when
        final PortManager portManager = new RtpPortManager(minimum, maximum);

        // then
        assertEquals(10, portManager.next());
        assertEquals(8, portManager.next());
        assertEquals(6, portManager.next());
        assertEquals(4, portManager.next());
        assertEquals(10, portManager.next());
    }

}