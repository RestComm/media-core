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

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.ModifyConnection;
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.Notify;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.stack.JainMgcpExtendedListener;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackProviderImpl;
import org.mobicents.protocols.mgcp.stack.test.createconnection.CreateConnectionTest;
import org.mobicents.protocols.mgcp.stack.test.modifyconnection.ModifyConnectionTest;
import org.mobicents.protocols.mgcp.stack.test.notificationrequest.NotificationRequestTest;
import org.mobicents.protocols.mgcp.stack.test.notify.NotifyTest;

public class CA implements JainMgcpExtendedListener {

	private static Logger logger = Logger.getLogger(CA.class);

	private JainMgcpStackProviderImpl caProvider;
	private int mgStack = 0;
	private boolean finalResponseReceived = false;

	// The calling application/listener will not receive the provisional response. Hence
	// we are setting this to true
	private boolean provisionalResponseReceived = false;
	private String command;

	public CA(JainMgcpStackProviderImpl caProvider, JainMgcpStackProviderImpl mgwProvider) {
		this.caProvider = caProvider;
		mgStack = mgwProvider.getJainMgcpStack().getPort();
	}

	public void sendReTransmissionCreateConnection() {

		try {
			caProvider.addJainMgcpListener(this);

			CallIdentifier callID = caProvider.getUniqueCallIdentifier();

			EndpointIdentifier endpointID = new EndpointIdentifier("media/trunk/Announcement/$", "127.0.0.1:" + mgStack);

			CreateConnection createConnection = new CreateConnection(this, callID, endpointID, ConnectionMode.SendRecv);

			String sdpData = "v=0\r\n" + "o=4855 13760799956958020 13760799956958020" + " IN IP4  127.0.0.1\r\n"
					+ "s=mysession session\r\n" + "p=+46 8 52018010\r\n" + "c=IN IP4  127.0.0.1\r\n" + "t=0 0\r\n"
					+ "m=audio 6022 RTP/AVP 0 4 18\r\n" + "a=rtpmap:0 PCMU/8000\r\n" + "a=rtpmap:4 G723/8000\r\n"
					+ "a=rtpmap:18 G729A/8000\r\n" + "a=ptime:20\r\n";

			createConnection.setRemoteConnectionDescriptor(new ConnectionDescriptor(sdpData));

			createConnection.setTransactionHandle(caProvider.getUniqueTransactionHandler());

			caProvider.sendMgcpEvents(new JainMgcpEvent[] { createConnection });

			logger.debug(" CreateConnection command sent for TxId " + createConnection.getTransactionHandle()
					+ " and CallId " + callID);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			TxRetransmissionTest.fail("Unexpected Exception");
		}
	}

	public void sendReTransmissionDeleteConnection() {

		try {
			caProvider.addJainMgcpListener(this);

			EndpointIdentifier endpointID = new EndpointIdentifier("media/trunk/Announcement/", "127.0.0.1:" + mgStack);

			ConnectionIdentifier connectionIdentifier = new ConnectionIdentifier((caProvider.getUniqueCallIdentifier())
					.toString());

			DeleteConnection deleteConnection = new DeleteConnection(this, endpointID);
			deleteConnection.setConnectionIdentifier(connectionIdentifier);
			deleteConnection.setTransactionHandle(caProvider.getUniqueTransactionHandler());

			caProvider.sendMgcpEvents(new JainMgcpEvent[] { deleteConnection });

			logger.debug(" DeleteConnection command sent for TxId " + deleteConnection.getTransactionHandle()
					+ " and ConnectionIdentifier " + connectionIdentifier);
		} catch (Exception e) {
			e.printStackTrace();
			CreateConnectionTest.fail("Unexpected Exception");
		}
	}

	public void sendReTransmissionModifyConnection() {

		try {
			caProvider.addJainMgcpListener(this);

			CallIdentifier callID = caProvider.getUniqueCallIdentifier();

			EndpointIdentifier endpointID = new EndpointIdentifier("media/trunk/Announcement/", "127.0.0.1:" + mgStack);

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

	public void sendReTransmissionNotificationRequest() {

		try {
			caProvider.addJainMgcpListener(this);

			EndpointIdentifier endpointID = new EndpointIdentifier("media/trunk/Announcement/", "127.0.0.1:" + mgStack);

			NotificationRequest notificationRequest = new NotificationRequest(this, endpointID, caProvider
					.getUniqueRequestIdentifier());
			notificationRequest.setTransactionHandle(caProvider.getUniqueTransactionHandler());

			caProvider.sendMgcpEvents(new JainMgcpEvent[] { notificationRequest });

			logger.debug(" NotificationRequest command sent for TxId " + notificationRequest.getTransactionHandle());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			NotificationRequestTest.fail("Unexpected Exception");
		}
	}

	public void sendReTransmissionNotify() {

		try {
			caProvider.addJainMgcpListener(this);

			EndpointIdentifier endpointID = new EndpointIdentifier("media/trunk/Announcement/", "127.0.0.1:" + mgStack);

			Notify notify = new Notify(this, endpointID, caProvider.getUniqueRequestIdentifier(), new EventName[] {});
			notify.setTransactionHandle(caProvider.getUniqueTransactionHandler());

			// TODO We are forced to set the NotifiedEntity, but this should
			// happen automatically. Fix this in MGCP Stack
			NotifiedEntity notifiedEntity = new NotifiedEntity("127.0.0.1", "127.0.0.1", mgStack);
			notify.setNotifiedEntity(notifiedEntity);

			caProvider.sendMgcpEvents(new JainMgcpEvent[] { notify });

			logger.debug(" Notify command sent for TxId " + notify.getTransactionHandle());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			NotifyTest.fail("Unexpected Exception");
		}
	}

	public void checkState() {
		TxRetransmissionTest.assertTrue("Expect to receive " + command + " Provisional Response",
				provisionalResponseReceived);
		TxRetransmissionTest.assertTrue("Expect to receive " + command + " Final Response", finalResponseReceived);

	}

	public void transactionEnded(int handle) {
		logger.info("transactionEnded " + handle);

	}

	public void transactionRxTimedOut(JainMgcpCommandEvent jainMgcpCommandEvent) {
		logger.info("transactionRxTimedOut " + jainMgcpCommandEvent);

	}

	public void transactionTxTimedOut(JainMgcpCommandEvent jainMgcpCommandEvent) {
		logger.info("transactionTxTimedOut " + jainMgcpCommandEvent);

	}

	public void processMgcpCommandEvent(JainMgcpCommandEvent jainmgcpcommandevent) {
		logger.info("processMgcpCommandEvent " + jainmgcpcommandevent);
	}

	public void processMgcpResponseEvent(JainMgcpResponseEvent jainmgcpresponseevent) {
		logger.debug("processMgcpResponseEvent = " + jainmgcpresponseevent);
		switch (jainmgcpresponseevent.getObjectIdentifier()) {

		case Constants.RESP_NOTIFY:
		case Constants.RESP_NOTIFICATION_REQUEST:
		case Constants.RESP_MODIFY_CONNECTION:
		case Constants.RESP_DELETE_CONNECTION:
		case Constants.RESP_CREATE_CONNECTION:

			if (isProvisional(jainmgcpresponseevent.getReturnCode())) {
				provisionalResponseReceived = true;
			} else {
				finalResponseReceived = true;
			}

			break;
		default:
			logger.warn("This RESPONSE is unexpected " + jainmgcpresponseevent);
			break;

		}

	}

	private static boolean isProvisional(ReturnCode rc) {
		final int rval = rc.getValue();
		return ((99 < rval) && (rval < 200));
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

}
