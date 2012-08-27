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
import jain.protocol.ip.mgcp.message.AuditEndpoint;
import jain.protocol.ip.mgcp.message.AuditEndpointResponse;
import jain.protocol.ip.mgcp.message.parms.BearerInformation;
import jain.protocol.ip.mgcp.message.parms.CapabilityValue;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.DigitMap;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.InfoCode;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.ReasonCode;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.message.parms.RestartMethod;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.parser.MgcpContentHandler;
import org.mobicents.protocols.mgcp.parser.MgcpMessageParser;
import org.mobicents.protocols.mgcp.parser.Utils;

/**
 * 
 * @author amit bhayani
 * 
 */
public class AuditEndpointHandler extends TransactionHandler {

	private static final Logger logger = Logger.getLogger(AuditEndpointHandler.class);

	private AuditEndpoint command;
	private AuditEndpointResponse response;

	public AuditEndpointHandler(JainMgcpStackImpl stack) {
		super(stack);
	}

	public AuditEndpointHandler(JainMgcpStackImpl stack, InetAddress address, int port) {
		super(stack, address, port);
	}

	@Override
	public JainMgcpCommandEvent decodeCommand(String message) throws ParseException {
		Utils utils = utilsFactory.allocate();
		MgcpMessageParser parser = new MgcpMessageParser(new CommandContentHandle(utils));
		try {
			parser.parse(message);
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
			logger.error("Decoding of AUEP Response failed", e);
		} finally {
			utilsFactory.deallocate(utils);
		}

		return response;
	}

	@Override
	public String encode(JainMgcpCommandEvent event) {
		Utils utils = utilsFactory.allocate();
		// encode message header
		AuditEndpoint evt = (AuditEndpoint) event;
		StringBuffer s = new StringBuffer();
		s.append("AUEP ").append(evt.getTransactionHandle()).append(SINGLE_CHAR_SPACE).append(
				evt.getEndpointIdentifier()).append(MGCP_VERSION).append(NEW_LINE);

		// encode mandatory parameters
		InfoCode[] requestedInfos = evt.getRequestedInfo();
		if (requestedInfos != null) {
			s.append("F: ").append(utils.encodeInfoCodeList(requestedInfos));
		}
		utilsFactory.deallocate(utils);
		// return msg;
		return s.toString();
	}

	@Override
	public String encode(JainMgcpResponseEvent event) {
		Utils utils = utilsFactory.allocate();
		AuditEndpointResponse response = (AuditEndpointResponse) event;
		ReturnCode returnCode = response.getReturnCode();

		StringBuffer s = new StringBuffer();
		s.append(returnCode.getValue()).append(SINGLE_CHAR_SPACE).append(response.getTransactionHandle()).append(
				SINGLE_CHAR_SPACE).append(returnCode.getComment()).append(NEW_LINE);

		if (response.getCapabilities() != null) {
			// TODO How to insert a new line with A : for different set of
			// compression Algo?
			s.append("A: ").append(utils.encodeCapabilityList(response.getCapabilities())).append(NEW_LINE);
		}
		if (response.getBearerInformation() != null) {
			s.append("B: ").append(utils.encodeBearerInformation(response.getBearerInformation())).append(NEW_LINE);
		}
		ConnectionIdentifier[] connectionIdentifiers = response.getConnectionIdentifiers();
		if (connectionIdentifiers != null) {
			s.append("I: ");
			// msg += "I:";
			boolean first = true;
			for (int i = 0; i < connectionIdentifiers.length; i++) {
				if (first) {
					first = false;
				} else {
					s.append(",");
				}
				s.append(connectionIdentifiers[i].toString());
			}
			s.append(NEW_LINE);
		}
		if (response.getNotifiedEntity() != null) {
			s.append("N: ").append(utils.encodeNotifiedEntity(response.getNotifiedEntity())).append(NEW_LINE);
		}
		if (response.getRequestIdentifier() != null) {
			s.append("X: ").append(response.getRequestIdentifier()).append(NEW_LINE);
		}
		RequestedEvent[] r = response.getRequestedEvents();
		if (r != null) {
			s.append("R: ").append(utils.encodeRequestedEvents(r)).append(NEW_LINE);
		}
		EventName[] sEvet = response.getSignalRequests();
		if (sEvet != null) {
			s.append("S: ").append(utils.encodeEventNames(sEvet)).append(NEW_LINE);
		}
		if (response.getDigitMap() != null) {
			s.append("D: ").append(response.getDigitMap()).append(NEW_LINE);
		}
		EventName[] o = response.getObservedEvents();
		if (o != null) {
			s.append("O: ").append(utils.encodeEventNames(o)).append(NEW_LINE);
		}
		if (response.getReasonCode() != null) {
			s.append("E: ").append(response.getReasonCode()).append(NEW_LINE);

		}
		EventName[] t = response.getDetectEvents();
		if (t != null) {
			s.append("T: ").append(utils.encodeEventNames(t)).append(NEW_LINE);

		}
		EventName[] es = response.getEventStates();
		if (es != null) {
			s.append("ES: ").append(utils.encodeEventNames(es)).append(NEW_LINE);

		}
		if (response.getRestartMethod() != null) {
			s.append("RM: ").append(response.getRestartMethod()).append(NEW_LINE);

		}
		if (response.getRestartDelay() > 0) {
			s.append("RD: ").append(response.getRestartDelay()).append(NEW_LINE);

		}
		EndpointIdentifier[] z = response.getEndpointIdentifierList();
		if (z != null) {
			s.append("Z: ").append(utils.encodeEndpointIdentifiers(z)).append(NEW_LINE);

		}
		utilsFactory.deallocate(utils);
		return s.toString();

	}

