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

/**
 * Start time:12:54:42 2008-11-24<br>
 * Project: mobicents-media-server-controllers<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.protocols.mgcp.stack.test.endpointhandler;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.DeleteConnectionResponse;
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.NotificationRequestResponse;
import jain.protocol.ip.mgcp.message.Notify;
import jain.protocol.ip.mgcp.message.NotifyResponse;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.NotificationRequestParms;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;
import jain.protocol.ip.mgcp.message.parms.RequestedAction;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;

import java.net.InetAddress;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.stack.JainMgcpExtendedListener;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackProviderImpl;
import org.mobicents.protocols.mgcp.stack.MgcpResponseType;

/**
 * Start time:12:54:42 2008-11-24<br>
 * Project: mobicents-media-server-controllers<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class CA implements JainMgcpExtendedListener {

	private static Logger logger = Logger.getLogger(CA.class);

	private JainMgcpStackProviderImpl caProvider;
	private int mgStack = 0;
	private InetAddress localAddress = null;
	private int localPort = -1;

	protected boolean sentCCR, receivedCCResponse, sentNotificationRequest,
			receiveNotificationRequestResponse, receivedNotification,
			sentNotificatioAnswer, sentDLCX, receivedDLCXA;

	protected EndpointIdentifier specificEndpointId = null;
	protected ConnectionIdentifier specificConnectionId = null;

	public CA(JainMgcpStackProviderImpl caProvider,
			JainMgcpStackProviderImpl mgwProvider, InetAddress localAddress,
			int localPort) {
		this.caProvider = caProvider;
		mgStack = mgwProvider.getJainMgcpStack().getPort();
		this.localAddress = localAddress;
		this.localPort = localPort;
	}

	public void sendCRCX() {

		try {
			caProvider.addJainMgcpListener(this);

			CallIdentifier callID = caProvider.getUniqueCallIdentifier();

			EndpointIdentifier endpointID = new EndpointIdentifier(
					"media/trunk/Announcement/$", "127.0.0.1:" + mgStack);

			CreateConnection createConnection = new CreateConnection(this,
					callID, endpointID, ConnectionMode.SendRecv);

			String sdpData = "v=0\r\n"
					+ "o=4855 13760799956958020 13760799956958020"
					+ " IN IP4  127.0.0.1\r\n" + "s=mysession session\r\n"
					+ "p=+46 8 52018010\r\n" + "c=IN IP4  127.0.0.1\r\n"
					+ "t=0 0\r\n" + "m=audio 6022 RTP/AVP 0 4 18\r\n"
					+ "a=rtpmap:0 PCMU/8000\r\n" + "a=rtpmap:4 G723/8000\r\n"
					+ "a=rtpmap:18 G729A/8000\r\n" + "a=ptime:20\r\n";

			createConnection
					.setRemoteConnectionDescriptor(new ConnectionDescriptor(
							sdpData));

			createConnection.setTransactionHandle(caProvider
					.getUniqueTransactionHandler());

			System.err.println(" - "+localAddress+":"+localPort+" SENDING CRCX");
			
			caProvider.sendMgcpEvents(new JainMgcpEvent[] { createConnection });

			logger.debug(" CreateConnection command sent for TxId "
					+ createConnection.getTransactionHandle() + " and CallId "
					+ callID);
			sentCCR = true;
		} catch (Exception e) {
			e.printStackTrace();
			SimpleFlowTest.fail("Unexpected error: " + e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.mgcp.stack.JainMgcpExtendedListener#transactionEnded(int)
	 */
	public void transactionEnded(int handle) {
		System.err.println("Transaction ended out on = " + localAddress + ":"
				+ localPort);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.mgcp.stack.JainMgcpExtendedListener#transactionRxTimedOut
	 * (jain.protocol.ip.mgcp.JainMgcpCommandEvent)
	 */
	public void transactionRxTimedOut(JainMgcpCommandEvent command) {
		System.err.println("Transaction Rx timed out on = " + localAddress + ":"
				+ localPort);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.mgcp.stack.JainMgcpExtendedListener#transactionTxTimedOut
	 * (jain.protocol.ip.mgcp.JainMgcpCommandEvent)
	 */
	public void transactionTxTimedOut(JainMgcpCommandEvent command) {
		System.err.println("Transaction Tx timed out on = " + localAddress + ":"
				+ localPort);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jain.protocol.ip.mgcp.JainMgcpListener#processMgcpCommandEvent(jain.protocol
	 * .ip.mgcp.JainMgcpCommandEvent)
	 */
	public void processMgcpCommandEvent(JainMgcpCommandEvent command) {
		if (command instanceof Notify) {
			receivedNotification = true;
			System.err.println(" - "+localAddress+":"+localPort+" RECEIVE NOTIFY");
			NotifyResponse response = new NotifyResponse(command.getSource(),
					ReturnCode.Transaction_Executed_Normally);
			response.setTransactionHandle(command.getTransactionHandle());
			caProvider.sendMgcpEvents(new JainMgcpEvent[] { response });
			sentNotificatioAnswer = true;

			DeleteConnection deleteConnection = new DeleteConnection(this,
					this.specificEndpointId);

			deleteConnection.setConnectionIdentifier(this.specificConnectionId);

			deleteConnection.setTransactionHandle(caProvider
					.getUniqueTransactionHandler());
			
			//Lets add NotificationParms
			NotificationRequestParms parms=new NotificationRequestParms(new RequestIdentifier("1"));
			deleteConnection.setNotificationRequestParms(parms);
			
			System.err.println(" - "+localAddress+":"+localPort+" SEND DLCX");
			caProvider.sendMgcpEvents(new JainMgcpEvent[] { deleteConnection });
			sentDLCX = true;

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jain.protocol.ip.mgcp.JainMgcpListener#processMgcpResponseEvent(jain.
	 * protocol.ip.mgcp.JainMgcpResponseEvent)
	 */
	public void processMgcpResponseEvent(JainMgcpResponseEvent response) {

		MgcpResponseType type = MgcpResponseType
				.getResponseTypeFromCode(response.getReturnCode().getValue());

		if (response instanceof CreateConnectionResponse) {
			receivedCCResponse = true;
			System.err.println(" - "+localAddress+":"+localPort+" RECEIVE CRCXResponse");
			switch (type) {
			case SuccessResponse:
				// Tx executed properly
				CreateConnectionResponse event = (CreateConnectionResponse) response;
				ConnectionIdentifier connectionIdentifier = event
						.getConnectionIdentifier();
				this.specificEndpointId = event.getSpecificEndpointIdentifier();
				NotificationRequest notificationRequest = new NotificationRequest(
						this, specificEndpointId, this.caProvider
								.getUniqueRequestIdentifier());

				this.specificConnectionId=connectionIdentifier;
				this.specificEndpointId=event.getSpecificEndpointIdentifier();
				EventName[] signalRequests = { new EventName(
						PackageName.Announcement, MgcpEvent.ann
								.withParm("http://tests.ip:8080/test.wav"),
						connectionIdentifier) };
				notificationRequest.setSignalRequests(signalRequests);

				RequestedAction[] actions = new RequestedAction[] { RequestedAction.NotifyImmediately };

				RequestedEvent[] requestedEvents = {
						new RequestedEvent(new EventName(PackageName.Dtmf,
								MgcpEvent.dtmf0, connectionIdentifier), actions),
						new RequestedEvent(new EventName(
								PackageName.Announcement, MgcpEvent.of,
								connectionIdentifier), actions) };

				notificationRequest.setRequestedEvents(requestedEvents);
				notificationRequest.setTransactionHandle(caProvider
						.getUniqueTransactionHandler());

				NotifiedEntity notifiedEntity = new NotifiedEntity(
						this.localAddress.toString(), localAddress.toString(),
						this.localPort);
				notificationRequest.setNotifiedEntity(notifiedEntity);

				System.err.println(" - "+localAddress+":"+localPort+" SEND NR");
				caProvider
						.sendMgcpEvents(new JainMgcpEvent[] { notificationRequest });
				sentNotificationRequest = true;
				break;
			case ProvisionalResponse:
				break;
			default:
				SimpleFlowTest.fail("Bad message: " + response);
			}
		} else if (response instanceof NotificationRequestResponse) {
			receiveNotificationRequestResponse = true;
			System.err.println(" - "+localAddress+":"+localPort+" Receive NRResponse");
			switch (type) {
			case SuccessResponse:

				break;
			case ProvisionalResponse:
				break;
			default:
				SimpleFlowTest.fail("Bad message: " + response);
			}
		} else if (response instanceof DeleteConnectionResponse) {
			receivedDLCXA = true;
			switch (type) {
			case SuccessResponse:

				break;
			case ProvisionalResponse:
				break;
			default:
				SimpleFlowTest.fail("Bad message: " + response);
			}
		}

	}

	public void checkState() {
		if (sentCCR && receivedCCResponse && sentNotificationRequest
				&& receiveNotificationRequestResponse && receivedNotification
				&& sentNotificatioAnswer && sentDLCX && receivedDLCXA) {
			
		} else {
			
			System.err.println("Receival sentCCR[" + sentCCR + "] receivedCCResponse["
			                                                    					+ receivedCCResponse + "] sentNotificationRequest["
			                                                    					+ sentNotificationRequest
			                                                    					+ "] receiveNotificationRequestResponse["
			                                                    					+ receiveNotificationRequestResponse
			                                                    					+ "] receivedNotification[" + receivedNotification
			                                                    					+ "] sentNotificatioAnswer[" + sentNotificatioAnswer
			                                                    					+ "] sentDLCX[" + sentDLCX + "] receivedDLCXA["
			                                                    					+ receivedDLCXA + "]");
			SimpleFlowTest.fail("Receival sentCCR[" + sentCCR + "] receivedCCResponse["
					+ receivedCCResponse + "] sentNotificationRequest["
					+ sentNotificationRequest
					+ "] receiveNotificationRequestResponse["
					+ receiveNotificationRequestResponse
					+ "] receivedNotification[" + receivedNotification
					+ "] sentNotificatioAnswer[" + sentNotificatioAnswer
					+ "] sentDLCX[" + sentDLCX + "] receivedDLCXA["
					+ receivedDLCXA + "]");
		}
	}

}
