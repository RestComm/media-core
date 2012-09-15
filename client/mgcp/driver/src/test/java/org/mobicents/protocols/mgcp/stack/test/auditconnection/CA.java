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
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.ConnectionParm;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.InfoCode;
import jain.protocol.ip.mgcp.message.parms.LocalOptionValue;
import jain.protocol.ip.mgcp.message.parms.PacketizationPeriod;
import jain.protocol.ip.mgcp.message.parms.RegularConnectionParm;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.stack.JainMgcpExtendedListener;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackProviderImpl;
import org.mobicents.protocols.mgcp.stack.test.auditendpoint.AuditEndpointTest;

public class CA implements JainMgcpExtendedListener {

	private static Logger logger = Logger.getLogger(CA.class);

	private JainMgcpStackProviderImpl caProvider;
	private int mgStack = 0;
	private boolean responseReceived = false;

	public CA(JainMgcpStackProviderImpl caProvider, JainMgcpStackProviderImpl mgwProvider) {
		this.caProvider = caProvider;
		mgStack = mgwProvider.getJainMgcpStack().getPort();
	}

	public void sendAuditConnection() {

		try {
			caProvider.addJainMgcpListener(this);

			EndpointIdentifier endpointID = new EndpointIdentifier("media/trunk/Announcement/enp-1", "127.0.0.1:"
					+ mgStack);

			ConnectionIdentifier connectionIdentifier = new ConnectionIdentifier("1");

			InfoCode[] infoCodeList = new InfoCode[] { InfoCode.CallIdentifier, InfoCode.NotifiedEntity,
					InfoCode.LocalConnectionOptions, InfoCode.ConnectionMode, InfoCode.ConnectionParameters,
					InfoCode.RemoteConnectionDescriptor, InfoCode.LocalConnectionDescriptor };

			AuditConnection auditConnection = new AuditConnection(this, endpointID, connectionIdentifier, infoCodeList);

			auditConnection.setTransactionHandle(caProvider.getUniqueTransactionHandler());

			caProvider.sendMgcpEvents(new JainMgcpEvent[] { auditConnection });

			logger.debug(" AuditConnection command sent for TxId " + auditConnection.getTransactionHandle());
		} catch (Exception e) {
			e.printStackTrace();
			AuditEndpointTest.fail("Unexpected Exception");
		}
	}

	public void checkState() {
		AuditEndpointTest.assertTrue("Expect to receive AUEP Response", responseReceived);

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
		//logger.debug("processMgcpResponseEvent = " + jainmgcpresponseevent);
		switch (jainmgcpresponseevent.getObjectIdentifier()) {
		case Constants.RESP_AUDIT_CONNECTION:
			AuditConnectionResponse response = (AuditConnectionResponse) jainmgcpresponseevent;
			
			AuditConnectionTest.assertNotNull(response.getCallIdentifier());
			//AuditConnectionTest.assertEquals("1", response.getCallIdentifier().toString());
			
			AuditConnectionTest.assertEquals("127.0.0.1",response.getNotifiedEntity().getDomainName());
			AuditConnectionTest.assertEquals(2427,response.getNotifiedEntity().getPortNumber());
			
			LocalOptionValue[] localConnectionOptions = response.getLocalConnectionOptions();
			AuditConnectionTest.assertNotNull(localConnectionOptions);
			AuditConnectionTest.assertEquals(2, localConnectionOptions.length);
			
			AuditConnectionTest.assertEquals(10, ((PacketizationPeriod)localConnectionOptions[0]).getPacketizationPeriodLowerBound());
			AuditConnectionTest.assertEquals(10, ((PacketizationPeriod)localConnectionOptions[0]).getPacketizationPeriodUpperBound());
			
			CompressionAlgorithm compressionAlgorithm = (CompressionAlgorithm)localConnectionOptions[1];
			AuditConnectionTest.assertNotNull(compressionAlgorithm);
			
			String[] names = compressionAlgorithm.getCompressionAlgorithmNames();
			AuditConnectionTest.assertNotNull(names);
			AuditConnectionTest.assertEquals(2, names.length);
			AuditConnectionTest.assertEquals("PCMU", names[0]);
			AuditConnectionTest.assertEquals("G729", names[1]);
			
			AuditConnectionTest.assertEquals(ConnectionMode.SENDRECV, response.getMode().getConnectionModeValue());
			
			ConnectionParm[] connectionParms = response.getConnectionParms();
			AuditConnectionTest.assertEquals(3, connectionParms.length);
			AuditConnectionTest.assertEquals(RegularConnectionParm.PACKETS_SENT, connectionParms[0].getConnectionParmType());
			AuditConnectionTest.assertEquals(100, connectionParms[0].getConnectionParmValue());
			
			AuditConnectionTest.assertEquals(RegularConnectionParm.OCTETS_SENT, connectionParms[1].getConnectionParmType());
			AuditConnectionTest.assertEquals(1110, connectionParms[1].getConnectionParmValue());
			
			AuditConnectionTest.assertEquals(RegularConnectionParm.JITTER, connectionParms[2].getConnectionParmType());
			AuditConnectionTest.assertEquals(26, connectionParms[2].getConnectionParmValue());
			
			logger.info(" The LocalConnection Descriptor = "+response.getLocalConnectionDescriptor());
			logger.info(" The RemoteConnection Descriptor = "+response.getRemoteConnectionDescriptor());
			
			responseReceived = true;
			break;
		default:
			logger.warn("This RESPONSE is unexpected " + jainmgcpresponseevent);
			break;

		}

	}

}
