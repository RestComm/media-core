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
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.RequestedAction;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TooManyListenersException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.stack.JainMgcpExtendedListener;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackProviderImpl;
import org.mobicents.protocols.mgcp.stack.test.TestHarness;

public class CA extends TestHarness implements JainMgcpExtendedListener {

	private static Logger logger = Logger.getLogger(CA.class);

	protected static final String CLIENT_ADDRESS = "127.0.0.1";

	protected static final String SERVER_ADDRESS = "127.0.0.1";

	protected static final int CA_PORT = 2427;

	protected static final int MGW_PORT = 2727;
	static int NDIALOGS = 50000;

	static int MAXCONCURRENTCRCX = 15;

	// a ramp-up period is required for performance testing.
	int deleteCount = -100;

	protected InetAddress caIPAddress = null;
	protected InetAddress mgIPAddress = null;

	protected JainMgcpStackImpl caStack = null;

	private int ENDPOINT_ID = 1;

	AtomicInteger nbConcurrentInvite = new AtomicInteger(0);

	private JainMgcpStackProviderImpl caProvider;

	long start = 0l;

	private static Timer timer;

	static {
		timer = new Timer();
	}

	// //////////////////////////////////
	// //// Listeners Method start /////
	// /////////////////////////////////

	public void transactionEnded(int handle) {
		// TODO Auto-generated method stub

	}

	public void transactionRxTimedOut(JainMgcpCommandEvent command) {
		System.out.println("Transaction Request Time out");
		fail("Unexpected event: Rx TimeoutEvent ");
	}

	public void transactionTxTimedOut(JainMgcpCommandEvent command) {
		System.out.println("Transaction Time out \n"+ command);
		fail("Unexpected event: TimeoutEvent ");

	}

	public void processMgcpCommandEvent(JainMgcpCommandEvent jainmgcpcommandevent) {
		// TODO Auto-generated method stub

	}

	public void processMgcpResponseEvent(JainMgcpResponseEvent jainmgcpresponseevent) {
		Appdata appdatad = (Appdata) jainmgcpresponseevent.getSource();

		EndpointIdentifier endpointID = new EndpointIdentifier(appdatad.getEndpointId(), SERVER_ADDRESS + ":"
				+ MGW_PORT);

		switch (jainmgcpresponseevent.getObjectIdentifier()) {
		case Constants.RESP_CREATE_CONNECTION:
			CreateConnectionResponse crcxResp = (CreateConnectionResponse) jainmgcpresponseevent;

			if (logger.isDebugEnabled()) {
				logger.debug("Received CRCX Response Tx Id = " + crcxResp.getTransactionHandle() + " Connection ID = "
						+ crcxResp.getConnectionIdentifier());
			}

			appdatad.setReceivedCrcxResponse(true);

			ConnectionIdentifier connectionIdentifier = crcxResp.getConnectionIdentifier();
			appdatad.setConnectionIdentifier(connectionIdentifier);
			// send RQNT

			NotificationRequest notificationRequest = new NotificationRequest(appdatad, endpointID, caProvider
					.getUniqueRequestIdentifier());
			notificationRequest.setTransactionHandle(caProvider.getUniqueTransactionHandler());

			RequestedAction[] actions = new RequestedAction[] { RequestedAction.NotifyImmediately };

			RequestedEvent[] requestedEvents = {
					new RequestedEvent(new EventName(PackageName.Announcement, MgcpEvent.oc, connectionIdentifier),
							actions),
					new RequestedEvent(new EventName(PackageName.Announcement, MgcpEvent.of, connectionIdentifier),
							actions) };

			notificationRequest.setRequestedEvents(requestedEvents);

			if (logger.isDebugEnabled()) {
				logger.debug("Sending RQNT Tx Id = " + notificationRequest.getTransactionHandle() + " Connection ID = "
						+ connectionIdentifier);
			}

			caProvider.sendMgcpEvents(new JainMgcpEvent[] { notificationRequest });
			break;
		case Constants.RESP_NOTIFICATION_REQUEST:

			if (logger.isDebugEnabled()) {
				logger.debug("Received RQNT Response Tx Id = " + jainmgcpresponseevent.getTransactionHandle()
						+ " Connection ID = " + appdatad.getConnectionIdentifier());
			}

			appdatad.setReceivedRqntResponse(true);

			// Send DLCX

			DeleteConnection deleteConnection = new DeleteConnection(appdatad, endpointID);
			deleteConnection.setConnectionIdentifier(appdatad.getConnectionIdentifier());
			deleteConnection.setTransactionHandle(caProvider.getUniqueTransactionHandler());

			if (logger.isDebugEnabled()) {
				logger.debug("Sending DLCX Tx Id = " + deleteConnection.getTransactionHandle() + " Connection ID = "
						+ appdatad.getConnectionIdentifier());
			}

			caProvider.sendMgcpEvents(new JainMgcpEvent[] { deleteConnection });

			break;
		case Constants.RESP_DELETE_CONNECTION:

			if (logger.isDebugEnabled()) {
				logger.debug("Received DLCX Response Tx Id = " + jainmgcpresponseevent.getTransactionHandle()
						+ " Connection ID = " + appdatad.getConnectionIdentifier());
			}

			appdatad.setReceivedDlcxResponse(true);

			int ndialogs = nbConcurrentInvite.decrementAndGet();
			// System.out.println(nbConcurrentInvite);
			if (ndialogs > MAXCONCURRENTCRCX) {
				System.out.println("Concurrent invites = " + ndialogs);
			}
			synchronized (this) {
				if (ndialogs < MAXCONCURRENTCRCX / 2)
					this.notify();
			}

			this.deleteCount++;

			if (this.deleteCount == NDIALOGS) {
				long current = System.currentTimeMillis();
				float sec = (float) (current - start) / 1000f;

				logger.info("Total time in sec = " + sec);
				logger.info("Thrupt = " + (float) (NDIALOGS / sec));
				System.out.println("Total time in sec = " + sec);
				System.out.println("Thrupt = " + (float) (NDIALOGS / sec));
			}
			break;
		default:
			System.out.println("This RESPONSE is unexpected " + jainmgcpresponseevent);
			logger.error("This RESPONSE is unexpected " + jainmgcpresponseevent);
			break;

		}

	}

