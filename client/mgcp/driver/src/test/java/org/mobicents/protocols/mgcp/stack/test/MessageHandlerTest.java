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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.protocols.mgcp.parser.UtilsFactory;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;
import org.mobicents.protocols.mgcp.stack.MessageHandler;

public class MessageHandlerTest extends TestCase {
	private MessageHandler handler = null;
	private JainMgcpStackImpl stack = null;
	private UtilsFactory factory = null;

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		factory = new UtilsFactory(1);
		stack = new JainMgcpStackImpl();
		stack.setUtilsFactory(factory);
		handler = new MessageHandler(stack);
	}

	@After
	public void tearDown() {
	}

	@Test
	public void testPiggyDismount() throws Exception {
		String message = "200 2005 OK\n.\nDLCX 1244 card23/21@tgw-7.example.net MGCP 1.0\nC: A3C47F21456789F0\nI: FDE234C8\naakkkxxcd";
		byte[] rawByte = message.getBytes();

		String[] messages = handler.piggyDismount(rawByte, rawByte.length-9);
		assertEquals("200 2005 OK\n", messages[0]);

		boolean flag = messages[1].startsWith("DLCX") && messages[1].endsWith("FDE234C8\n");
		assertTrue(flag);
		
		assertEquals("DLCX 1244 card23/21@tgw-7.example.net MGCP 1.0\nC: A3C47F21456789F0\nI: FDE234C8\n",messages[1] );
 
	}

	@Test
	public void testNoPiggyDismount() throws Exception {
		String message = "DLCX 1244 card23/21@tgw-7.example.net MGCP 1.0\nC: A3C47F21456789F0\nI: FDE234C8\n";
		byte[] rawByte = message.getBytes();

		String[] messages = handler.piggyDismount(rawByte, rawByte.length);

		assertEquals(1, messages.length);
		boolean flag = messages[0].startsWith("DLCX") && messages[0].endsWith("FDE234C8\n");
		assertTrue(flag);

	}

}
