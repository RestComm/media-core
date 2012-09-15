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

package org.mobicents.protocols.mgcp.stack.test.createconnection;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.util.TooManyListenersException;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.stack.JainMgcpExtendedListener;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackProviderImpl;

public class MGW implements JainMgcpExtendedListener {

	private static Logger logger = Logger.getLogger(MGW.class);
	private boolean responseSent = false;

	private boolean sendFailedResponse = false;

	JainMgcpStackProviderImpl mgwProvider;

	public MGW(JainMgcpStackProviderImpl mgwProvider, boolean sendFailedResponse) {
		this(mgwProvider);
		this.sendFailedResponse = sendFailedResponse;		
	}

	public MGW(JainMgcpStackProviderImpl mgwProvider) {
		this.mgwProvider = mgwProvider;
		try {
			this.mgwProvider.addJainMgcpListener(this);
		} catch (TooManyListenersException e) {
			e.printStackTrace();
			CreateConnectionTest.fail("Unexpected Exception");
		}
	}

	public void checkState() {
		CreateConnectionTest.assertTrue("Expect to sent CRCX Response", responseSent);
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
		case Constants.CMD_CREATE_CONNECTION:
			
			CreateConnectionResponse response = null;

			if (this.sendFailedResponse) {
				response = new CreateConnectionResponse(jainmgcpcommandevent.getSource(),
						ReturnCode.Endpoint_Unknown, new ConnectionIdentifier("0"));
				response.setTransactionHandle(jainmgcpcommandevent.getTransactionHandle());
			} else {
				String identifier = ((CallIdentifier) mgwProvider.getUniqueCallIdentifier()).toString();
				ConnectionIdentifier connectionIdentifier = new ConnectionIdentifier(identifier);

				response = new CreateConnectionResponse(jainmgcpcommandevent.getSource(),
						ReturnCode.Transaction_Executed_Normally, connectionIdentifier);

				response.setTransactionHandle(jainmgcpcommandevent.getTransactionHandle());
				try {
					// FIXME: we asume there is wildcard - "any of"
					CreateConnection cc = (CreateConnection) jainmgcpcommandevent;
					EndpointIdentifier wildcard = cc.getEndpointIdentifier();
					EndpointIdentifier specific = new EndpointIdentifier(wildcard.getLocalEndpointName().replace("$",
							"")
							+ "test-1", wildcard.getDomainName());
					response.setSpecificEndpointIdentifier(specific);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

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
