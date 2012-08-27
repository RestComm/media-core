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

package org.mobicents.protocols.mgcp.stack.test.commandparsing;

import jain.protocol.ip.mgcp.CreateProviderException;
import jain.protocol.ip.mgcp.JainMgcpProvider;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.DeleteConnectionResponse;
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;
import jain.protocol.ip.mgcp.message.parms.RequestedAction;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;

import org.mobicents.protocols.mgcp.stack.CreateConnectionHandler;
import org.mobicents.protocols.mgcp.stack.DeleteConnectionHandler;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;
import org.mobicents.protocols.mgcp.stack.NotificationRequestHandler;
import org.mobicents.protocols.mgcp.stack.test.TestHarness;

public class CommandParseTest extends TestHarness {

	private JainMgcpStackImpl jainMgcpStack = null;
	InetAddress inetAddress;
	int port;

	public CommandParseTest() {
		super("CommandParseTest");
	}

	@Override
	public void setUp() {

		try {
			inetAddress = InetAddress.getByName("127.0.0.1");
			jainMgcpStack = new JainMgcpStackImpl(inetAddress, 2729);
			
			port = jainMgcpStack.getPort();
			
			JainMgcpProvider provider = jainMgcpStack.createProvider();
		} catch (UnknownHostException e) {			
			e.printStackTrace();
			fail("Could not setUp the CommandParseTest");
		} catch (CreateProviderException e) {
			e.printStackTrace();
			fail("Could not setUp the CommandParseTest");
		}

	}

