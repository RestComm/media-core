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
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.NotificationRequestResponse;
import jain.protocol.ip.mgcp.message.parms.DigitMap;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.parser.MgcpContentHandler;
import org.mobicents.protocols.mgcp.parser.MgcpMessageParser;
import org.mobicents.protocols.mgcp.parser.Utils;

/**
 * @author Oleg Kulikov
 * @author Amit Bhayani
 * 
 */

public class NotificationRequestHandler extends TransactionHandler {

	private static final Logger logger = Logger.getLogger(NotificationRequestHandler.class);

	private NotificationRequest command;
	private NotificationRequestResponse response;

	public NotificationRequestHandler(JainMgcpStackImpl stack) {
		super(stack);
	}

	public NotificationRequestHandler(JainMgcpStackImpl stack, InetAddress address, int port) {
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
		NotifiedEntity notifiedEntity = command.getNotifiedEntity();
		if (command.getNotifiedEntity() != null) {
			this.stack.provider.setNotifiedEntity(notifiedEntity);
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
			logger.error("Decode RQNT Response failed", e);
		} finally {
			utilsFactory.deallocate(utils);
		}

		return response;
	}

	@Override
	public String encode(JainMgcpCommandEvent event) {
		Utils utils = utilsFactory.allocate();
		NotificationRequest req = (NotificationRequest) event;
		StringBuffer buffer = new StringBuffer();

		buffer.append("RQNT ").append(event.getTransactionHandle()).append(SINGLE_CHAR_SPACE).append(
				req.getEndpointIdentifier()).append(SINGLE_CHAR_SPACE).append(MGCP_VERSION).append(NEW_LINE);

		if (req.getNotifiedEntity() != null) {
			buffer.append("N:").append(req.getNotifiedEntity()).append(NEW_LINE);
		}

		buffer.append("X:").append(req.getRequestIdentifier()).append(NEW_LINE);

		if (req.getDigitMap() != null) {
			buffer.append("D:").append(req.getDigitMap()).append(NEW_LINE);
		}

		if (req.getSignalRequests() != null) {
			buffer.append("S:").append(utils.encodeEventNames(req.getSignalRequests())).append(NEW_LINE);
		}

		if (req.getRequestedEvents() != null) {
			buffer.append("R:").append(utils.encodeRequestedEvents(req.getRequestedEvents())).append(NEW_LINE);
		}

		if (req.getDetectEvents() != null) {
			buffer.append("T:").append(utils.encodeEventNames(req.getDetectEvents())).append(NEW_LINE);
		}
		utilsFactory.deallocate(utils);
		return buffer.toString();
	}

	@Override
	public String encode(JainMgcpResponseEvent event) {

		NotificationRequestResponse response = (NotificationRequestResponse) event;
		ReturnCode returnCode = response.getReturnCode();

		String encodedEvent = (new StringBuffer().append(returnCode.getValue()).append(SINGLE_CHAR_SPACE).append(
				response.getTransactionHandle()).append(SINGLE_CHAR_SPACE).append(returnCode.getComment())
				.append(NEW_LINE)).toString();

		return encodedEvent;
	}

	private class CommandContentHandle implements MgcpContentHandler {
		private Utils utils = null;

		public CommandContentHandle(Utils utils) {
			this.utils = utils;
		}

		public void header(String header) throws ParseException {

			command = new NotificationRequest(source != null ? source : stack, endpoint, new RequestIdentifier("0"));
			command.setTransactionHandle(remoteTID);
		}

		public void param(String name, String value) throws ParseException {
			if (name.equalsIgnoreCase("N")) {
				command.setNotifiedEntity(utils.decodeNotifiedEntity(value, true));
			} else if (name.equalsIgnoreCase("X")) {
				command.setRequestIdentifier(new RequestIdentifier(value));
			} else if (name.equalsIgnoreCase("R")) {
				command.setRequestedEvents(utils.decodeRequestedEventList(value));
			} else if (name.equalsIgnoreCase("S")) {
				command.setSignalRequests(utils.decodeEventNames(value));
			} else if (name.equalsIgnoreCase("T")) {
				command.setDetectEvents(utils.decodeEventNames(value));
			} else if (name.equalsIgnoreCase("D")) {
				command.setDigitMap(new DigitMap(value));
			}
		}

		public void sessionDescription(String sd) throws ParseException {
			throw new UnsupportedOperationException("Not supported yet.");
		}
	}

	private class ResponseContentHandle implements MgcpContentHandler {
		private Utils utils;

		public ResponseContentHandle(Utils utils) {
			this.utils = utils;
		}

		public void header(String header) throws ParseException {
			String[] tokens = utils.splitStringBySpace(header);

			int tid = Integer.parseInt(tokens[1]);
			response = new NotificationRequestResponse(source != null ? source : stack, utils.decodeReturnCode(Integer
					.parseInt(tokens[0])));
			response.setTransactionHandle(tid);
		}

		public void param(String name, String value) throws ParseException {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public void sessionDescription(String sd) throws ParseException {
			throw new UnsupportedOperationException("Not supported yet.");
		}
	}

	@Override
	public JainMgcpResponseEvent getProvisionalResponse() {
		NotificationRequestResponse provisionalresponse = null;
		if (!sent) {
			provisionalresponse = new NotificationRequestResponse(source != null ? source : stack,
					ReturnCode.Transaction_Being_Executed);
		}
		return provisionalresponse;

	}

}
