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
 * File Name     : TransactionHandle.java
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
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.Notify;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.parser.UtilsFactory;

/**
 * Implements the base gateway control interface.
 * 
 * The MGCP implements the media gateway control interface as a set of transactions. The transactions are composed of a
 * command and a mandatory response. There are eight types of command:
 * 
 * <li>CreateConnection ModifyConnection DeleteConnection NotificationRequest Notify AuditEndpoint AuditConnection
 * RestartInProgress</li>
 * 
 * The first four commands are sent by the Call Agent to a gateway. The Notify command is sent by the gateway to the
 * Call Agent. The gateway may also send a DeleteConnection. The Call Agent may send either of the Audit commands to the
 * gateway. The Gateway may send a RestartInProgress command to the Call Agent.
 * 
 * All commands are composed of a Command header, optionally followed by a session description.
 * 
 * All responses are composed of a Response header, optionally followed by a session description.
 * 
 * Headers and session descriptions are encoded as a set of text lines, separated by a line feed character. The headers
 * are separated from the session description by an empty line.
 * 
 * MGCP uses a transaction identifier to correlate commands and responses. The transaction identifier is encoded as a
 * component of the command header and repeated as a component of the response header.
 * 
 * Transaction identifiers have values between 1 and 999999999. An MGCP entity shall not reuse a transaction identifier
 * sooner than 3 minutes after completion of the previous command in which the identifier was used.
 * 
 * @author Oleg Kulikov
 * @author Pavel Mitrenko
 * @author Amit Bhayani
 */
// public abstract class TransactionHandler implements Runnable,
// TransactionHandlerManagement {
public abstract class TransactionHandler implements Runnable {
	/** Logger instance */
	private static final Logger logger = Logger.getLogger(TransactionHandler.class);

	private static int GENERATOR = (int) (System.currentTimeMillis() & 999999999);

	public static final String NEW_LINE = "\n";
	public static final String SINGLE_CHAR_SPACE = " ";
	public static final String MGCP_VERSION = " MGCP 1.0"; // let the single
	// char space prefix
	// the version

	public final static int LONGTRAN_TIMER_TIMEOUT = 5000; // 5secs

	public static final int THIST_TIMER_TIMEOUT = 30000; // 30 sec
	/** Is this a transaction on a command sent or received? */
	protected boolean sent;
	/** Transaction handle sent from application to the MGCP provider. */
	protected int remoteTID;
	/** Transaction handle sent from MGCP provider to MGCP listener */
	private int localTID;
	protected JainMgcpStackImpl stack;
	/** Holds the address from wich request was originaly received by provider */
	private InetAddress remoteAddress;

	/**
	 * Holds the port number from wich request was originaly received by provider
	 */
	private int remotePort;
	/** Used to hold parsed command event */
	protected JainMgcpCommandEvent commandEvent;

	/** Used to hold parsed response event * */
	protected JainMgcpResponseEvent responseEvent;

	/** Expiration timer */
	protected static Timer transactionHandlerTimer = new Timer("TransactionHandlerTimer");
	private LongtranTimerTask longtranTimerTask;

	/** Flag to check if this is Command or Response event * */
	private boolean isCommand = false;

	private ReTransmissionTimerTask reTransmissionTimer;

	private THISTTimerTask tHISTTimerTask;

	private int A = 0;
	private int D = 2;
	private int N = 2;

	// private DatagramPacket sendComandDatagram = null;

	private int countOfCommandRetransmitted = 0;

	protected UtilsFactory utilsFactory = null;

	// protected EndpointHandler endpointHandler = null;

	protected boolean retransmision;

	protected Object source = null;

	private String msgTemp = null;

	protected EndpointIdentifier endpoint = null;

	private byte[] data;
	private InetSocketAddress inetSocketAddress = null;

	/**
	 * Creates a new instance of TransactionHandle
	 * 
	 * Used by provider to prepare origination transaction for sending command message from an application to the stack.
	 * 
	 * @param stack
	 *            the reference to the MGCP stack.
	 */
	public TransactionHandler(JainMgcpStackImpl stack) {
		this.stack = stack;
		this.localTID = GENERATOR++;
		// utils = new Utils();
		utilsFactory = stack.getUtilsFactory();
		stack.getLocalTransactions().put(Integer.valueOf(localTID), this);
		// if (logger.isDebugEnabled()) {
		// logger.debug("New mgcp transaction with id localID=" + localTID);
		// }
	}