	// //////////////////////////////////
	// //// Listeners Method over //////
	// /////////////////////////////////

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

			if (ENDPOINT_ID == 11) {
				ENDPOINT_ID = 1;
			}
			EndpointIdentifier endpointID = new EndpointIdentifier("media/trunk/Announcement/enp-" + ENDPOINT_ID++,
					SERVER_ADDRESS + ":" + MGW_PORT);

			String endpointName = endpointID.getLocalEndpointName();

			Appdata appdata = new Appdata(endpointName);

			CreateConnection createConnection = new CreateConnection(appdata, callID, endpointID,
					ConnectionMode.SendRecv);

			String sdpData = "v=0\r\n" + "o=4855 13760799956958020 13760799956958020" + " IN IP4  127.0.0.1\r\n"
					+ "s=mysession session\r\n" + "p=+46 8 52018010\r\n" + "c=IN IP4  127.0.0.1\r\n" + "t=0 0\r\n"
					+ "m=audio 6022 RTP/AVP 0 4 18\r\n" + "a=rtpmap:0 PCMU/8000\r\n" + "a=rtpmap:4 G723/8000\r\n"
					+ "a=rtpmap:18 G729A/8000\r\n" + "a=ptime:20\r\n";

			createConnection.setRemoteConnectionDescriptor(new ConnectionDescriptor(sdpData));

