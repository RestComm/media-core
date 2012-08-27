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

/*
 * File Name     : EndpointConfigurationHandle.java
 *
 * The JAIN MGCP API implementaion.
 *
 * The source code contained in this file is in in the public domain.
 * It can be used in any project or product without prior permission,
 * license or royalty payments. There is  NO WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR STATUTORY, INCLUDING, WITHOUT LIMITATION,
 * THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * AND DATA ACCURACY.  We do not warrant or make any representations
 * regarding the use of the software or the  results thereof, including
 * but not limited to the correctness, accuracy, reliability or
 * usefulness of the software.
 */

package org.mobicents.protocols.mgcp.stack;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.EndpointConfiguration;
import jain.protocol.ip.mgcp.message.EndpointConfigurationResponse;
import jain.protocol.ip.mgcp.message.parms.BearerInformation;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.net.InetAddress;
import java.text.ParseException;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.parser.MgcpContentHandler;
import org.mobicents.protocols.mgcp.parser.MgcpMessageParser;
import org.mobicents.protocols.mgcp.parser.Utils;

/**
 * 
 * @author Oleg Kulikov
 * @author Pavel Mitrenko
 */
public class EndpointConfigurationHandler extends TransactionHandler {

	private EndpointConfiguration command;
	private EndpointConfigurationResponse response;

	private static final Logger logger = Logger.getLogger(EndpointConfigurationHandler.class);

	/** Creates a new instance of EndpointConfigurationHandle */
	public EndpointConfigurationHandler(JainMgcpStackImpl stack) {
		super(stack);
	}

	public EndpointConfigurationHandler(JainMgcpStackImpl stack, InetAddress address, int port) {
		super(stack, address, port);
	}

	public JainMgcpCommandEvent decodeCommand(String message) throws ParseException {
		Utils utils = utilsFactory.allocate();
		MgcpMessageParser parser = new MgcpMessageParser(new CommandContentHandle(utils));
		try {
			parser.parse(message);
		} catch (Exception e) {
			// should never happen
			logger.error("Parsing of EndpointConfiguration command failed", e);
		} finally {
			utilsFactory.deallocate(utils);
		}

		return command;
	}

	public JainMgcpResponseEvent decodeResponse(String message) throws ParseException {
		Utils utils = utilsFactory.allocate();
		MgcpMessageParser parser = new MgcpMessageParser(new ResponseContentHandle(utils));
		try {
			parser.parse(message);
		} catch (Exception e) {
			// should never happen
			logger.error("Parsing of EndpointConfiguration Response failed", e);
		} finally {
			utilsFactory.deallocate(utils);
		}
		return response;
	}

	public String encode(JainMgcpCommandEvent event) {
		// encode message header
		EndpointConfiguration evt = (EndpointConfiguration) event;
		StringBuffer s = new StringBuffer();
		s.append("EPCF ").append(evt.getTransactionHandle()).append(SINGLE_CHAR_SPACE).append(
				evt.getEndpointIdentifier()).append(MGCP_VERSION).append(NEW_LINE);

		// encode mandatory parameters
		s.append("B:e:").append(evt.getBearerInformation()).append(NEW_LINE);

		return s.toString();
	}

	public String encode(JainMgcpResponseEvent event) {
		EndpointConfigurationResponse response = (EndpointConfigurationResponse) event;
		ReturnCode returnCode = response.getReturnCode();
		StringBuffer s = new StringBuffer();
		s.append(returnCode.getValue()).append(SINGLE_CHAR_SPACE).append(response.getTransactionHandle()).append(
				SINGLE_CHAR_SPACE).append(returnCode.getComment()).append(NEW_LINE);

		return s.toString();
	}

	private class CommandContentHandle implements MgcpContentHandler {
		private Utils utils = null;

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

			command = new EndpointConfiguration(source != null ? source : stack, endpoint, BearerInformation.EncMethod_A_Law);
			command.setTransactionHandle(remoteTID);
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
			if (name.equalsIgnoreCase("B")) {
				command.setBearerInformation(utils.decodeBearerInformation(value));
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
		}
	}

	private class ResponseContentHandle implements MgcpContentHandler {
		private Utils utils = null;

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
			response = new EndpointConfigurationResponse(source != null ? source : stack, utils
					.decodeReturnCode(Integer.parseInt(tokens[0])));
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
		}

		/**
		 * Receive notification of the session description. Parser will call
		 * this method to report about session descriptor reading.
		 * 
		 * @param sd
		 *            the session description from message.
		 */
		public void sessionDescription(String sd) throws ParseException {
		}
	}

	@Override
	public JainMgcpResponseEvent getProvisionalResponse() {
		EndpointConfigurationResponse provisionalresponse = null;
		if (!sent) {
			provisionalresponse = new EndpointConfigurationResponse(source != null ? source : stack,
					ReturnCode.Transaction_Being_Executed);
		}
		return provisionalresponse;
	}

}