	@Override
	public JainMgcpResponseEvent getProvisionalResponse() {
		AuditEndpointResponse provisionalResponse = null;

		if (!sent) {
			provisionalResponse = new AuditEndpointResponse(source != null ? source : stack,
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
			
			command = new AuditEndpoint(source != null ? source : stack, endpoint);
			command.setTransactionHandle(remoteTID);
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
			if (name.equalsIgnoreCase("F")) {
				command.setRequestedInfo(utils.decodeInfoCodeList(value));
			} else {
				logger.error("Unknown code " + name);
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
			throw new ParseException("SessionDescription shouldn't have been included in AUEP command", 0);
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
			response = new AuditEndpointResponse(source != null ? source : stack, utils.decodeReturnCode(Integer
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
			if (name.equals("Z")) {
				EndpointIdentifier[] endpointIdentifierList = utils.decodeEndpointIdentifiers(value);
				response.setEndpointIdentifierList(endpointIdentifierList);
			}
			if (name.equalsIgnoreCase("B")) {
				BearerInformation b = utils.decodeBearerInformation(value);
				response.setBearerInformation(b);
			} else if (name.equalsIgnoreCase("I")) {
				ConnectionIdentifier[] is = response.getConnectionIdentifiers();
				if (is == null) {
					ConnectionIdentifier i = new ConnectionIdentifier(value);
					response.setConnectionIdentifiers(new ConnectionIdentifier[] { i });
				} else {
					ArrayList<ConnectionIdentifier> arrayList = new ArrayList<ConnectionIdentifier>();
					Collections.addAll(arrayList, is);
					arrayList.add(new ConnectionIdentifier(value));

					ConnectionIdentifier[] temp = new ConnectionIdentifier[arrayList.size()];
					response.setConnectionIdentifiers(arrayList.toArray(temp));
				}
			} else if (name.equalsIgnoreCase("N")) {
				NotifiedEntity n = utils.decodeNotifiedEntity(value, true);
				response.setNotifiedEntity(n);
			} else if (name.equalsIgnoreCase("X")) {
				RequestIdentifier r = new RequestIdentifier(value);
				response.setRequestIdentifier(r);
			} else if (name.equalsIgnoreCase("R")) {
				RequestedEvent[] r = utils.decodeRequestedEventList(value);
				response.setRequestedEvents(r);

			} else if (name.equalsIgnoreCase("S")) {
				EventName[] s = utils.decodeEventNames(value);
				response.setSignalRequests(s);
			} else if (name.equalsIgnoreCase("D")) {
				DigitMap d = new DigitMap(value);
				response.setDigitMap(d);
			} else if (name.equalsIgnoreCase("O")) {
				EventName[] o = utils.decodeEventNames(value);
				response.setObservedEvents(o);
			} else if (name.equalsIgnoreCase("E")) {
				ReasonCode e = utils.decodeReasonCode(value);
				response.setReasonCode(e);
			} else if (name.equalsIgnoreCase("Q")) {
				// response.set

			} else if (name.equalsIgnoreCase("T")) {
				EventName[] t = utils.decodeEventNames(value);
				response.setDetectEvents(t);
			} else if (name.equalsIgnoreCase("A")) {

				CapabilityValue[] capabilities = response.getCapabilities();
				if (capabilities == null) {
					response.setCapabilities(utils.decodeCapabilityList(value));
				} else {
					CapabilityValue[] newCapability = utils.decodeCapabilityList(value);
					int size = capabilities.length + newCapability.length;
					CapabilityValue[] temp = new CapabilityValue[size];
					int count = 0;
					for (int i = 0; i < capabilities.length; i++) {
						temp[count] = capabilities[i];
						count++;
					}

					for (int j = 0; j < newCapability.length; j++) {
						temp[count] = newCapability[j];
						count++;
					}
					response.setCapabilities(temp);

				}

			} else if (name.equalsIgnoreCase("ES")) {
				EventName[] es = utils.decodeEventNames(value);
				response.setEventStates(es);

			} else if (name.equalsIgnoreCase("RM")) {
				RestartMethod rm = utils.decodeRestartMethod(value);
				response.setRestartMethod(rm);
			} else if (name.equalsIgnoreCase("RD")) {
				int restartDelay = 0;
				try {
					restartDelay = Integer.parseInt(value);
				} catch (NumberFormatException nfe) {
					logger.error("RD throws error " + value, nfe);
				}
				response.setRestartDelay(restartDelay);
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
			// response.setLocalConnectionDescriptor(new
			// ConnectionDescriptor(sd));
		}
	}

}
