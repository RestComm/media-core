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

package org.mobicents.media.server.ctrl.mgcp.test;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.stack.JainMgcpExtendedListener;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackProviderImpl;

/**
 * 
 * @author amit bhayani
 *
 */
public class CA implements JainMgcpExtendedListener {

	private static Logger logger = Logger.getLogger(CA.class);

	private JainMgcpStackProviderImpl caProvider;
	private boolean successCRCXRespReceived = false;
	private boolean successFormatNegotiationFail = false;

	public CA(JainMgcpStackProviderImpl caProvider) {
		this.caProvider = caProvider;
	}

	public void sendSuccessCRCX() {

		try {
			caProvider.addJainMgcpListener(this);

			CallIdentifier callID = caProvider.getUniqueCallIdentifier();

			EndpointIdentifier endpointID = new EndpointIdentifier("/mobicents/media/aap/1", "127.0.0.1:" + MgcpMicrocontainerTest.REMOTE_PORT);

			CreateConnection createConnection = new CreateConnection(this, callID, endpointID, ConnectionMode.SendOnly);

			String sdpData = "v=0\r\n" + "o=4855 13760799956958020 13760799956958020" + " IN IP4  127.0.0.1\r\n"
					+ "s=mysession session\r\n" + "p=+46 8 52018010\r\n" + "c=IN IP4  127.0.0.1\r\n" + "t=0 0\r\n"
					+ "m=audio 6022 RTP/AVP 0\r\n" + "a=rtpmap:0 PCMU/8000\r\n";

			createConnection.setRemoteConnectionDescriptor(new ConnectionDescriptor(sdpData));

			createConnection.setTransactionHandle(caProvider.getUniqueTransactionHandler());

			caProvider.sendMgcpEvents(new JainMgcpEvent[] { createConnection });

			logger.debug(" CreateConnection command sent for TxId " + createConnection.getTransactionHandle()
					+ " and CallId " + callID);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			MgcpMicrocontainerTest.fail("Unexpected Exception");
		}
	}

	public void sendFormatNegotiationFailCRCX() {

		try {
			caProvider.addJainMgcpListener(this);

			CallIdentifier callID = caProvider.getUniqueCallIdentifier();

			EndpointIdentifier endpointID = new EndpointIdentifier("/mobicents/media/aap/2", "127.0.0.1:" + MgcpMicrocontainerTest.REMOTE_PORT);

			CreateConnection createConnection = new CreateConnection(this, callID, endpointID, ConnectionMode.SendOnly);

			String sdpData = "v=0\r\n" + "o=4855 13760799956958020 13760799956958020" + " IN IP4  127.0.0.1\r\n"
					+ "s=mysession session\r\n" + "p=+46 8 52018010\r\n" + "c=IN IP4  127.0.0.1\r\n" + "t=0 0\r\n"
					+ "m=audio 6023 RTP/AVP 102\r\n" + "a=rtpmap:102 G726-16/8000\r\n" + "a=ptime:20\r\n";

			createConnection.setRemoteConnectionDescriptor(new ConnectionDescriptor(sdpData));

			createConnection.setTransactionHandle(caProvider.getUniqueTransactionHandler());

			caProvider.sendMgcpEvents(new JainMgcpEvent[] { createConnection });

			logger.debug(" CreateConnection command sent for TxId " + createConnection.getTransactionHandle()
					+ " and CallId " + callID);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			MgcpMicrocontainerTest.fail("Unexpected Exception");
		}
	}

	public void checkSuccessCRCX() {
		MgcpMicrocontainerTest.assertTrue("Expect to receive CRCX Response", successCRCXRespReceived);

	}

	public void checkFormatNegotiationFailCRCX() {
		MgcpMicrocontainerTest.assertTrue(successFormatNegotiationFail);

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
		case Constants.RESP_CREATE_CONNECTION:
			CreateConnectionResponse crcxResp = (CreateConnectionResponse) jainmgcpresponseevent;
			switch (crcxResp.getReturnCode().getValue()) {
			case ReturnCode.TRANSACTION_EXECUTED_NORMALLY:
				successCRCXRespReceived = true;
				break;

			case ReturnCode.MISSING_REMOTECONNECTIONDESCRIPTOR:
				successFormatNegotiationFail = true;
				break;
			default:
				logger.error("CRCX Response is not successfull. Recived ReturCode = " + crcxResp.getReturnCode());
				successCRCXRespReceived = false;
				successFormatNegotiationFail = false;
				break;
			}
			break;
		default:
			logger.warn("This RESPONSE is unexpected " + jainmgcpresponseevent);
			break;

		}

	}

}
