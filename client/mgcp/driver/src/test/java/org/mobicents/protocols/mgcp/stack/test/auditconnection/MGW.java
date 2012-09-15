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

package org.mobicents.protocols.mgcp.stack.test.auditconnection;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.AuditConnection;
import jain.protocol.ip.mgcp.message.AuditConnectionResponse;
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.parms.CompressionAlgorithm;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.ConnectionParm;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.InfoCode;
import jain.protocol.ip.mgcp.message.parms.LocalOptionValue;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.PacketizationPeriod;
import jain.protocol.ip.mgcp.message.parms.RegularConnectionParm;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.util.TooManyListenersException;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.stack.JainMgcpExtendedListener;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackProviderImpl;

public class MGW implements JainMgcpExtendedListener {

	private static Logger logger = Logger.getLogger(MGW.class);
	private boolean responseSent = false;
	int caPort = 0;

	JainMgcpStackProviderImpl mgwProvider;

	public MGW(JainMgcpStackProviderImpl mgwProvider, int caPort) {
		this.mgwProvider = mgwProvider;
		this.caPort = caPort;
		try {
			this.mgwProvider.addJainMgcpListener(this);
		} catch (TooManyListenersException e) {
			e.printStackTrace();
			AuditConnectionTest.fail("Unexpected Exception");
		}
	}

	public void checkState() {
		AuditConnectionTest.assertTrue("Expect to sent CRCX Response", responseSent);
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

		switch (jainmgcpcommandevent.getObjectIdentifier()) {
		case Constants.CMD_AUDIT_CONNECTION:
			AuditConnection auditConnection = (AuditConnection) jainmgcpcommandevent;

			EndpointIdentifier endpointIdentifier = auditConnection.getEndpointIdentifier();
			AuditConnectionTest.assertNotNull(endpointIdentifier);
			AuditConnectionTest.assertEquals("media/trunk/Announcement/enp-1@127.0.0.1:2727", endpointIdentifier.toString());

			ConnectionIdentifier connectionIdentifier = auditConnection.getConnectionIdentifier();
			AuditConnectionTest.assertNotNull(connectionIdentifier);
			AuditConnectionTest.assertEquals("1", connectionIdentifier.toString());

			InfoCode[] infoCodeList = auditConnection.getRequestedInfo();
			AuditConnectionTest.assertEquals(7, infoCodeList.length);
			AuditConnectionTest.assertEquals(InfoCode.CALL_IDENTIFIER, infoCodeList[0].getInfoCode());
			AuditConnectionTest.assertEquals(InfoCode.NOTIFIED_ENTITY, infoCodeList[1].getInfoCode());
			AuditConnectionTest.assertEquals(InfoCode.LOCAL_CONNECTION_OPTIONS, infoCodeList[2].getInfoCode());
			AuditConnectionTest.assertEquals(InfoCode.CONNECTION_MODE, infoCodeList[3].getInfoCode());
			AuditConnectionTest.assertEquals(InfoCode.CONNECTION_PARAMETERS, infoCodeList[4].getInfoCode());
			AuditConnectionTest.assertEquals(InfoCode.REMOTE_CONNECTION_DESCRIPTOR, infoCodeList[5].getInfoCode());
			AuditConnectionTest.assertEquals(InfoCode.LOCAL_CONNECTION_DESCRIPTOR, infoCodeList[6].getInfoCode());

			AuditConnectionResponse response = new AuditConnectionResponse(jainmgcpcommandevent.getSource(),
					ReturnCode.Transaction_Executed_Normally);

			response.setCallIdentifier(mgwProvider.getUniqueCallIdentifier());
			NotifiedEntity notifiedEntity = new NotifiedEntity("127.0.0.1");
			response.setNotifiedEntity(notifiedEntity);

			LocalOptionValue[] localConnectionOptions = new LocalOptionValue[] { new PacketizationPeriod(10),
					new CompressionAlgorithm(new String[] { "PCMU", "G729" }) };
			response.setLocalConnectionOptions(localConnectionOptions);

			response.setMode(ConnectionMode.SendRecv);

			ConnectionParm[] connectionParms = new ConnectionParm[] {
					new RegularConnectionParm(RegularConnectionParm.PACKETS_SENT, 100),
					new RegularConnectionParm(RegularConnectionParm.OCTETS_SENT, 1110),
					new RegularConnectionParm(RegularConnectionParm.JITTER, 26) };
			response.setConnectionParms(connectionParms);

			response.setRemoteConnectionDescriptor(new ConnectionDescriptor("v=0"));
			response
					.setLocalConnectionDescriptor(new ConnectionDescriptor(
							"v=0 \n o=- 4723891 7428910 IN IP4 128.96.63.25 \n s=- \n c=IN IP4 128.96.63.25 \n t=0 0 \n m=audio 1296 RTP/AVP 0"));

			response.setTransactionHandle(jainmgcpcommandevent.getTransactionHandle());

			mgwProvider.sendMgcpEvents(new JainMgcpEvent[] { response });

			responseSent = true;

			break;
		default:
			logger.warn("This REQUEST is unexpected " + jainmgcpcommandevent);
			break;

		}

	}

	public void processMgcpResponseEvent(JainMgcpResponseEvent jainmgcpresponseevent) {
		logger.info("processMgcpResponseEvent " + jainmgcpresponseevent);

	}

}
