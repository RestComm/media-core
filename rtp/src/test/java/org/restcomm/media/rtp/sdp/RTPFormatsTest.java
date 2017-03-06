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

package org.restcomm.media.rtp.sdp;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.EncodingName;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.restcomm.media.sdp.format.AVProfile;
import org.restcomm.media.sdp.format.RTPFormat;
import org.restcomm.media.sdp.format.RTPFormats;

/**
 *
 * @author kulikov
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class RTPFormatsTest {

    private RTPFormats formats = new RTPFormats();

    private Format fmt1 = FormatFactory.createAudioFormat(new EncodingName("test1"));
    private Format fmt2 = FormatFactory.createAudioFormat(new EncodingName("test2"));
    private Format fmt3 = FormatFactory.createAudioFormat(new EncodingName("test3"));
    private Format fmt4 = FormatFactory.createAudioFormat(new EncodingName("test4"));

    private RTPFormat f1 = new RTPFormat(1, fmt1);
    private RTPFormat f2 = new RTPFormat(2, fmt2);
    private RTPFormat f3 = new RTPFormat(3, fmt3);
    private RTPFormat f4 = new RTPFormat(1, fmt4);

    /**
     * Test of add method, of class RTPFormats.
     */
    @Test
    public void testAdd() {
        formats.add(f1);
        assertEquals(1, formats.size());
        formats.add(f2);
        assertEquals(2, formats.size());
    }

    /**
     * Test of remove method, of class RTPFormats.
     */
    @Test
    public void testRemove() {
        formats.add(f1);
        formats.add(f2);

        formats.remove(f1);
        assertEquals(1, formats.size());
        formats.remove(f2);
        assertEquals(0, formats.size());
    }

    /**
     * Test of clean method, of class RTPFormats.
     */
    @Test
    public void testClean() {
        formats.add(f1);
        formats.add(f2);
        assertEquals(2, formats.size());

        formats.clean();
        assertEquals(0, formats.size());
    }

    /**
     * Test of getRTPFormat method, of class RTPFormats.
     */
    @Test
    public void testGetRTPFormat() {
        formats.add(f1);
        formats.add(f2);

        assertEquals("test1", formats.getRTPFormat(1).getFormat().getName().toString());
        assertEquals("test2", formats.getRTPFormat(2).getFormat().getName().toString());

        formats.add(f3);
        assertEquals("test1", formats.getRTPFormat(1).getFormat().getName().toString());
        assertEquals("test2", formats.getRTPFormat(2).getFormat().getName().toString());
        assertEquals("test3", formats.getRTPFormat(3).getFormat().getName().toString());

        formats.remove(f1);
        assertEquals(null, formats.getRTPFormat(1));
        assertEquals("test2", formats.getRTPFormat(2).getFormat().getName().toString());
        assertEquals("test3", formats.getRTPFormat(3).getFormat().getName().toString());
    }

    @Test
    public void testFind() {
        formats.add(f1);
        formats.add(f2);
        
        RTPFormat f = formats.find(1);
        assertTrue(f1.getID() == f.getID());
    }
    
    @Test
    public void testTravers() {
        formats.add(f1);
        formats.add(f2);
        
        assertTrue("Two objects expected", formats.hasMore());
        
        formats.next();
        assertTrue("One remains", formats.hasMore());
        
        formats.next();
        assertFalse("Nothing remains", formats.hasMore());
    }
    
    @Test
    public void testTraversAfterRewind() {
        formats.add(f1);
        formats.add(f2);
        
        formats.next();
        formats.next();
        assertFalse("Nothing remains", formats.hasMore());
        
        formats.rewind();
        assertTrue("Two objects expected", formats.hasMore());
        
        formats.next();
        assertTrue("One remains", formats.hasMore());
        
        formats.next();
        assertFalse("Nothing remains", formats.hasMore());
    }

    @Test
    public void testTraversAfterClear() {
        formats.add(f1);
        formats.add(f2);
        
        formats.clean();
        assertFalse("Nothing remains", formats.hasMore());
        
        formats.add(f1);
        formats.add(f2);
        
        assertTrue("Two objects expected", formats.hasMore());
        
        formats.next();
        assertTrue("One remains", formats.hasMore());
        
        formats.next();
        assertFalse("Nothing remains", formats.hasMore());
    }
    
    @Test
    public void testEmpty() {
        assertTrue("Formats empty now", formats.isEmpty());

        formats.add(f1);
        assertFalse("Formats not empty now", formats.isEmpty());
        
        formats.clean();
        assertTrue("Formats empty now", formats.isEmpty());
        
    }
    
    @Test
    public void testIntersection() {
        RTPFormats other = new RTPFormats();
        RTPFormats res = new RTPFormats();
        
        formats.add(f1);
        formats.add(f2);
        
        other.add(f2);
        other.add(f3);
        other.add(f4);
        
        formats.intersection(other, res);
        
        assertEquals(1, res.size());
        
        res.rewind();
        RTPFormat f = res.next();
        
        assertTrue("Wrong format", f2.getFormat().matches(f.getFormat()));
    }
    
    @Test
    public void testFormatIntersectionWithDynamicNegotiation() {
    	// given
    	AudioFormat telephoneEvent126 = FormatFactory.createAudioFormat("telephone-event", 8000);
    	RTPFormat dtmf126 = new RTPFormat(126, telephoneEvent126, 8000);
    	
    	RTPFormats supported = new RTPFormats();
    	supported.add(AVProfile.audio.find(0));
    	supported.add(AVProfile.audio.find(8));
    	supported.add(AVProfile.audio.find(97));
    	supported.add(AVProfile.audio.find(101));

    	RTPFormats offered = new RTPFormats();
    	offered.add(AVProfile.audio.find(0));
    	offered.add(AVProfile.audio.find(97));
    	offered.add(dtmf126);
    	
    	// when
    	RTPFormats negotiated = new RTPFormats();
    	supported.intersection(offered, negotiated);
    	negotiated.rewind();
    	
    	// then
    	assertEquals(3, negotiated.size());
    	assertNotNull(negotiated.find(126));
    	assertNull(negotiated.find(101));
    }
    
}