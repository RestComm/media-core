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

package org.mobicents.protocols.mgcp.stack.test.modifyconnection;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.ModifyConnection;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.stack.JainMgcpExtendedListener;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackProviderImpl;

public class CA implements JainMgcpExtendedListener {

	private static Logger logger = Logger.getLogger(CA.class);

	private JainMgcpStackProviderImpl caProvider;
	private int mgStack = 0;
	private boolean responseReceived = false;

	public CA(JainMgcpStackProviderImpl caProvider, JainMgcpStackProviderImpl mgwProvider) {
		this.caProvider = caProvider;
		mgStack = mgwProvider.getJainMgcpStack().getPort();
	}

	public void sendModifyConnection() {

		try {
			caProvider.addJainMgcpListener(this);

			CallIdentifier callID = caProvider.getUniqueCallIdentifier();

			EndpointIdentifier endpointID = new EndpointIdentifier("media/trunk/Announcement/enp-15", "127.0.0.1:"
					+ mgStack);

			String identifier = ((CallIdentifier) caProvider.getUniqueCallIdentifier()).toString();
			ConnectionIdentifier connectionIdentifier = new ConnectionIdentifier(identifier);

			ModifyConnection modifyConnection = new ModifyConnection(this, callID, endpointID, connectionIdentifier);

			String sdpData = "v=0\r\n" + "o=4855 13760799956958020 13760799956958020" + " IN IP4  127.0.0.1\r\n"
					+ "s=mysession session\r\n" + "p=+46 8 52018010\r\n" + "c=IN IP4  127.0.0.1\r\n" + "t=0 0\r\n"
					+ "m=audio 6022 RTP/AVP 0 4 18\r\n" + "a=rtpmap:0 PCMU/8000\r\n" + "a=rtpmap:4 G723/8000\r\n"
					+ "a=rtpmap:18 G729A/8000\r\n" + "a=ptime:20\r\n";

			modifyConnection.setRemoteConnectionDescriptor(new ConnectionDescriptor(sdpData));

			modifyConnection.setTransactionHandle(caProvider.getUniqueTransactionHandler());

			caProvider.sendMgcpEvents(new JainMgcpEvent[] { modifyConnection });

			logger.debug(" ModifyConnection command sent for TxId " + modifyConnection.getTransactionHandle()
					+ " and CallId " + callID);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ModifyConnectionTest.fail("Unexpected Exception");
		}
	}

	public void checkState() {
		ModifyConnectionTest.assertTrue("Expect to receive MDCX Response", responseReceived);

	}

	public void transactionEnded(int handle) {
		logger.info("transactionEnded " + handle);

	}

	public void transactionRxTimedOut(JainMgcpCommandEvent command) {
		logger.info("transactionRxTimedOut " + command);

	}

	public void transactionTxTimedOut(JainMgcpCommandEvent command) {
		logger.info("transactionTxTimedOut " + command);

	}

	public void processMgcpCommandEvent(JainMgcpCommandEvent jainmgcpcommandevent) {
		logger.info("processMgcpCommandEvent " + jainmgcpcommandevent);
	}

	public void processMgcpResponseEvent(JainMgcpResponseEvent jainmgcpresponseevent) {
		logger.debug("processMgcpResponseEvent = " + jainmgcpresponseevent);
		switch (jainmgcpresponseevent.getObjectIdentifier()) {
		case Constants.RESP_MODIFY_CONNECTION:
			if (jainmgcpresponseevent.getReturnCode().getValue() == ReturnCode.ENDPOINT_INSUFFICIENT_RESOURCES
					|| jainmgcpresponseevent.getReturnCode().getValue() == ReturnCode.TRANSACTION_EXECUTED_NORMALLY) {
				responseReceived = true;
			}
			break;
		default:
			logger.warn("This RESPONSE is unexpected " + jainmgcpresponseevent);
			ModifyConnectionTest.fail("Incorrect response for MDCX command ");
			responseReceived = false;
			break;

		}

	}

}
