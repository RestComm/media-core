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

package org.mobicents.protocols.mgcp.stack;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.AuditConnection;
import jain.protocol.ip.mgcp.message.AuditConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.ConnectionParm;
import jain.protocol.ip.mgcp.message.parms.InfoCode;
import jain.protocol.ip.mgcp.message.parms.LocalOptionValue;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.text.ParseException;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.parser.MgcpContentHandler;
import org.mobicents.protocols.mgcp.parser.MgcpMessageParser;
import org.mobicents.protocols.mgcp.parser.Utils;

/**
 * 
 * @author amit bhayani
 * 
 */
public class AuditConnectionHandler extends TransactionHandler {

	private static final Logger logger = Logger.getLogger(AuditConnectionHandler.class);

	private AuditConnection command;
	private AuditConnectionResponse response;
	private ConnectionIdentifier connectionIdentifier = null;

	private InfoCode[] requestedInfo = null;

	boolean RCfirst = false;

	public AuditConnectionHandler(JainMgcpStackImpl stack) {
		super(stack);
	}

	public AuditConnectionHandler(JainMgcpStackImpl stack, InetAddress address, int port) {
		super(stack, address, port);
	}

	@Override
	public JainMgcpCommandEvent decodeCommand(String message) throws ParseException {
		Utils utils = utilsFactory.allocate();
		MgcpMessageParser parser = new MgcpMessageParser(new CommandContentHandle(utils));
		try {
			parser.parse(message);
			command = new AuditConnection(source != null ? source : stack, endpoint, connectionIdentifier, requestedInfo);
			command.setTransactionHandle(remoteTID);
		} catch (Exception e) {
			throw new ParseException(e.getMessage(), -1);
		} finally {
			utilsFactory.deallocate(utils);
		}
		return command;
	}

	@Override
	public JainMgcpResponseEvent decodeResponse(String message) throws ParseException {
		Utils utils = utilsFactory.allocate();
		MgcpMessageParser parser = new MgcpMessageParser(new ResponseContentHandle(utils));
		try {
			parser.parse(message);
		} catch (IOException e) {
			logger.error("Parsing of AUCX Response failed ", e);
		} finally {
			utilsFactory.deallocate(utils);
		}
		return response;
	}

	@Override
	public String encode(JainMgcpCommandEvent event) {

		// encode message header
		Utils utils = utilsFactory.allocate();
		AuditConnection evt = (AuditConnection) event;
		StringBuffer s = new StringBuffer();
		s.append("AUCX ").append(evt.getTransactionHandle()).append(TransactionHandler.SINGLE_CHAR_SPACE).append(
				evt.getEndpointIdentifier()).append(MGCP_VERSION).append(NEW_LINE);

		// encode mandatory parameters
		if (evt.getConnectionIdentifier() != null) {
			s.append("I:").append(evt.getConnectionIdentifier()).append(NEW_LINE);
		}

		InfoCode[] requestedInfos = evt.getRequestedInfo();

		if (requestedInfos != null) {
			s.append("F: ").append(utils.encodeInfoCodeList(requestedInfos));
			int foundRC = 0;
			int foundLC = 0;

			// This is to determine which SDP is RemoteSDP and which one is
			// LocalSDP
			for (int count = 0; count < requestedInfos.length; count++) {
				InfoCode info = requestedInfos[count];
				switch (info.getInfoCode()) {
				case (InfoCode.REMOTE_CONNECTION_DESCRIPTOR):
					foundRC = count;
					if (foundLC != 0 && foundLC < count) {
						RCfirst = false;
					} else {
						RCfirst = true;
					}
					break;

				case (InfoCode.LOCAL_CONNECTION_DESCRIPTOR):
					foundLC = count;
					if (foundRC != 0 && foundRC < count) {
						RCfirst = true;
					} else {
						RCfirst = false;
					}
					break;
				}
			}

		}
		utilsFactory.deallocate(utils);
		// return msg;
		return s.toString();
	}

	@Override
	public String encode(JainMgcpResponseEvent event) {
		Utils utils = utilsFactory.allocate();

		AuditConnectionResponse response = (AuditConnectionResponse) event;
		ReturnCode returnCode = response.getReturnCode();

		StringBuffer s = new StringBuffer();
		s.append(returnCode.getValue()).append(SINGLE_CHAR_SPACE).append(response.getTransactionHandle()).append(
				SINGLE_CHAR_SPACE).append(returnCode.getComment()).append(NEW_LINE);

		if (response.getCallIdentifier() != null) {
			s.append("C:").append(response.getCallIdentifier()).append(NEW_LINE);
		}

		if (response.getNotifiedEntity() != null) {
			s.append("N:").append(utils.encodeNotifiedEntity(response.getNotifiedEntity())).append(NEW_LINE);
		}

		if (response.getLocalConnectionOptions() != null) {
			s.append("L:").append(utils.encodeLocalOptionValueList(response.getLocalConnectionOptions())).append(
					NEW_LINE);
		}

		if (response.getMode() != null) {
			s.append("M:").append(response.getMode()).append(NEW_LINE);
		}

		if (response.getConnectionParms() != null) {
			s.append("P:").append(utils.encodeConnectionParms(response.getConnectionParms())).append(NEW_LINE);
		}

		if (RCfirst && response.getRemoteConnectionDescriptor() != null) {
			s.append(NEW_LINE).append(response.getRemoteConnectionDescriptor()).append(NEW_LINE);
		}

		if (response.getLocalConnectionDescriptor() != null) {
			s.append(NEW_LINE).append(response.getLocalConnectionDescriptor()).append(NEW_LINE);
		}

		if (!RCfirst && response.getRemoteConnectionDescriptor() != null) {
			s.append(NEW_LINE).append(response.getRemoteConnectionDescriptor()).append(NEW_LINE);
		}
		utilsFactory.deallocate(utils);
		return s.toString();
		// return msg;
	}

