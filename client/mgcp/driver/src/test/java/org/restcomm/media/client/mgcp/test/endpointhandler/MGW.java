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

package org.restcomm.media.client.mgcp.test.endpointhandler;

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
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.net.InetAddress;
import java.util.TooManyListenersException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.client.mgcp.stack.JainMgcpExtendedListener;
import org.restcomm.media.client.mgcp.stack.JainMgcpStackProviderImpl;


public class MGW implements JainMgcpExtendedListener {

	
	private static Logger logger = LogManager.getLogger(MGW.class);
	private boolean responseSent = false;

	JainMgcpStackProviderImpl mgwProvider;

	private InetAddress localAddress = null;
	private int localPort = -1;
	private int caPort=-1;

	protected EndpointIdentifier specificEndpointId = null;
	protected ConnectionIdentifier specificConnectionId = null;
	private boolean receivedCCR;
	private boolean sentCCResponse;
	private boolean receivedNotificationRequest;
	private boolean sentNotification;
	private boolean sentNotificationRequestResponse;
	private boolean receivedNotificatioAnswer;
	private boolean sentDLCXA;
	private boolean receivedDLCX;
	
	
	public MGW(JainMgcpStackProviderImpl mgwProvider, InetAddress localAddress,int localPort, int caPort) {
		this.mgwProvider = mgwProvider;
		try {
			this.mgwProvider.addJainMgcpListener(this);
			this.localAddress = localAddress;
			this.localPort = localPort;
			this.caPort=caPort;
			
			
			
			
		} catch (TooManyListenersException e) {
			e.printStackTrace();
			SimpleFlowTest.fail("Unexpected Exception");
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

	public void processMgcpCommandEvent(JainMgcpCommandEvent command) {
	
		
		if(command instanceof CreateConnection)
		{
			receivedCCR=true;
			System.err.println(" - "+localAddress+":"+localPort+" RECEIVE CRCX");
			String identifier = ((CallIdentifier) mgwProvider.getUniqueCallIdentifier()).toString();
			ConnectionIdentifier connectionIdentifier = new ConnectionIdentifier(identifier);

			CreateConnectionResponse response = new CreateConnectionResponse(command.getSource(),
					ReturnCode.Transaction_Executed_Normally, connectionIdentifier);

			response.setTransactionHandle(command.getTransactionHandle());
			try{
				CreateConnection cc=(CreateConnection) command;
				EndpointIdentifier wildcard=cc.getEndpointIdentifier();
				EndpointIdentifier specific=new EndpointIdentifier(wildcard.getLocalEndpointName().replace("$", "")+"test-1",wildcard.getDomainName());
				response.setSpecificEndpointIdentifier(specific);
			}catch(Exception e)
			{
				e.printStackTrace();
			}
			
			this.specificConnectionId=connectionIdentifier;
			this.specificEndpointId=response.getSpecificEndpointIdentifier();
			System.err.println(" - "+localAddress+":"+localPort+" SENDING CCRespose");
			mgwProvider.sendMgcpEvents(new JainMgcpEvent[] { response });
			sentCCResponse=true;
			
			
		}else if(command instanceof NotificationRequest)
		{
			
			System.err.println(" - "+localAddress+":"+localPort+" RECEIVE NotificationRequest");
			receivedNotificationRequest=true;
			NotificationRequestResponse response = new NotificationRequestResponse(command.getSource(),
					ReturnCode.Transaction_Executed_Normally);
			response.setTransactionHandle(command.getTransactionHandle());
			System.err.println(" - "+localAddress+":"+localPort+" Sending NotificationreqeustResponse");
			mgwProvider.sendMgcpEvents(new JainMgcpEvent[] { response });
			sentNotificationRequestResponse=true;
			
			Notify notify = new Notify(this, specificEndpointId, mgwProvider.getUniqueRequestIdentifier(), new EventName[] {});
			notify.setTransactionHandle(mgwProvider.getUniqueTransactionHandler());

			//TODO We are forced to set the NotifiedEntity, but this should happen automatically. Fix this in MGCP Stack
			NotifiedEntity notifiedEntity = new NotifiedEntity("127.0.0.1", "127.0.0.1", caPort);
			notify.setNotifiedEntity(notifiedEntity);
			System.err.println(" - "+localAddress+":"+localPort+" Sending NOTIFY");
			mgwProvider.sendMgcpEvents(new JainMgcpEvent[] { notify });
			sentNotification=true;
			
		}else if(command instanceof DeleteConnection)
		{
			System.err.println(" - "+localAddress+":"+localPort+" RECEIVE DLCX");
			receivedDLCX=true;
			DeleteConnectionResponse response = new DeleteConnectionResponse(command
					.getSource(), ReturnCode.Transaction_Executed_Normally);

			response.setTransactionHandle(command.getTransactionHandle());
			System.err.println(" - "+localAddress+":"+localPort+" Sending DLCXresponse");
			mgwProvider.sendMgcpEvents(new JainMgcpEvent[] { response });
			sentDLCXA=true;
		}
			
		
		
	}

	public void processMgcpResponseEvent(JainMgcpResponseEvent resp) {
		if(resp instanceof NotifyResponse)
		{
			System.err.println(" - "+localAddress+":"+localPort+" RECEIVE NOTIFY Response");
			receivedNotificatioAnswer=true;
		}

	}

	public void checkState() {
		if (receivedCCR && sentCCResponse && receivedNotificationRequest
				&& sentNotificationRequestResponse && sentNotification
				&& receivedNotificatioAnswer && receivedDLCX && sentDLCXA) {
			
		} else {
			
			System.err.println("Receival receivedCCR[" + receivedCCR + "] sentCCResponse["
					+ sentCCResponse + "] receivedNotificationRequest["
					+ receivedNotificationRequest
					+ "] sentNotificationRequestResponse["
					+ sentNotificationRequestResponse
					+ "] sentNotification[" + sentNotification
					+ "] receivedNotificatioAnswer[" + receivedNotificatioAnswer
					+ "] receivedDLCX[" + receivedDLCX + "] sentDLCXA["
					+ sentDLCXA + "]");
			
			SimpleFlowTest.fail("Receival receivedCCR[" + receivedCCR + "] sentCCResponse["
					+ sentCCResponse + "] receivedNotificationRequest["
					+ receivedNotificationRequest
					+ "] sentNotificationRequestResponse["
					+ sentNotificationRequestResponse
					+ "] sentNotification[" + sentNotification
					+ "] receivedNotificatioAnswer[" + receivedNotificatioAnswer
					+ "] receivedDLCX[" + receivedDLCX + "] sentDLCXA["
					+ sentDLCXA + "]");
		}
	}
	
}