	@Override
	public void tearDown() {
		jainMgcpStack.close();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

/*	public void testNotificationRequestDecode() {
		String command = "RQNT 1201 aaln/1@rgw-2567.whatever.net MGCP 1.0 \nN: ca@ca1.whatever.net:5678 \nX: 16AC \nR: L/hd(N) \nS: L/rg";

		NotificationRequest notificationCommand = null;
		NotificationRequestHandler rqntHandler = new NotificationRequestHandler(jainMgcpStack, inetAddress, port);
		try {
			notificationCommand = (NotificationRequest) rqntHandler.decodeCommand(command);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Parsing of RQNT failed");
		}

		assertNotNull(" RQNT Command created", notificationCommand);
		assertEquals(new Integer(Constants.CMD_NOTIFICATION_REQUEST), new Integer(notificationCommand
				.getObjectIdentifier()));

		RequestIdentifier X = notificationCommand.getRequestIdentifier();
		assertNotNull(" RQNT Command RequestIdentifier", X);

		NotifiedEntity N = notificationCommand.getNotifiedEntity();
		assertNotNull(" RQNT NotifiedEntity", N);
		assertEquals(N.getLocalName(), "ca");
		assertEquals(N.getDomainName(), "ca1.whatever.net");
		assertEquals(N.getPortNumber(), 5678);

		RequestedEvent[] R = notificationCommand.getRequestedEvents();
		assertNotNull(" RQNT RequestedEvent[]", R);
		RequestedEvent requestedEvent = R[0];
		assertNotNull("RequestedEvent ", requestedEvent);

		EventName eventName = requestedEvent.getEventName();
		assertNotNull(eventName);

		PackageName packageName = eventName.getPackageName();
		assertNotNull(packageName);
		assertEquals(packageName.intValue(), PackageName.LINE);

		MgcpEvent mgcpEvent = eventName.getEventIdentifier();
		assertNotNull(mgcpEvent);
		assertEquals(mgcpEvent.intValue(), MgcpEvent.OFF_HOOK_TRANSITION);

		EventName[] eventNames = notificationCommand.getSignalRequests();
		assertNotNull(eventNames);

		EventName signalEventName = eventNames[0];
		assertNotNull(signalEventName);

		PackageName signalPackageName = signalEventName.getPackageName();
		assertNotNull(signalPackageName);
		assertEquals(signalPackageName.intValue(), PackageName.LINE);

		MgcpEvent signalMgcpEvent = signalEventName.getEventIdentifier();
		assertNotNull(signalMgcpEvent);
		assertEquals(signalMgcpEvent.intValue(), MgcpEvent.RINGING);

		// requestedEvent.getEventName()

	} */

	public void testNotificationRequestEncode() {

		String originalCommand = "RQNT 155 ann/1@rgw-2567.whatever.net MGCP 1.0\nX:16AC\nS:A/ann@17AC(http://127.0.0.1:8080/mms-load-test/mms.wav)\nR:A/oc@17AC (N),A/of@17AC (N)";

		EndpointIdentifier endpointIdentifier = new EndpointIdentifier("ann/1", "rgw-2567.whatever.net");
		RequestIdentifier R = new RequestIdentifier("16AC");
		NotificationRequest notificationRequest = new NotificationRequest(new Object(), endpointIdentifier, R);
		ConnectionIdentifier connectionIdentifier = new ConnectionIdentifier("17AC");
		EventName[] signalRequests = { new EventName(PackageName.Announcement, MgcpEvent.ann
				.withParm("http://127.0.0.1:8080/mms-load-test/mms.wav"), connectionIdentifier) };
		notificationRequest.setSignalRequests(signalRequests);

		RequestedAction[] actions = new RequestedAction[] { RequestedAction.NotifyImmediately };
		RequestedEvent[] requestedEvents = {
				new RequestedEvent(new EventName(PackageName.Announcement, MgcpEvent.oc, connectionIdentifier), actions),
				new RequestedEvent(new EventName(PackageName.Announcement, MgcpEvent.of, connectionIdentifier), actions) };
		notificationRequest.setRequestedEvents(requestedEvents);
		notificationRequest.setTransactionHandle(155);

		NotificationRequestHandler rqntHandler = new NotificationRequestHandler(jainMgcpStack, inetAddress, port);
		String command = rqntHandler.encode(notificationRequest);

		// assertEquals(originalCommand, command);

		System.out.println(command);
	}

//	public void testNotifyDecode() {
//		String command = "NTFY 2002 aaln/1@rgw-2567.whatever.net MGCP 1.0\nN: ca@ca1.whatever.net:5678\nX: 16AC\nO: L/hd,D/9,D/1,D/2,D/0,D/1,D/8,D/2,D/9,D/4,D/2,D/6,D/6";
//
//		Notify notify = null;
//		NotifyHandler ntfyHandler = new NotifyHandler(jainMgcpStack, inetAddress, port);
//		try {
//			notify = (Notify) ntfyHandler.decodeCommand(command);
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			fail("Parsing of NTFY failed");
//		}
//
//		assertNotNull(" NTFY Command created", notify);
//		assertEquals(new Integer(Constants.CMD_NOTIFY), new Integer(notify.getObjectIdentifier()));
//
//		RequestIdentifier X = notify.getRequestIdentifier();
//		assertNotNull(" NTFY Command RequestIdentifier", X);
//
//		X.toString();
//
//		NotifiedEntity N = notify.getNotifiedEntity();
//		assertNotNull(" NTFY NotifiedEntity", N);
//		assertEquals(N.getLocalName(), "ca");
//		assertEquals(N.getDomainName(), "ca1.whatever.net");
//		assertEquals(N.getPortNumber(), 5678);
//
//		EventName[] O = notify.getObservedEvents();
//		assertNotNull(" NTFY ObservedEvents", O);
//
//		EventName offHookEvent = O[0];
//		assertNotNull(offHookEvent);
//
//		PackageName linePackageName = offHookEvent.getPackageName();
//		assertNotNull(linePackageName);
//		assertEquals(linePackageName.intValue(), PackageName.LINE);
//
//		MgcpEvent offHook = offHookEvent.getEventIdentifier();
//		assertNotNull(offHook);
//		assertEquals(offHook.intValue(), MgcpEvent.OFF_HOOK_TRANSITION);
//
//		// DTMF 9
//		EventName DTMFEvent9 = O[1];
//		assertNotNull(DTMFEvent9);
//
//		PackageName DTMFPackageName9 = DTMFEvent9.getPackageName();
//		assertNotNull(DTMFPackageName9);
//		assertEquals(PackageName.DTMF, DTMFPackageName9.intValue());
//
//		MgcpEvent DTMFMgcpEvent9 = DTMFEvent9.getEventIdentifier();
//		assertNotNull(DTMFMgcpEvent9);
//		assertEquals("9", DTMFMgcpEvent9.getName());
//
//		// DTMF 1
//		EventName DTMFEvent1 = O[2];
//		assertNotNull(DTMFEvent1);
//
//		PackageName DTMFPackageName1 = DTMFEvent1.getPackageName();
//		assertNotNull(DTMFPackageName1);
//		assertEquals(PackageName.DTMF, DTMFPackageName1.intValue());
//
//		MgcpEvent DTMFMgcpEvent1 = DTMFEvent1.getEventIdentifier();
//		assertNotNull(DTMFMgcpEvent1);
//		assertEquals("1", DTMFMgcpEvent1.getName());
//
//		// DTMF 2
//		EventName DTMFEvent2 = O[3];
//		assertNotNull(DTMFEvent2);
//
//		PackageName DTMFPackageName2 = DTMFEvent2.getPackageName();
//		assertNotNull(DTMFPackageName2);
//		assertEquals(PackageName.DTMF, DTMFPackageName2.intValue());
//
//		MgcpEvent DTMFMgcpEvent2 = DTMFEvent2.getEventIdentifier();
//		assertNotNull(DTMFMgcpEvent2);
//		assertEquals("2", DTMFMgcpEvent2.getName());
//
//		// DTMF 0
//		EventName DTMFEvent0 = O[4];
//		assertNotNull(DTMFEvent0);
//
//		PackageName DTMFPackageName0 = DTMFEvent0.getPackageName();
//		assertNotNull(DTMFPackageName0);
//		assertEquals(PackageName.DTMF, DTMFPackageName0.intValue());
//
//		MgcpEvent DTMFMgcpEvent0 = DTMFEvent0.getEventIdentifier();
//		assertNotNull(DTMFMgcpEvent0);
//		assertEquals("0", DTMFMgcpEvent0.getName());
//
//		// DTMF 1 - Again
//		EventName DTMFEvent11 = O[5];
//		assertNotNull(DTMFEvent11);
//
//		PackageName DTMFPackageName11 = DTMFEvent11.getPackageName();
//		assertNotNull(DTMFPackageName11);
//		assertEquals(PackageName.DTMF, DTMFPackageName11.intValue());
//
//		MgcpEvent DTMFMgcpEvent11 = DTMFEvent11.getEventIdentifier();
//		assertNotNull(DTMFMgcpEvent11);
//		assertEquals("1", DTMFMgcpEvent11.getName());
//
//		// requestedEvent.getEventName()
//
//	}

//	public void testCreateConnectionDecode() {
//		String command = "CRCX 1204 aaln/1@rgw-2567.whatever.net MGCP 1.0\nC: A3C47F21456789F0\nL: p:10, a:PCMU\nM: recvonly";
//		System.out.println(command);
//
//		CreateConnection createConnectionCommand = null;
//		CreateConnectionHandler crcxHandler = new CreateConnectionHandler(jainMgcpStack, inetAddress, port);
//		try {
//			createConnectionCommand = (CreateConnection) crcxHandler.decodeCommand(command);
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			fail("Parsing of CRCX failed");
//		}
//
//		assertNotNull(" CRCX Command created", createConnectionCommand);
//		assertEquals(new Integer(Constants.CMD_CREATE_CONNECTION), new Integer(createConnectionCommand
//				.getObjectIdentifier()));
//
//		EndpointIdentifier endpointIdentifier = createConnectionCommand.getEndpointIdentifier();
//		assertEquals("aaln/1", endpointIdentifier.getLocalEndpointName());
//		assertEquals("rgw-2567.whatever.net", endpointIdentifier.getDomainName());
//
//		int tx = createConnectionCommand.getTransactionHandle();
//		assertEquals(1204, tx);
//
//		CallIdentifier C = createConnectionCommand.getCallIdentifier();
//		assertNotNull(" CRCX Command CallIdentifier", C);
//		assertEquals("A3C47F21456789F0", C.toString());
//
//		LocalOptionValue[] localOptionValues = createConnectionCommand.getLocalConnectionOptions();
//
//		PacketizationPeriod p = (PacketizationPeriod) localOptionValues[0];
//		assertEquals(10, p.getPacketizationPeriodLowerBound());
//		assertEquals(10, p.getPacketizationPeriodUpperBound());
//
//		CompressionAlgorithm c = (CompressionAlgorithm) localOptionValues[1];
//		String[] names = c.getCompressionAlgorithmNames();
//		assertNotNull(names);
//		assertEquals("PCMU", names[0]);
//
//		ConnectionMode M = createConnectionCommand.getMode();
//		assertNotNull(M);
//		assertEquals(ConnectionMode.RECVONLY, M.getConnectionModeValue());
//
//	}

	public void testCreateConnectionResponseDecode() {
		String response = "200 1204 OK\nI: FDE234C8\n\nv=0\no=- 25678 753849 IN IP4 128.96.41.1\ns=-\nc=IN IP4 128.96.41.1\nt=0 0\nm=audio 3456 RTP/AVP 0";

		CreateConnectionResponse createConnectionResponse = null;
		CreateConnectionHandler crcxHandler = new CreateConnectionHandler(jainMgcpStack, inetAddress, port);
		try {
			createConnectionResponse = (CreateConnectionResponse) crcxHandler.decodeResponse(response);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Parsing of CRCX Response failed");
		}

		assertNotNull(" CRCX Response created", createConnectionResponse);
		assertEquals(1204, createConnectionResponse.getTransactionHandle());

		ReturnCode returnCode = createConnectionResponse.getReturnCode();
		assertNotNull(returnCode);
		assertEquals(ReturnCode.TRANSACTION_EXECUTED_NORMALLY, returnCode.getValue());

		ConnectionIdentifier I = createConnectionResponse.getConnectionIdentifier();
		assertNotNull(I);
		assertEquals("FDE234C8", I.toString());

		ConnectionDescriptor connectionDescriptor = createConnectionResponse.getLocalConnectionDescriptor();
		assertNotNull(connectionDescriptor);

		// TODO : Add the JAIN SIP SDP Api for testing
		System.out.println(connectionDescriptor.toString());

	}

	public void testCreateConnectionProvisionalResponseDecode() {
		String response = "100 1206 Pending\nI: DFE233D1\nv=0\no=- 4723891 7428910 IN IP4 128.96.63.25\ns=-\nc=IN IP4 128.96.63.25\nt=0 0\nm=audio 3456 RTP/AVP 0";

		CreateConnectionResponse createConnectionProvisionalResponse = null;
		CreateConnectionHandler crcxHandler = new CreateConnectionHandler(jainMgcpStack, inetAddress, port);
		try {
			createConnectionProvisionalResponse = (CreateConnectionResponse) crcxHandler.decodeResponse(response);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Parsing of CRCX Response failed");
		}

		assertNotNull(" CRCX Provisional Response created", createConnectionProvisionalResponse);
		assertEquals(1206, createConnectionProvisionalResponse.getTransactionHandle());

		ReturnCode returnCode = createConnectionProvisionalResponse.getReturnCode();
		assertNotNull(returnCode);
		assertEquals(ReturnCode.TRANSACTION_BEING_EXECUTED, returnCode.getValue());

	}

//	public void testModifyConnectionDecode() {
//		String command = "MDCX 1209 aaln/1@rgw-2567.whatever.net MGCP 1.0\nC: A3C47F21456789F0\nI: FDE234C8\nN: ca@ca1.whatever.net\nM: sendrecv";
//
//		ModifyConnection modifyConnectionCommand = null;
//		ModifyConnectionHandler mdcxHandler = new ModifyConnectionHandler(jainMgcpStack, inetAddress, port);
//		try {
//			modifyConnectionCommand = (ModifyConnection) mdcxHandler.decodeCommand(command);
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			fail("Parsing of CRCX failed");
//		}
//
//		assertNotNull(" MDCX Command created", modifyConnectionCommand);
//		assertEquals(new Integer(Constants.CMD_MODIFY_CONNECTION), new Integer(modifyConnectionCommand
//				.getObjectIdentifier()));
//
//		EndpointIdentifier endpointIdentifier = modifyConnectionCommand.getEndpointIdentifier();
//		assertEquals("aaln/1", endpointIdentifier.getLocalEndpointName());
//		assertEquals("rgw-2567.whatever.net", endpointIdentifier.getDomainName());
//
//		int tx = modifyConnectionCommand.getTransactionHandle();
//		assertEquals(1209, tx);
//
//		CallIdentifier C = modifyConnectionCommand.getCallIdentifier();
//		assertNotNull(" MDCX Command CallIdentifier", C);
//		assertEquals("A3C47F21456789F0", C.toString());
//
//		ConnectionIdentifier I = modifyConnectionCommand.getConnectionIdentifier();
//		assertNotNull(I);
//		assertEquals("FDE234C8", I.toString());
//		
//		NotifiedEntity N = modifyConnectionCommand.getNotifiedEntity();
//		assertNotNull(" MDCX NotifiedEntity", N);
//		assertEquals(N.getLocalName(), "ca");
//		assertEquals(N.getDomainName(), "ca1.whatever.net");
//		
//
//		ConnectionMode M = modifyConnectionCommand.getMode();
//		assertNotNull(M);
//		assertEquals(ConnectionMode.SENDRECV, M.getConnectionModeValue());
//	}
	
	
//	public void testDeleteConnectionDecode() {
//		String command = "DLCX 1210 aaln/1@rgw-2567.whatever.net MGCP 1.0\nC: A3C47F21456789F0\nI: FDE234C8\n";
//
//		DeleteConnection deleteConnectionCommand = null;
//		DeleteConnectionHandler dlcxHandler = new DeleteConnectionHandler(jainMgcpStack, inetAddress, port);
//		try {
//			deleteConnectionCommand = (DeleteConnection) dlcxHandler.decodeCommand(command);
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			fail("Parsing of CRCX failed");
//		}
//
//		assertNotNull(" DLCX Command created", deleteConnectionCommand);
//		assertEquals(new Integer(Constants.CMD_DELETE_CONNECTION), new Integer(deleteConnectionCommand
//				.getObjectIdentifier()));
//
//		EndpointIdentifier endpointIdentifier = deleteConnectionCommand.getEndpointIdentifier();
//		assertEquals("aaln/1", endpointIdentifier.getLocalEndpointName());
//		assertEquals("rgw-2567.whatever.net", endpointIdentifier.getDomainName());
//
//		int tx = deleteConnectionCommand.getTransactionHandle();
//		assertEquals(1210, tx);
//
//		CallIdentifier C = deleteConnectionCommand.getCallIdentifier();
//		assertNotNull(" DLCX Command CallIdentifier", C);
//		assertEquals("A3C47F21456789F0", C.toString());
//
//		ConnectionIdentifier I = deleteConnectionCommand.getConnectionIdentifier();
//		assertNotNull(I);
//		assertEquals("FDE234C8", I.toString());
//		
//	}	
	
	public void testDeleteConnectionResponseDecode() {
		String response = "250 1210 OK\nP: PS=1245, OS=62345, PR=780, OR=45123, PL=10, JI=27, LA=48";

		DeleteConnectionResponse deleteConnectionResponse = null;
		DeleteConnectionHandler dlcxHandler = new DeleteConnectionHandler(jainMgcpStack, inetAddress, port);
		try {
			deleteConnectionResponse = (DeleteConnectionResponse) dlcxHandler.decodeResponse(response);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Parsing of CRCX Response failed");
		}

		assertNotNull(" DLCX Response created", deleteConnectionResponse);
		assertEquals(1210, deleteConnectionResponse.getTransactionHandle());

		ReturnCode returnCode = deleteConnectionResponse.getReturnCode();
		assertNotNull(returnCode);
		assertEquals(ReturnCode.CONNECTION_WAS_DELETED, returnCode.getValue());

		

	}	
}
