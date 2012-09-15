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

package org.mobicents.protocols.mgcp.stack.test.transactionretransmisson;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.stack.test.MessageFlowHarness;

public class TxRetransmissionTest extends MessageFlowHarness {

	private static Logger logger = Logger.getLogger("mgcp.test");

	private CA ca;
	private MGW mgw;

	public TxRetransmissionTest() {
		super("TxRetransmissionTest");
	}

	public void setUp() {
		try {
			super.setUp();

			ca = new CA(caProvider, mgProvider);
			mgw = new MGW(mgProvider);

		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Unexpected Exception");
		}
	}

	//TODO : We Just have 1 Thread now. Provisional Response for Re-Transmission will never be sent with this test. Need to modify test first
	
	public void testDummy() {
		assertTrue(true);
	}
	
//	public void testReTransmissionCreateConnection() {
//		
//		ca.setCommand("CRCX");
//		mgw.setCommand("CRCX");
//		this.ca.sendReTransmissionCreateConnection();
//		waitForRetransmissionTimeout();
//	}
//	
//	public void testReTransmissionDeleteConnection() {
//		ca.setCommand("DLCX");
//		mgw.setCommand("DLCX");
//		this.ca.sendReTransmissionDeleteConnection();
//		waitForRetransmissionTimeout();
//	}
//	
//	public void testReTransmissionModifyConnection() {
//		ca.setCommand("MDCX");
//		mgw.setCommand("MDCX");
//		this.ca.sendReTransmissionModifyConnection();
//		waitForRetransmissionTimeout();
//	}	
//	
//	public void testReTransmissionNotificationRequest() {
//		ca.setCommand("RQNT");
//		mgw.setCommand("RQNT");
//		this.ca.sendReTransmissionNotificationRequest();
//		waitForRetransmissionTimeout();
//	}
//	
//	public void testReTransmissionNotify() {
//		ca.setCommand("NTFY");
//		mgw.setCommand("NTFY");
//		this.ca.sendReTransmissionNotify();
//		waitForRetransmissionTimeout();
//	}	
	
	public void tearDown() {
		try {
			super.tearDown();
		} catch (Exception ex) {

		}
		
//		this.ca.checkState();
//		this.mgw.checkState();
		logTestCompleted();

	}
}