	/**
	 * Creates a new instance of TransactionHandle.
	 * 
	 * Used by stack to prepare transaction for transmitting message from provider to the application.
	 * 
	 * @param stack
	 *            the reference to the MGCP stack.
	 * @remoteAddress the address from wich command message was received.
	 * @port the number of the port from wich command received.
	 */
	public TransactionHandler(JainMgcpStackImpl stack, InetAddress remoteAddress, int port) {
		this(stack);
		this.remoteAddress = remoteAddress;
		this.remotePort = port;
		if (this.stack.provider.getNotifiedEntity() == null) {
			NotifiedEntity notifiedEntity = new NotifiedEntity(this.remoteAddress.getHostName(), this.remoteAddress
					.getHostAddress(), this.remotePort);
			this.stack.provider.setNotifiedEntity(notifiedEntity);
		}
	}

	private void processTxTimeout() {
		try {
			// releases the tx
			release();

			// the try ensures the static timer will not get a runtime
			// exception process tx timeout
			commandEvent.setTransactionHandle(this.remoteTID);
			if (sent) {				
				stack.provider.processTxTimeout(commandEvent);
			} else {
				// TODO : Send back 406 TxTimedOut to NotifiedEntity
				stack.provider.processRxTimeout(commandEvent);
			}

		} catch (Exception e) {
			logger.error("Failed to release mgcp transaction localID=" + localTID, e);
		}
	}

	private class LongtranTimerTask extends TimerTask {

		public void run() {
			if (logger.isDebugEnabled()) {
				logger.debug("Transaction localID=" + localTID + " timeout");
			}
			processTxTimeout();
		}
	}

	private class ReTransmissionTimerTask extends TimerTask {

		public void run() {
			try {
				// Sending the command
				countOfCommandRetransmitted++;

				logger.warn("message = \n" + msgTemp + "\n local Tx ID = " + localTID + " Remote Tx ID = " + remoteTID
						+ " Sending the Command " + countOfCommandRetransmitted);

				stack.send(data, inetSocketAddress);
				resetReTransmissionTimer();

			} catch (Exception e) {
				logger.error("Failed to release mgcp transaction localID=" + localTID, e);
			}
		}
	}

	private class THISTTimerTask extends TimerTask {

		boolean responseSent = false;

		THISTTimerTask(boolean responseSent) {
			this.responseSent = responseSent;
		}

		public void run() {

			if (!responseSent) {
				if (logger.isDebugEnabled()) {
					logger.debug("T-HIST timeout processTxTimeout ");
				}
				try {
					processTxTimeout();
				} catch (Exception e) {
					logger.error("Failed to delete the jainMgcpResponseEvent for txId", e);
				}
			} else {
				Integer key = new Integer(remoteTID);
				TransactionHandler obj = stack.getCompletedTransactions().remove(key);
				if (logger.isDebugEnabled()) {
					logger.debug("T-HIST timeout deleting Response for Tx = " + remoteTID + " Response = " + obj);
				}
				obj = null;
			}
		}
	}

	/**
	 * Check whether the given return code is a provisional response.
	 * 
	 * @param rc
	 *            the return code
	 * @return true when the code is provisional
	 */
	private boolean isProvisional(ReturnCode rc) {
		final int rval = rc.getValue();

		return ((99 < rval) && (rval < 200));
	}

	/** Release this transaction and frees all allocated resources. */
	protected void release() {
		stack.getLocalTransactions().remove(Integer.valueOf(localTID));
		stack.getRemoteTxToLocalTxMap().remove(Integer.valueOf(remoteTID));
		cancelTHISTTimerTask();
		cancelLongtranTimer();
		cancelReTransmissionTimer();

	}

	/**
	 * Returns the transaction handle sent from application to the MGCP provider.
	 * 
	 * @return the int value wich identifiers the transaction handle.
	 */
	public int getRemoteTID() {
		return remoteTID;
	}

	/**
	 * Returns the transaction handle sent from MGCP provider to listener.
	 * 
	 * @return the int value wich identifiers the transaction handle.
	 */
	public int getLocalTID() {
		return localTID;
	}

	/**
	 * Encodes command event object into MGCP command message.
	 * 
	 * All descendant classes should implement this method with accordance of the command type.
	 * 
	 * @param event
	 *            the command event object.
	 * @return the encoded MGCP message.
	 */
	public abstract String encode(JainMgcpCommandEvent event);

	/**
	 * Encodes response event object into MGCP response message.
	 * 
	 * All descendant classes should implement this method with accordance of the response type.
	 * 
	 * @param event
	 *            the response event object.
	 * @return the encoded MGCP message.
	 */
	public abstract String encode(JainMgcpResponseEvent event);

