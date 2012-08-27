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

package org.mobicents.protocols.mgcp.stack.test.concurrency;

import jain.protocol.ip.mgcp.CreateProviderException;
import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.DeleteConnectionResponse;
import jain.protocol.ip.mgcp.message.NotificationRequestResponse;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.TooManyListenersException;

import org.mobicents.protocols.mgcp.stack.JainMgcpExtendedListener;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackProviderImpl;
import org.mobicents.protocols.mgcp.stack.test.TestHarness;

public class MGW extends TestHarness implements JainMgcpExtendedListener {

	protected static final String CLIENT_ADDRESS = "127.0.0.1";

	protected static final String SERVER_ADDRESS = "127.0.0.1";

	protected static final int CA_PORT = 2724;

	protected static final int MGW_PORT = 2727;
	static int NDIALOGS = 1000;

	private int terminationCount = 0;
	private int timedOut = 0;

	protected InetAddress mgwIPAddress = null;

	protected JainMgcpStackImpl mgwStack = null;

	private JainMgcpStackProviderImpl mgwProvider = null;

	long start;


	// //////////////////////////////////
	// //// Listeners Method start /////
	// /////////////////////////////////

	public void transactionEnded(int handle) {
		// TODO Auto-generated method stub

	}

	public void transactionRxTimedOut(JainMgcpCommandEvent command) {
		this.timedOut ++;
		System.out.println("Timed Out Transaction "+timedOut);
	}

	public void transactionTxTimedOut(JainMgcpCommandEvent command) {
		this.timedOut ++;
		System.out.println("Timed Out Transaction "+timedOut);

	}

	public void processMgcpCommandEvent(JainMgcpCommandEvent jainmgcpcommandevent) {
		switch (jainmgcpcommandevent.getObjectIdentifier()) {
		case Constants.CMD_CREATE_CONNECTION:

			String identifier = ((CallIdentifier) mgwProvider.getUniqueCallIdentifier()).toString();
			ConnectionIdentifier connectionIdentifier = new ConnectionIdentifier(identifier);

			CreateConnectionResponse response = new CreateConnectionResponse(jainmgcpcommandevent.getSource(),
					ReturnCode.Transaction_Executed_Normally, connectionIdentifier);

			response.setTransactionHandle(jainmgcpcommandevent.getTransactionHandle());
			response.setSpecificEndpointIdentifier(jainmgcpcommandevent.getEndpointIdentifier());
			mgwProvider.sendMgcpEvents(new JainMgcpEvent[] { response });

			break;

		case Constants.CMD_NOTIFICATION_REQUEST:

			NotificationRequestResponse ntrqResponse = new NotificationRequestResponse(
					jainmgcpcommandevent.getSource(), ReturnCode.Transaction_Executed_Normally);
			ntrqResponse.setTransactionHandle(jainmgcpcommandevent.getTransactionHandle());
			mgwProvider.sendMgcpEvents(new JainMgcpEvent[] { ntrqResponse });

			break;

		case Constants.CMD_DELETE_CONNECTION:

			DeleteConnectionResponse dlcxResponse = new DeleteConnectionResponse(jainmgcpcommandevent.getSource(),
					ReturnCode.Transaction_Executed_Normally);

			dlcxResponse.setTransactionHandle(jainmgcpcommandevent.getTransactionHandle());

			mgwProvider.sendMgcpEvents(new JainMgcpEvent[] { dlcxResponse });

			terminationCount++;
			if(terminationCount % 100 == 0) System.out.println("MGCP cycle termination count = " + terminationCount);
			
//			if(terminationCount == 10100){
//				stop();
//			}
			break;

		default:
			System.out.println("This COMMAND is unexpected " + jainmgcpcommandevent);
			break;

		}

	}

	public void processMgcpResponseEvent(JainMgcpResponseEvent jainmgcpresponseevent) {
		// TODO Auto-generated method stub

	}
	
	private void stop(){
		this.mgwStack.close();
	}

	// //////////////////////////////////
	// //// Listeners Method over //////
	// /////////////////////////////////

	public void createMgcpStack(JainMgcpListener listener) throws UnknownHostException, CreateProviderException,
			TooManyListenersException {
		mgwIPAddress = InetAddress.getByName(SERVER_ADDRESS);
		mgwStack = new JainMgcpStackImpl(mgwIPAddress, MGW_PORT);
		mgwProvider = (JainMgcpStackProviderImpl) mgwStack.createProvider();
		mgwProvider.addJainMgcpListener(listener);
	}

	public static void main(String args[]) {
		final MGW mgw = new MGW();
		try {
			mgw.createMgcpStack(mgw);
			mgw.start = System.currentTimeMillis();

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (CreateProviderException e) {
			e.printStackTrace();
		} catch (TooManyListenersException e) {
			e.printStackTrace();
		}

	}

}
