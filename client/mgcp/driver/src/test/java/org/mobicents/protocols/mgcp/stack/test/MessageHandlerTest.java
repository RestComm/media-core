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

package org.mobicents.protocols.mgcp.stack.test;

import junit.framework.TestCase;
import java.util.ArrayList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;
import org.mobicents.protocols.mgcp.handlers.MessageHandler;

import org.mobicents.protocols.mgcp.parser.StringFunctions;
import org.mobicents.protocols.mgcp.parser.SplitDetails;

public class MessageHandlerTest extends TestCase {
	private MessageHandler handler = null;
	private JainMgcpStackImpl stack = null;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		stack = new JainMgcpStackImpl();
		handler = new MessageHandler(stack);
	}

	@After
	public void tearDown() {
	}

	@Test
	public void testPiggyDismount() throws Exception {
		String message = "200 2005 OK\n.\nDLCX 1244 card23/21@tgw-7.example.net MGCP 1.0\nC: A3C47F21456789F0\nI: FDE234C8";
		byte[] rawByte = message.getBytes();
		ArrayList<SplitDetails[]> result=StringFunctions.splitLinesWithTrim(rawByte,0,rawByte.length);		
		
		assertEquals("200 2005 OK", new String(rawByte,result.get(0)[0].getOffset(),result.get(0)[0].getLength()));
		
		String firstLine=new String(rawByte,result.get(1)[0].getOffset(),result.get(1)[0].getLength());
		String secondLine=new String(rawByte,result.get(1)[1].getOffset(),result.get(1)[1].getLength());
		String lastLine=new String(rawByte,result.get(1)[result.get(1).length-1].getOffset(),result.get(1)[result.get(1).length-1].getLength());
		
		boolean flag = firstLine.startsWith("DLCX") && lastLine.endsWith("FDE234C8");
		assertTrue(flag);
		
		assertEquals("DLCX 1244 card23/21@tgw-7.example.net MGCP 1.0",firstLine);
		assertEquals("C: A3C47F21456789F0",secondLine);
		assertEquals("I: FDE234C8",lastLine);
	}

	@Test
	public void testNoPiggyDismount() throws Exception {
		String message = "DLCX 1244 card23/21@tgw-7.example.net MGCP 1.0\nC: A3C47F21456789F0\nI: FDE234C8\n";
		byte[] rawByte = message.getBytes();
		ArrayList<SplitDetails[]> result=StringFunctions.splitLinesWithTrim(rawByte,0,rawByte.length);
		
		assertEquals(1, result.size());
		String firstLine=new String(rawByte,result.get(0)[0].getOffset(),result.get(0)[0].getLength());
		String lastLine=new String(rawByte,result.get(0)[result.get(0).length-1].getOffset(),result.get(0)[result.get(0).length-1].getLength());
		boolean flag = firstLine.startsWith("DLCX") && lastLine.endsWith("FDE234C8");
		assertTrue(flag);
	}
}