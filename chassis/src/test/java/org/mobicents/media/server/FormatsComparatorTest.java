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

package org.mobicents.media.server;

import java.util.ArrayList;
import java.util.Collection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.spi.format.EncodingName;
import org.mobicents.media.server.spi.format.FormatFactory;

/**
 *
 * @author kulikov
 */
public class FormatsComparatorTest {

    private ArrayList<RTPFormat> fmts1 = new ArrayList();
    private ArrayList<RTPFormat> fmts2 = new ArrayList();

    private ArrayList<RTPFormat> res = new ArrayList();

    private FormatsComparator comparator = new FormatsComparator();

    public FormatsComparatorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        fmts1.add(new RTPFormat(1, FormatFactory.createAudioFormat(new EncodingName("a1"))));
        fmts1.add(new RTPFormat(2, FormatFactory.createAudioFormat(new EncodingName("a2"))));
        fmts1.add(new RTPFormat(3, FormatFactory.createAudioFormat(new EncodingName("a3"))));

        fmts2.add(new RTPFormat(2, FormatFactory.createAudioFormat(new EncodingName("a2"))));
        fmts2.add(new RTPFormat(3, FormatFactory.createAudioFormat(new EncodingName("a3"))));
        fmts2.add(new RTPFormat(4, FormatFactory.createAudioFormat(new EncodingName("a4"))));

        res.add(new RTPFormat(2, FormatFactory.createAudioFormat(new EncodingName("a2"))));
        res.add(new RTPFormat(3, FormatFactory.createAudioFormat(new EncodingName("a3"))));
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of intersection method, of class FormatsComparator.
     */
    @Test
    public void testIntersection() {
        Collection<RTPFormat> list = comparator.intersection(fmts1, fmts2);
        assertEquals(res.size(), list.size());
    }

}