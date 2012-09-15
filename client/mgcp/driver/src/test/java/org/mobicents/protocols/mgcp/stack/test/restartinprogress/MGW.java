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

package org.mobicents.protocols.mgcp.stack.test.restartinprogress;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.RestartInProgress;
import jain.protocol.ip.mgcp.message.RestartInProgressResponse;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.RestartMethod;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.stack.JainMgcpExtendedListener;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackProviderImpl;
import org.mobicents.protocols.mgcp.stack.test.auditendpoint.AuditEndpointTest;

public class MGW implements JainMgcpExtendedListener {

	private static Logger logger = Logger.getLogger(MGW.class);
	private boolean responseReceived = false;
	int caPort = 0;

	JainMgcpStackProviderImpl mgwProvider;

	public MGW(JainMgcpStackProviderImpl mgwProvider, int caPort) {
		this.mgwProvider = mgwProvider;
		this.caPort = caPort;
	}

	public void sendRestartInProgress() {

		try {
			this.mgwProvider.addJainMgcpListener(this);

			EndpointIdentifier endpointID = new EndpointIdentifier("media/trunk/Announcement/enp-1", "127.0.0.1:"
					+ caPort);

			RestartInProgress restartInProgress = new RestartInProgress(this, endpointID, RestartMethod.Graceful);

			restartInProgress.setTransactionHandle(mgwProvider.getUniqueTransactionHandler());

			mgwProvider.sendMgcpEvents(new JainMgcpEvent[] { restartInProgress });

			logger.debug(" RestartInProgress command sent for TxId " + restartInProgress.getTransactionHandle());
		} catch (Exception e) {
			e.printStackTrace();
			AuditEndpointTest.fail("Unexpected Exception");
		}
	}

	public void checkState() {
		RestartInProgressTest.assertTrue("Expect to Receive RSIP Response", responseReceived);
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
		logger.info("processMgcpResponseEvent " + jainmgcpresponseevent);

		switch (jainmgcpresponseevent.getObjectIdentifier()) {
		case Constants.RESP_RESTART_IN_PROGRESS:
			RestartInProgressResponse response = (RestartInProgressResponse) jainmgcpresponseevent;

			NotifiedEntity notifiedEntity = response.getNotifiedEntity();
			RestartInProgressTest.assertNotNull(notifiedEntity);
			RestartInProgressTest.assertEquals("127.0.0.1", notifiedEntity.getDomainName());
			responseReceived = true;
			break;
		default:
			logger.warn("This RESPONSE is unexpected " + jainmgcpresponseevent);
			break;
		}

	}

}
