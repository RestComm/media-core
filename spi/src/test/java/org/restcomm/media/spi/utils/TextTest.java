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

package org.restcomm.media.spi.utils;

import java.util.Iterator;
import java.util.Collection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.spi.utils.Text;

import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class TextTest {

    public TextTest() {
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
     * Test of strain method, of class Text.
     */
    @Test
    public void testStrain() {
        byte[] data = "One two three".getBytes();
        Text t = new Text();
        t.strain(data, 4, 3);
        assertEquals("two", t.toString());
    }

    /**
     * Test of length method, of class Text.
     */
    @Test
    public void testLength() {
        byte[] data = "One two three".getBytes();
        Text t = new Text();
        t.strain(data, 0, data.length);
        assertEquals(data.length, t.length());
    }

    /**
     * Test of charAt method, of class Text.
     */
    @Test
    public void testCharAt() {
        String s = "One two three";
        byte[] data = s.getBytes();
        Text t = new Text();
        t.strain(data, 0, data.length);

        for (int i = 0; i < data.length; i++) {
            assertEquals(s.charAt(i), t.charAt(i));
        }
    }

    @Test
    public void testCharAtWithShifting() {
        String s = "12345 One two three";
        byte[] data = s.getBytes();
        Text t = new Text();
        t.strain(data, 6, data.length -6);

        assertEquals('O', t.charAt(0));
    }

    /**
     * Test of subSequence method, of class Text.
     */
    @Test
    public void testSubSequence() {
        String s = "One two three";
        byte[] data = s.getBytes();

        Text t = new Text();
        t.strain(data, 0, data.length);

        Text t2 = new Text();
        t2.strain(data, 4, 3);
        
        CharSequence t1 = t.subSequence(4, 7);

        for (int i = 0; i < t1.length(); i++) {
            assertEquals(t2.charAt(i), t1.charAt(i));
        }
    }

    /**
     * Test of equals method, of class Text.
     */
    @Test
    public void testEquals() {
        Text t1 = new Text("test");
        Text t2 = new Text("test");
        assertTrue("Must be equals", t1.equals(t2));
    }

    @Test
    public void testEqualsDifferentBackground() {
        Text t1 = new Text("test");

        String s = "12345678test";

        Text t2 = new Text();
        t2.strain(s.getBytes(), 8, 4);

        boolean res = t1.equals(t2);

        assertTrue("Must be equals", res);
    }

    @Test
    public void testEqualsWithDifferentCase() {
        Text t1 = new Text("test");
        Text t2 = new Text("Test");
        assertTrue("Must be equals", t1.equals(t2));
    }

    @Test
    public void testSplit() {
        Text t1 = new Text("test test1 test2");

        Collection<Text> tokens = t1.split(' ');
        assertEquals(3, tokens.size());

        Iterator<Text> i = tokens.iterator();

        Text t = i.next();
        assertEquals("test", t.toString());

        t = i.next();
        assertEquals("test1", t.toString());

        t = i.next();
        assertEquals("test2", t.toString());
    }

    @Test
    public void testSplit2() {
        Text t1 = new Text("test");

        Collection<Text> tokens = t1.split(' ');
        assertEquals(1, tokens.size());

        Iterator<Text> i = tokens.iterator();

        Text t = i.next();
        assertEquals("test", t.toString());
    }
    
    @Test
    public void testDivide() {
        Text t1 = new Text("test test1 test2");

        Text t2 = new Text();
        Text t3 = new Text();
        Text t4 = new Text();
        
        int count = t1.divide(' ', new Text[] {t2,t3,t4});
        
        assertEquals(3, count);

        assertEquals("test", t2.toString());

        assertEquals("test1", t3.toString());

        assertEquals("test2", t4.toString());
    }
    
    @Test
    public void testSplitExecutionTime() {
        Text t1 = new Text("test test1 test2");

        long s = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            Collection<Text> tokens = t1.split(' ');
        }

        long f = System.nanoTime();
        System.out.println("Split execution time=" + (f-s));
        assertTrue("Too slow", 15000000 > (f-s));
    }

    @Test
    public void testTrim() {
        Text t1 = new Text(" test ");
        t1.trim();
        assertEquals("test", t1.toString());
    }

    @Test
    public void testToInteger() {
        Text t1 = new Text("1234");
        assertEquals(1234, t1.toInteger());
        
        Text t2 = new Text("8000\r");
        t2.trim();
        assertEquals(8000, t2.toInteger());
    }
    
    @Test
    public void testLineSplit() {
        Text t1 = new Text("test\ntest1\ntest2");

        Text t = t1.nextLine();
        assertEquals("test", t.toString());

        t = t1.nextLine();
        assertEquals("test1", t.toString());

        t = t1.nextLine();
        assertEquals("test2", t.toString());
    }

    @Test
    public void testCopy() {
        Text t1 = new Text("hello");

        Text t = new Text();
        t1.copy(t);
        
        assertEquals("hello", t.toString());
        
        byte[] data = "world".getBytes();
        t1.strain(data, 0, data.length);
        
        assertEquals("hello", t.toString());
    }
    
    @Test
    public void testMoreLines() {
        Text t1 = new Text("test\ntest1\ntest2");

        assertTrue(t1.hasMoreLines());
        Text t = t1.nextLine();
        assertEquals("test", t.toString());

        t = t1.nextLine();
        assertEquals("test1", t.toString());

        t = t1.nextLine();
        assertEquals("test2", t.toString());

        assertFalse(t1.hasMoreLines());
    }
    
    @Test
    public void perfTests() {
        byte[] data = "name1".getBytes();
        long s = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            Text t = new Text();
            t.strain(data, 0, data.length);
        }
        long f = System.nanoTime();
        System.out.println("Create/straing execution time=" + (f-s));
    }

    @Test
    public void testCreateFromStringTime() {
        long s = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            Text t = new Text("name1");
        }
        long f = System.nanoTime();
        System.out.println("Create/straing execution time=" + (f-s));
    }

    @Test
    public void testConstructorWithInteger() {
        long s = System.nanoTime();
        Text t = new Text(123);
        long f = System.nanoTime();
        
        System.out.println("==== Duration=" + (f-s));
        assertEquals("123", t.toString());
        
        s = System.nanoTime();
        t = new Text(1234);
        f = System.nanoTime();
        
        System.out.println("==== Duration=" + (f-s));
        assertEquals("1234", t.toString());
        
        s = System.nanoTime();
        String ss = Integer.toString(123);
        f = System.nanoTime();
        System.out.println("==== Duration=" + (f-s));
        
    }
    
    @Test
    public void testCopyRemainder() {
        Text text = new Text("one\ntwo\nthree");
        Text other = new Text();
        
        text.hasMoreLines();
        text.nextLine();
        
        text.copyRemainder(other);
        
        assertEquals("two\nthree", other.toString());
    }
    
    @Test
    public void testContains() {
        Text text = new Text("one$two");
        assertTrue("Text contains $", text.contains('$'));
        assertFalse("Text does not contains *", text.contains('*'));
    
    }
    
    @Test
    public void testDuplicate() {
        Text t1 = new Text("Text");
        Text t2 = new Text(new byte[10], 0, 10);
        
        t1.duplicate(t2);
        assertEquals("Text", t2.toString());
    }
    
    @Test
    public void testDivide2() {
        Text t = new Text("oc(N)");
        
        Text t1 = new Text();
        Text t2 = new Text();
        
        char[] brackets = new char[]{'(', ')'};
        Text[] parts = new Text[]{t1, t2};
        
        t.divide(brackets, parts);
        
        assertEquals("oc", t1.toString());
        assertEquals("N", t2.toString());
    }
    
    @Test
    public void testIntegerConstructor() {
        Text t = new Text(10);
        System.out.println(t.toString());
        assertEquals("10", t.toString());
    }
    
    @Test
    public void testLinesAfterReuse() {
        byte[] buff = new byte[]{(byte)0x32, (byte)0x30, (byte)0x30, (byte)0x20, (byte)0x31, (byte)0x30, 
            (byte)0x20, (byte)0x53, (byte)0x75, (byte)0x63, (byte)0x63, (byte)0x65, (byte)0x73, (byte)0x73, (byte)0x0a};
        Text t = new Text();
        
        int count = 0;
        t.strain(buff, 0, buff.length);
        while (t.hasMoreLines()) {
            Text tl = t.nextLine();
            count++;
        }
        
        assertEquals(2, count);

        count = 0;
        t.strain(buff, 0, buff.length);
        while (t.hasMoreLines()) {
            Text tl = t.nextLine();
            count++;
        }
        
        assertEquals(2, count);
        
    }
}