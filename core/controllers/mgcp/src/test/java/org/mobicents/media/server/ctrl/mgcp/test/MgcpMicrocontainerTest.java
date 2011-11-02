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
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.DeleteConnectionResponse;
import jain.protocol.ip.mgcp.message.ModifyConnection;
import jain.protocol.ip.mgcp.message.ModifyConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jboss.test.kernel.junit.MicrocontainerTest;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackProviderImpl;

/**
 * 
 * @author amit bhayani
 * 
 */
public abstract class MgcpMicrocontainerTest extends MicrocontainerTest implements JainMgcpListener {

	// protected static long TRANSACTION_TIMES_OUT_FOR = 31000;
	protected static long STACKS_START_FOR = 1000;
	protected static long STACKS_SHUT_DOWN_FOR = 500; // timeout values depend on pc
	// protected static long MESSAGES_ARRIVE_FOR = 3000;
	// protected static long RETRANSMISSION_TRANSACTION_TIMES_OUT_FOR = 5000;
	protected static final String LOCAL_ADDRESS = "127.0.0.1";

	protected static final int CA_PORT = 2724;
	public static final int REMOTE_PORT = 2427;

	protected InetAddress caIPAddress = null;
	protected JainMgcpStackImpl caStack = null;
	protected JainMgcpStackProviderImpl caProvider = null;
	protected CallIdentifier callID;
	protected HashMap<String, Connection> connections = new HashMap();

	private Semaphore semaphore = new Semaphore(0);
	private JainMgcpResponseEvent response;
	private boolean op = false;

	public MgcpMicrocontainerTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		caIPAddress = InetAddress.getByName(LOCAL_ADDRESS);
		caStack = new JainMgcpStackImpl(caIPAddress, CA_PORT);
		caProvider = (JainMgcpStackProviderImpl) caStack.createProvider();
		caProvider.addJainMgcpListener(this);