	@Override
	public JainMgcpResponseEvent getProvisionalResponse() {
		AuditConnectionResponse provisionalResponse = null;

		if (!sent) {
			provisionalResponse = new AuditConnectionResponse(source != null ? source : stack,
					ReturnCode.Transaction_Being_Executed);
			provisionalResponse.setTransactionHandle(remoteTID);
		}

		return provisionalResponse;
	}

	private class CommandContentHandle implements MgcpContentHandler {
		Utils utils = null;

		public CommandContentHandle(Utils utils) {
			this.utils = utils;
		}

		/**
		 * Receive notification of the header of a message. Parser will call
		 * this method to report about header reading.
		 * 
		 * @param header
		 *            the header from the message.
		 */
		public void header(String header) throws ParseException {
			// Can't create the AuditConnection object here as
			// ConnectionIdentifier and InfoCode[] is required

		}

		/**
		 * Receive notification of the parameter of a message. Parser will call
		 * this method to report about parameter reading.
		 * 
		 * @param name
		 *            the name of the parameter
		 * @param value
		 *            the value of the parameter.
		 */
		public void param(String name, String value) throws ParseException {
			if (name.equalsIgnoreCase("I")) {
				connectionIdentifier = new ConnectionIdentifier(value);
			} else if (name.equalsIgnoreCase("F")) {

				int RCindex = value.indexOf("RC");
				int LCindex = value.indexOf("LC");

				if (RCindex != -1 && RCindex < LCindex) {
					RCfirst = true;
				}

				requestedInfo = utils.decodeInfoCodeList(value);
			} else {
				logger.error("Unknown code while encoding AUCX Command name = " + name + " value = " + value);
			}
		}

		/**
		 * Receive notification of the session description. Parser will call
		 * this method to report about session descriptor reading.
		 * 
		 * @param sd
		 *            the session description from message.
		 */
		public void sessionDescription(String sd) throws ParseException {
			throw new ParseException("SessionDescription shouldn't have been included in AUCX command", 0);
		}
	}

	private class ResponseContentHandle implements MgcpContentHandler {
		Utils utils = null;

		public ResponseContentHandle(Utils utils) {
			this.utils = utils;
		}

		/**
		 * Receive notification of the header of a message. Parser will call
		 * this method to report about header reading.
		 * 
		 * @param header
		 *            the header from the message.
		 */
		public void header(String header) throws ParseException {
			String[] tokens = utils.splitStringBySpace(header);

			int tid = Integer.parseInt(tokens[1]);
			response = new AuditConnectionResponse(source != null ? source : stack, utils.decodeReturnCode(Integer
					.parseInt(tokens[0])));
			response.setTransactionHandle(tid);
		}

		/**
		 * Receive notification of the parameter of a message. Parser will call
		 * this method to report about parameter reading.
		 * 
		 * @param name
		 *            the name of the paremeter
		 * @param value
		 *            the value of the parameter.
		 */
		public void param(String name, String value) throws ParseException {
			if (name.equalsIgnoreCase("C")) {
				response.setCallIdentifier(new CallIdentifier(value));
			} else if (name.equalsIgnoreCase("N")) {
				NotifiedEntity n = utils.decodeNotifiedEntity(value, true);
				response.setNotifiedEntity(n);
			} else if (name.equalsIgnoreCase("L")) {
				LocalOptionValue[] LocalOptionValueList = utils.decodeLocalOptionValueList(value);
				response.setLocalConnectionOptions(LocalOptionValueList);
			} else if (name.equalsIgnoreCase("M")) {
				ConnectionMode connectionMode = utils.decodeConnectionMode(value);
				response.setMode(connectionMode);
			} else if (name.equalsIgnoreCase("P")) {
				ConnectionParm[] connectionParms = utils.decodeConnectionParms(value);
				response.setConnectionParms(connectionParms);
			} else {
				logger.warn("Unidentified AUCX Response parameter " + name + " with value = " + value);
			}
		}

		/**
		 * Receive notification of the session description. Parser will call
		 * this method to report about session descriptor reading.
		 * 
		 * @param sd
		 *            the session description from message.
		 */
		public void sessionDescription(String sd) throws ParseException {

			StringReader stringReader = new StringReader(sd);
			BufferedReader reader = new BufferedReader(stringReader);

			String line = null;
			boolean sdpPresent = false;
			String sdp1 = "";
			String sdp2 = "";
			try {
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					sdpPresent = line.length() == 0;
					if (sdpPresent)
						break;
					sdp1 = sdp1 + line.trim() + "\r\n";

				}

				while ((line = reader.readLine()) != null) {
					line = line.trim();
					sdp2 = sdp2 + line.trim() + "\r\n";
				}

			} catch (IOException e) {
				logger.error("Error while reading the SDP for AUCX Response and decoding to AUCX command ", e);
			}

			if (RCfirst) {
				response.setRemoteConnectionDescriptor(new ConnectionDescriptor(sdp1));
				if (!sdp2.equals("")) {
					response.setLocalConnectionDescriptor(new ConnectionDescriptor(sdp2));
				}

			} else {
				response.setLocalConnectionDescriptor(new ConnectionDescriptor(sdp1));
				if (!sdp2.equals("")) {
					response.setRemoteConnectionDescriptor(new ConnectionDescriptor(sdp2));
				}
			}

		}
	}

}
