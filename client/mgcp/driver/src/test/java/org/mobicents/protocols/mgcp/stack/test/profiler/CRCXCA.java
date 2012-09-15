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

package org.mobicents.protocols.mgcp.stack.test.profiler;

import jain.protocol.ip.mgcp.CreateProviderException;
import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.TooManyListenersException;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.stack.JainMgcpExtendedListener;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackProviderImpl;

/**
 * Simple CA that sends CRCX and expects back CRCX Resp. Used for profiling
 * 
 * @author amit bhayani
 * 
 */
public class CRCXCA implements JainMgcpExtendedListener {

	private static Logger logger = Logger.getLogger(CRCXCA.class);

	private JainMgcpStackProviderImpl caProvider;

	protected static final String CLIENT_ADDRESS = "127.0.0.1";

	protected static final String SERVER_ADDRESS = "127.0.0.1";

	protected static final int CA_PORT = 2724;

	protected static final int MGW_PORT = 2729;

	protected InetAddress caIPAddress = null;
	protected InetAddress mgIPAddress = null;

	protected JainMgcpStackImpl caStack = null;

	private static int count = 0;

	long start = 0l;

	public CRCXCA() {
	}

	public void createMgcpStack(JainMgcpListener listener) throws UnknownHostException, CreateProviderException,
			TooManyListenersException {
		caIPAddress = InetAddress.getByName(CLIENT_ADDRESS);
		caStack = new JainMgcpStackImpl(caIPAddress, CA_PORT);
		caProvider = (JainMgcpStackProviderImpl) caStack.createProvider();
		caProvider.addJainMgcpListener(listener);
	}

	public void sendCreateConnection() {

		try {

			CallIdentifier callID = caProvider.getUniqueCallIdentifier();

			EndpointIdentifier endpointID = new EndpointIdentifier("media/trunk/Announcement/$", "127.0.0.1:"
					+ MGW_PORT);

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

		}
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
			CreateConnectionResponse response = (CreateConnectionResponse) jainmgcpresponseevent;
			if (response.getReturnCode().getValue() == ReturnCode.TRANSACTION_EXECUTED_NORMALLY) {
				if (count < 100) {
					this.sendCreateConnection();
					count++;
				}

				if (count == 100) {
					System.out.println("Received CRCX Response " + count);
				}
			}
			break;
		default:
			logger.warn("This RESPONSE is unexpected " + jainmgcpresponseevent);

			break;

		}

	}

	public static void main(String args[]) {
		final CRCXCA ca = new CRCXCA();
		try {
			ca.createMgcpStack(ca);
			ca.start = System.currentTimeMillis();

			ca.sendCreateConnection();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (CreateProviderException e) {
			e.printStackTrace();
		} catch (TooManyListenersException e) {
			e.printStackTrace();
		}
	}

}