		callID = caProvider.getUniqueCallIdentifier();
	}

	@Override
	public void tearDown() throws java.lang.Exception {

		System.out.println("CLOSE THE STACK");

		if (caStack != null) {
			caStack.close();
			caStack = null;
		}

		sleep(STACKS_SHUT_DOWN_FOR);
	}

	public Connection createLocalConnection(String endpointName, ConnectionMode mode, String secondEndpoint)
			throws Exception {
		EndpointIdentifier endpointID = new EndpointIdentifier(endpointName, "127.0.0.1:" + REMOTE_PORT);
		CreateConnection createConnection = new CreateConnection(this, callID, endpointID, mode);

		EndpointIdentifier secEndpointID = new EndpointIdentifier(secondEndpoint, "127.0.0.1:" + REMOTE_PORT);
		createConnection.setSecondEndpointIdentifier(secEndpointID);

		createConnection.setTransactionHandle(caProvider.getUniqueTransactionHandler());

		this.response = null;
		this.op = true;

		caProvider.sendMgcpEvents(new JainMgcpEvent[] { createConnection });
		semaphore.tryAcquire(5, TimeUnit.SECONDS);

		this.op = false;
		if (response == null) {
			throw new Exception("Time out");
		}

		CreateConnectionResponse resp = (CreateConnectionResponse) response;
		if (resp.getReturnCode().getValue() == ReturnCode.TRANSACTION_EXECUTED_NORMALLY) {
			Connection conn = new Connection(resp.getConnectionIdentifier());
			conn.setEndpoint(resp.getSpecificEndpointIdentifier());
			if (resp.getLocalConnectionDescriptor() != null) {
				conn.setLocalSdp(resp.getLocalConnectionDescriptor().toString());
			}

			conn.setSecondEndpoint(resp.getSecondEndpointIdentifier());
			conn.setSecondConnId(resp.getSecondConnectionIdentifier());

			return conn;
		}

		throw new Exception(resp.getReturnCode().getComment());
	}

	public Connection createConnection(String endpointName, ConnectionMode mode, String sdp) throws Exception {
		EndpointIdentifier endpointID = new EndpointIdentifier(endpointName, "127.0.0.1:" + REMOTE_PORT);
		CreateConnection createConnection = new CreateConnection(this, callID, endpointID, mode);
		if (sdp != null) {
			ConnectionDescriptor cd = new ConnectionDescriptor(sdp);
			createConnection.setRemoteConnectionDescriptor(cd);
		}
		createConnection.setTransactionHandle(caProvider.getUniqueTransactionHandler());

		this.response = null;
		this.op = true;

		caProvider.sendMgcpEvents(new JainMgcpEvent[] { createConnection });
		semaphore.tryAcquire(5, TimeUnit.SECONDS);

		this.op = false;
		if (response == null) {
			throw new Exception("Time out");
		}

		CreateConnectionResponse resp = (CreateConnectionResponse) response;
		if (resp.getReturnCode().getValue() == ReturnCode.TRANSACTION_EXECUTED_NORMALLY) {
			Connection conn = new Connection(resp.getConnectionIdentifier());
			conn.setEndpoint(resp.getSpecificEndpointIdentifier());
			conn.setLocalSdp(resp.getLocalConnectionDescriptor().toString());
			return conn;
		}

		throw new Exception(resp.getReturnCode().getComment());
	}

	public void modifyConnection(Connection connection, String remoteSdp) throws Exception {
		ModifyConnection modifyConnection = new ModifyConnection(this, callID, connection.getEndpoint(), connection
				.getId());
		ConnectionDescriptor connectionDescriptor = new ConnectionDescriptor(remoteSdp);
		modifyConnection.setRemoteConnectionDescriptor(connectionDescriptor);
		modifyConnection.setTransactionHandle(caProvider.getUniqueTransactionHandler());

		this.response = null;
		this.op = true;

		caProvider.sendMgcpEvents(new JainMgcpEvent[] { modifyConnection });
		semaphore.tryAcquire(5, TimeUnit.SECONDS);

		this.op = false;
		if (response == null) {
			throw new Exception("Time out");
		}

		ModifyConnectionResponse resp = (ModifyConnectionResponse) response;
		if (resp.getReturnCode().getValue() == ReturnCode.TRANSACTION_EXECUTED_NORMALLY) {
			connection.setRemoteSDP(remoteSdp);
		}
	}

	public void deleteConnectionConnection(Connection connection) throws Exception {
		DeleteConnection deleteConnection = new DeleteConnection(this, callID, connection.getEndpoint(), connection
				.getId());
		deleteConnection.setTransactionHandle(caProvider.getUniqueTransactionHandler());

		this.response = null;
		this.op = true;

		caProvider.sendMgcpEvents(new JainMgcpEvent[] { deleteConnection });
		semaphore.tryAcquire(5, TimeUnit.SECONDS);

		this.op = false;
		if (response == null) {
			throw new Exception("Time out");
		}

		DeleteConnectionResponse resp = (DeleteConnectionResponse) response;
		if (resp.getReturnCode().getValue() != ReturnCode.TRANSACTION_EXECUTED_NORMALLY) {
			throw new Exception("Could not delete connections: " + resp.getReturnCode().getComment());
		}
	}

	protected static void sleep(long sleepFor) {
		try {
			Thread.sleep(sleepFor);
		} catch (InterruptedException ex) {
			// Ignore
		}
	}

	public void processMgcpResponseEvent(JainMgcpResponseEvent event) {
		this.response = event;
		if (this.op)
			semaphore.release();
	}

	public void transactionEnded(int paramInt) {
		// TODO Auto-generated method stub
	}

	public void transactionRxTimedOut(JainMgcpCommandEvent paramJainMgcpCommandEvent) {
		// TODO Auto-generated method stub
	}

	public void transactionTxTimedOut(JainMgcpCommandEvent paramJainMgcpCommandEvent) {
		// TODO Auto-generated method stub
	}

}