			createConnection.setTransactionHandle(caProvider.getUniqueTransactionHandler());
			nbConcurrentInvite.incrementAndGet();
			if (logger.isDebugEnabled()) {
				logger.debug("Sending CRCX Tx Id = " + createConnection.getTransactionHandle());
			}
			caProvider.sendMgcpEvents(new JainMgcpEvent[] { createConnection });

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Unexpected Exception " + e.getMessage());
		}
	}

	class Appdata {

		protected TTask ttask;

		protected long startTime;

		protected long endTime;

		private ConnectionIdentifier connectionIdentifier = null;
		private boolean receivedCrcxResponse = false;
		private boolean receivedRqntResponse = false;
		private boolean receivedDlcxResponse = false;

		String endpointId = null;

		Appdata(String endpointId) {
			this.endpointId = endpointId;
			ttask = new TTask(this);
			timer.schedule(ttask, 20 * 1000 * NDIALOGS / 100);
			startTime = System.currentTimeMillis();
		}

		public void setReceivedCrcxResponse(boolean response) {
			if (!receivedRqntResponse && !receivedDlcxResponse) {
				this.receivedCrcxResponse = response;
			} else {

				System.out.println("receivedCrcxResponse after receivedRqntResponse or receivedDlcxResponse");
				System.out.println("System falsed " + this.toString());
				System.exit(0);
			}
		}

		public void setReceivedRqntResponse(boolean response) {
			if (receivedCrcxResponse && !receivedDlcxResponse) {
				this.receivedRqntResponse = response;
			} else {

				System.out.println("receivedRqntResponse after receivedDlcxResponse or before receivedCrcxResponse");
				System.out.println("System falsed " + this.toString());
				System.exit(0);
			}
		}

		public void setReceivedDlcxResponse(boolean response) {
			if (receivedCrcxResponse && receivedRqntResponse) {
				this.receivedDlcxResponse = response;
			} else {

				System.out.println("receivedDlcxResponse before receivedCrcxResponse or receivedDlcxResponse");
				System.out.println("System falsed " + this.toString());
				System.exit(0);
			}
		}

		public TTask getTtask() {
			return ttask;
		}

		public void setTtask(TTask ttask) {
			this.ttask = ttask;
		}

		public String getEndpointId() {
			return endpointId;
		}

		public void setEndpointId(String endpointId) {
			this.endpointId = endpointId;
		}

		public boolean isReceivedCrcxResponse() {
			return receivedCrcxResponse;
		}

		public boolean isReceivedRqntResponse() {
			return receivedRqntResponse;
		}

		public boolean isReceivedDlcxResponse() {
			return receivedDlcxResponse;
		}

		public void cancelTimer() {
			this.ttask.cancel();
			endTime = System.currentTimeMillis();
		}

		public String toString() {
			return new StringBuffer("EndpointId = ").append(this.endpointId).append(" receivedCrcxResponse = ").append(
					receivedCrcxResponse).append(" receivedRqntResponse= ").append(receivedRqntResponse).append(
					" receivedDlcxResponse= ").append(receivedDlcxResponse).toString();

		}

		public ConnectionIdentifier getConnectionIdentifier() {
			return connectionIdentifier;
		}

		public void setConnectionIdentifier(ConnectionIdentifier connectionIdentifier) {
			this.connectionIdentifier = connectionIdentifier;
		}
	}

	class TTask extends TimerTask {
		Appdata appdata;

		public TTask(Appdata appdata) {
			this.appdata = appdata;
		}

		public void run() {
			if (!this.appdata.isReceivedCrcxResponse() || !this.appdata.isReceivedDlcxResponse()
					|| !this.appdata.isReceivedRqntResponse()) {
				System.out.println("Appdata " + appdata.toString());
				logger.info("Appdata " + appdata.toString());
				System.exit(0);
			} else {
				this.appdata = null;
			}
		}
	}

	public static void main(String args[]) {

		int noOfCalls = Integer.parseInt(args[0]);
		int noOfConcurrentCalls = Integer.parseInt(args[1]);

		System.out.println("Number calls to be completed = " + noOfCalls
				+ " Number of concurrent calls to be maintained = " + noOfConcurrentCalls);

		logger.info("Number calls to be completed = " + noOfCalls + " Number of concurrent calls to be maintained = "
				+ noOfConcurrentCalls);

		NDIALOGS = noOfCalls;
		MAXCONCURRENTCRCX = noOfConcurrentCalls;

		final CA ca = new CA();
		try {
			ca.createMgcpStack(ca);
			ca.start = System.currentTimeMillis();

			while (ca.deleteCount < NDIALOGS) {

				while (ca.nbConcurrentInvite.intValue() >= MAXCONCURRENTCRCX) {
					System.out.println("nbConcurrentInvite = " + ca.nbConcurrentInvite.intValue()
							+ " Waiting for max CRCX count to go down!");

					logger.info("nbConcurrentInvite = " + ca.nbConcurrentInvite.intValue()
							+ " Waiting for max CRCX count to go down!");
					synchronized (ca) {
						try {
							ca.wait();
						} catch (Exception ex) {
						}
					}
				}

				if (ca.deleteCount == 0) {
					ca.start = System.currentTimeMillis();
				}

//				try {
//					Thread.sleep(2);
//				} catch (InterruptedException e) {
//				}

				ca.sendCreateConnection();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (CreateProviderException e) {
			e.printStackTrace();
		} catch (TooManyListenersException e) {
			e.printStackTrace();
		}

	}

}