	/**
	 * Decodes MGCP command message into jain mgcp command event object.
	 * 
	 * All descendant classes should implement this method with accordance of the command type.
	 * 
	 * @param MGCP
	 *            message
	 * @return jain mgcp command event object.
	 */
	public abstract JainMgcpCommandEvent decodeCommand(final String msg) throws ParseException;

	/**
	 * Decodes MGCP response message into jain mgcp response event object.
	 * 
	 * All descendant classes should implement this method with accordance of the command type.
	 * 
	 * @param MGCP
	 *            message
	 * @return jain mgcp response event object.
	 */
	public abstract JainMgcpResponseEvent decodeResponse(String message) throws ParseException;

	public abstract JainMgcpResponseEvent getProvisionalResponse();

	public void run() {
		if (isCommand) {
			this.send(this.getCommandEvent());
		} else {
			this.send(this.getResponseEvent());
		}

	}

	protected void sendProvisionalResponse() {
		this.send(getProvisionalResponse());
	}

	/**
	 * Sends MGCP command from the application to the endpoint specified in the message.
	 * 
	 * @param event
	 *            the jain mgcp command event object.
	 */
	private void send(JainMgcpCommandEvent event) {

		sent = true;

		String host = "";
		int port = 0;

		switch (event.getObjectIdentifier()) {
		case Constants.CMD_NOTIFY:
			Notify notifyCommand = (Notify) event;
			NotifiedEntity notifiedEntity = notifyCommand.getNotifiedEntity();
			if (notifiedEntity == null) {
				notifiedEntity = this.stack.provider.getNotifiedEntity();
			}
			port = notifiedEntity.getPortNumber();
			// if (notifiedEntity.getLocalName() != null) {
			// host = notifiedEntity.getLocalName() + "@";
			// }
			host += notifiedEntity.getDomainName();

			break;

		default:
			// determite destination address and port to send request to
			// from endpoint identifier parameter.
			String domainName = event.getEndpointIdentifier().getDomainName();

			// now checks does port number is specified in the domain name
			// if port number is not specified use 2427 by default
			int pos = domainName.indexOf(':');
			if (pos > 0) {
				port = Integer.parseInt(domainName.substring(pos + 1));
				host = domainName.substring(0, pos);
			} else {
				port = 2427;
				host = domainName;
			}
			break;
		}

		// construct the destination as InetAddress object
		InetAddress address = null;
		try {
			address = InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("Unknown endpoint " + host);
		}

		// save this tx in stack and start timer
		remoteTID = event.getTransactionHandle();
		source = event.getSource();
		event.setTransactionHandle(localTID);

		// encode event object as MGCP command and send over UDP.
		String msg = encode(event);

		msgTemp = msg;

		data = msg.getBytes();
		inetSocketAddress = new InetSocketAddress(address, port);

		resetReTransmissionTimer();
		resetTHISTTimerTask(false);

		if (logger.isDebugEnabled()) {
			logger.debug("Send command event to " + address + ", message\n" + msg);
		}
		countOfCommandRetransmitted++;
		stack.send(data, inetSocketAddress);
		
	}

	/**
	 * Sends MGCP response message from the application to the host from wich origination command was received.
	 * 
	 * @param event
	 *            the jain mgcp response event object.
	 */
	private void send(JainMgcpResponseEvent event) {

		cancelLongtranTimer();

		// to send response we already should know the address and port
		// number from which the original request was received
		if (remoteAddress == null) {
			throw new IllegalArgumentException("Unknown orinator address");
		}

		// restore the original transaction handle parameter
		// and encode event objet into MGCP response message
		event.setTransactionHandle(remoteTID);

		// encode event object into MGCP response message
		String msg = encode(event);

		// send response message to the originator
		data = msg.getBytes();
		inetSocketAddress = new InetSocketAddress(remoteAddress, remotePort);

		if (logger.isDebugEnabled()) {
			logger.debug("--- TransactionHandler:" + this + " :LocalID=" + localTID + ", Send response event to "
					+ remoteAddress + ":" + remotePort + ", message\n" + msg);
		}
		stack.send(data, inetSocketAddress);

		/*
		 * Just reset timer in case of provisional response. Otherwise, release tx.
		 */
		if (isProvisional(event.getReturnCode())) {
			// reset timer.
			resetLongtranTimer();
		} else {
			
			release();
			stack.getCompletedTransactions().put(Integer.valueOf(event.getTransactionHandle()), this);
			resetTHISTTimerTask(true);
		}

	}

