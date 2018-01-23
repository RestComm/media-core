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

package org.restcomm.media.resource.player.audio.tts;

import java.net.URL;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.resource.player.audio.tts.TtsTrackImpl;
import org.restcomm.media.resource.player.audio.tts.VoicesCache;

import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class TtsTrackImplTest {

    private TtsTrackImpl track;
    private VoicesCache voicesCache = new VoicesCache();
    public TtsTrackImplTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of setPeriod method, of class WavTrackImpl.
     */
    @Test
    public void testDurationd() throws Exception {
        /*URL url = TtsTrackImplTest.class.getClassLoader().getResource(
                "org/mobicents/media/server/impl/tts.txt");
        track = new TtsTrackImpl(url, "kevin16", voicesCache);
        assertEquals(1109, track.getDuration());*/
    }

}