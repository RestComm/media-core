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
 * File Name     : MessageHandler.java
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

import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.parser.Utils;
import org.mobicents.protocols.mgcp.utils.PacketRepresentation;

/**
 * 
 * @author Oleg Kulikov
 */
public class MessageHandler {

	private JainMgcpStackImpl stack;
	private static Logger logger = Logger.getLogger(MessageHandler.class);

	private Utils utils = null;

	private static ArrayList<String> mList = new ArrayList<String>();

	/** Creates a new instance of MessageHandler */

	public MessageHandler(JainMgcpStackImpl jainMgcpStackImpl) {
		this.stack = jainMgcpStackImpl;
		utils = jainMgcpStackImpl.getUtilsFactory().allocate();
	}

	/**
	 * RFC 3435, $3.5.5: split piggy backed messages again
	 * <P>
	 * Messages within the packet are split on their separator "EOL DOT EOL".
	 * 
	 * @param packet
	 *            the packet to split
	 * @return array of all separate messages
	 */
	public static String[] piggyDismount(byte[] msgBuffer, int length) {
		try {
			int msgStart = 0;
			int msgLength = 0;
			String currentLine = null;

			for (int i = 0; i < length - 1; i++) {
				if ((msgBuffer[i] == '\n' || msgBuffer[i] == '\r') && msgBuffer[i + 1] == '.') {
					msgLength = i - msgStart;

					try {
						currentLine = new String(msgBuffer, msgStart, msgLength + 1, "UTF-8");
						mList.add(currentLine);
					} catch (UnsupportedEncodingException e) {
						logger.error(e);
					}
					i = i + 3;
					msgStart = i;

				}
			}
			try {
				msgLength = length - msgStart;
				currentLine = new String(msgBuffer, msgStart, msgLength, "UTF-8");
				mList.add(currentLine);
			} catch (UnsupportedEncodingException e) {
				logger.error(e); // Should never happen though
			}
			String[] result = new String[mList.size()];
			return (String[]) mList.toArray(result);
		} finally {
			mList.clear();
		}

	}

	public boolean isRequest(String header) {
		header = header.trim();

		char ch = header.charAt(0);

		if (Character.isDigit(ch)) {
			return false;
		}
		return true;
		// Dont use matcher due to bug
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6337993
		// Matcher m = p.matcher(header);
		// return m.matches();
		// return header.matches("[\\w]{4}(\\s|\\S)*");
	}

	public void scheduleMessages(PacketRepresentation pr) {
		try {
			final InetAddress address = pr.getRemoteAddress();
			final int port = pr.getRemotePort();
			for (String msg : piggyDismount(pr.getRawData(), pr.getLength())) {

				int pos = msg.indexOf("\n");

				// extract message header to determine transaction handle
				// parameter and type of the received message
				String header = msg.substring(0, pos).trim();
				if (logger.isDebugEnabled()) {
					logger.debug("Message header: " + header);
				}

				// check message type if this message is command then create new
				// transaction handler for specified type of this message. if
				// received message is a response then try to find corresponded
				// transaction to handle this message
				String tokens[] = utils.splitStringBySpace(header);
				if (isRequest(header)) {

					final String verb = tokens[0];
					final String remoteTxIdString = tokens[1];
					final EndpointIdentifier endpoint = utils.decodeEndpointIdentifier(tokens[2].trim());

					if (logger.isDebugEnabled()) {
						logger.debug("Processing command message = " + verb + " remote Tx = " + remoteTxIdString);
					}

					Integer remoteTxIdIntegere = new Integer(remoteTxIdString);

					// Check if the Response still in responseTx Map
					TransactionHandler completedTxHandler = stack.getCompletedTransactions().get(remoteTxIdIntegere);
					if (completedTxHandler != null) {

						// EndpointHandler eh = completedTxHandler.getEndpointHandler();
						completedTxHandler.markRetransmision();
						completedTxHandler.run();
						// eh.scheduleTransactionHandler(completedTxHandler);

						if (logger.isDebugEnabled()) {
							logger.debug("Received Command for which stack has already sent response Tx = " + verb
									+ " " + remoteTxIdIntegere);
						}

						return;
					}

					Integer tmpLoaclTID = stack.getRemoteTxToLocalTxMap().get(remoteTxIdIntegere);
					if (tmpLoaclTID != null) {
						TransactionHandler ongoingTxHandler = stack.getLocalTransactions().get(tmpLoaclTID);
						ongoingTxHandler.sendProvisionalResponse();
						if (logger.isDebugEnabled()) {
							logger.debug("Received Command for ongoing Tx = " + remoteTxIdIntegere);
						}
						return;
					}

					// If we are here, it means this is new TX, we have to
					// create TxH and EH

					TransactionHandler handler;
					if (verb.equalsIgnoreCase("crcx")) {
						handler = new CreateConnectionHandler(stack, address, port);
					} else if (verb.equalsIgnoreCase("mdcx")) {
						handler = new ModifyConnectionHandler(stack, address, port);
					} else if (verb.equalsIgnoreCase("dlcx")) {
						handler = new DeleteConnectionHandler(stack, address, port);
					} else if (verb.equalsIgnoreCase("epcf")) {
						handler = new EndpointConfigurationHandler(stack, address, port);
					} else if (verb.equalsIgnoreCase("rqnt")) {
						handler = new NotificationRequestHandler(stack, address, port);
					} else if (verb.equalsIgnoreCase("ntfy")) {
						handler = new NotifyHandler(stack, address, port);
					} else if (verb.equalsIgnoreCase("rsip")) {
						handler = new RestartInProgressHandler(stack, address, port);
					} else if (verb.equalsIgnoreCase("auep")) {
						handler = new AuditEndpointHandler(stack, address, port);
					} else if (verb.equalsIgnoreCase("aucx")) {
						handler = new AuditConnectionHandler(stack, address, port);
					} else {
						logger.warn("Unsupported message verbose " + verb);
						return;
					}

					// This makes this command to be set in queue to process
					handler.receiveRequest(endpoint, msg, remoteTxIdIntegere);
					// boolean useFakeOnWildcard = false;
					// if (handler instanceof CreateConnectionHandler) {
					// useFakeOnWildcard = EndpointHandler.isAnyOfWildcard(handler.getEndpointId());
					// }
					// EndpointHandler eh = stack.getEndpointHandler(handler.getEndpointId(), useFakeOnWildcard);
					// eh.addTransactionHandler(handler);
					//
					// eh.scheduleTransactionHandler(handler);
					// handle.receiveCommand(msg);
				} else {
					// RESPONSE HANDLING
					if (logger.isDebugEnabled()) {
						logger.debug("Processing response message");
					}
					// String domainName = address.getHostName();
					String tid = tokens[1];

					// XXX:TransactionHandler handler = (TransactionHandler)
					// stack.getLocalTransaction(Integer.valueOf(tid));
					TransactionHandler handler = (TransactionHandler) stack.getLocalTransactions().get(
							Integer.valueOf(tid));
					if (handler == null) {
						logger.warn("---  Address:" + address + "\nPort:" + port + "\nID:" + this.hashCode()
								+ "\n Unknown transaction: " + tid);
						return;
					}
					handler.receiveResponse(msg);

					// EndpointHandler eh = handler.getEndpointHandler();
					// eh.scheduleTransactionHandler(handler);

				}
			}
		} finally {
			pr.release();
		}

	}
}