	private void cancelLongtranTimer() {
		if (longtranTimerTask != null) {
			longtranTimerTask.cancel();
			longtranTimerTask = null;
		}
	}

	private void resetLongtranTimer() {

		longtranTimerTask = new LongtranTimerTask();
		transactionHandlerTimer.schedule(longtranTimerTask, LONGTRAN_TIMER_TIMEOUT);
	}

	private void cancelReTransmissionTimer() {
		if (reTransmissionTimer != null) {
			reTransmissionTimer.cancel();
			reTransmissionTimer = null;
		}
	}

	private void resetReTransmissionTimer() {
		cancelReTransmissionTimer();
		reTransmissionTimer = new ReTransmissionTimerTask();
		transactionHandlerTimer.schedule(reTransmissionTimer, calculateReTransmissionTimeout());
	}

	// TODO : Implement the AAD and ADEV from TCP
	private int calculateReTransmissionTimeout() {
		int reTransmissionTimeoutSec = A + N * D;
		N = N * 2;
		return reTransmissionTimeoutSec * 1000;
	}

	private void cancelTHISTTimerTask() {
		if (tHISTTimerTask != null) {
			tHISTTimerTask.cancel();
			tHISTTimerTask = null;
		}
	}

	private void resetTHISTTimerTask(boolean responseSent) {
		cancelTHISTTimerTask();
		tHISTTimerTask = new THISTTimerTask(responseSent);
		transactionHandlerTimer.schedule(tHISTTimerTask, THIST_TIMER_TIMEOUT);
	}

	/**
	 * constructs the object source for a command
	 * 
	 * @param tid
	 * @return
	 */
	protected Object getObjectSource(int tid) {
		if (sent) {
			return stack;
		} else {
			return new ReceivedTransactionID(tid, this.remoteAddress, remotePort);
		}
	}

	public boolean isCommand() {
		return isCommand;
	}

	public void setCommand(boolean isCommand) {
		this.isCommand = isCommand;
	}

	private JainMgcpCommandEvent getCommandEvent() {
		return commandEvent;
	}

	public void setCommandEvent(JainMgcpCommandEvent commandEvent) {

		this.commandEvent = commandEvent;
		// this.actionToPerform.add(new ScheduleCommandSend());
	}

	private JainMgcpResponseEvent getResponseEvent() {
		return responseEvent;
	}

	public void setResponseEvent(JainMgcpResponseEvent responseEvent) {

		this.responseEvent = responseEvent;
		// this.actionToPerform.add(new ScheduleCommandSend());

	}

	public void markRetransmision() {
		this.retransmision = true;

	}

	public void receiveRequest(final EndpointIdentifier endpoint, final String msg, final Integer remoteTID) {

		this.remoteTID = remoteTID;
		this.endpoint = endpoint;
		try {
			commandEvent = decodeCommand(msg);
			// if (logger.isDebugEnabled()) {
			// logger.debug("Event decoded: \n" + event);
			// }
		} catch (ParseException e) {
			logger.error("Coud not parse message: ", e);
			return;
		}
		sent = false;

		// store original transaction handle parameter
		// and populate with local value

		stack.getRemoteTxToLocalTxMap().put(remoteTID, new Integer(localTID));

		commandEvent.setTransactionHandle(localTID);

		resetLongtranTimer();

		stack.provider.processMgcpCommandEvent(commandEvent);

		// this.actionToPerform.add(new ScheduleRequestReceival(this));
		// we shoudl be scheduled by message handler

	}

	/**
	 * Used by stack for relaying received MGCP response messages to the application.
	 * 
	 * @param message
	 *            receive MGCP response message.
	 */
	public void receiveResponse(String message) {

		cancelReTransmissionTimer();
		cancelLongtranTimer();

		JainMgcpResponseEvent event = null;

		try {
			event = decodeResponse(message);
		} catch (Exception e) {
			logger.error("Could not decode message: ", e);
		}

		// restore original transaction handle parameter
		event.setTransactionHandle(remoteTID);

		/*
		 * Just reset timer in case of provisional response. Otherwise, release tx.
		 */
		if (this.isProvisional(event.getReturnCode())) {
			resetLongtranTimer();
		}

		stack.provider.processMgcpResponseEvent(event, commandEvent);

		if (!this.isProvisional(event.getReturnCode())) {
			this.release();
		}
	}

	public String getEndpointId() {

		return this.commandEvent.getEndpointIdentifier().toString();
	}
}
