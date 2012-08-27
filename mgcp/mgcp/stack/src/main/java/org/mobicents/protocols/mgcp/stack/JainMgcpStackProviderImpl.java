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
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpProvider;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.JainMgcpStack;
import jain.protocol.ip.mgcp.message.AuditConnectionResponse;
import jain.protocol.ip.mgcp.message.AuditEndpointResponse;
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.DeleteConnectionResponse;
import jain.protocol.ip.mgcp.message.ModifyConnectionResponse;
import jain.protocol.ip.mgcp.message.NotificationRequestResponse;
import jain.protocol.ip.mgcp.message.Notify;
import jain.protocol.ip.mgcp.message.NotifyResponse;
import jain.protocol.ip.mgcp.message.RestartInProgressResponse;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.TooManyListenersException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

public class JainMgcpStackProviderImpl implements ExtendedJainMgcpProvider {

	private static Logger logger = Logger.getLogger(JainMgcpStackProviderImpl.class);
	private final JainMgcpStackImpl runningStack;

	// a tx handle id must be between 1 and 999999999
	private static int MIN_TRANSACTION_HANDLE_ID = 1;
	private static int MAX_TRANSACTION_HANDLE_ID = 999999999;
	private static AtomicInteger transactionHandleCounter = new AtomicInteger(
			(int) (System.currentTimeMillis() & 999999999));
	private static AtomicLong callIdentifierCounter = new AtomicLong(1);
	private static AtomicLong requestIdentifierCounter = new AtomicLong(1);
	// For now provider is only holder of listeners
	protected Set<JainMgcpListener> jainListeners = new HashSet<JainMgcpListener>();
	// This set contains upgraded listeners - one that allow to notify when tx
	// ends
	protected Set<JainMgcpExtendedListener> jainMobicentsListeners = new HashSet<JainMgcpExtendedListener>();

	protected NotifiedEntity notifiedEntity = null;

	public JainMgcpStackProviderImpl(JainMgcpStackImpl runningStack) {
		super();
		// eventQueue = new QueuedExecutor();
		// pool = Executors.newCachedThreadPool(new ThreadFactoryImpl());
		this.runningStack = runningStack;
	}

	public void setNotifiedEntity(NotifiedEntity notifiedEntity) {
		this.notifiedEntity = notifiedEntity;
	}

	public NotifiedEntity getNotifiedEntity() {
		return this.notifiedEntity;
	}

	public void addJainMgcpListener(JainMgcpListener listener) throws TooManyListenersException {
		if (listener instanceof JainMgcpExtendedListener) {
			synchronized (this.jainMobicentsListeners) {
				this.jainMobicentsListeners.add((JainMgcpExtendedListener) listener);
			}
		} else {
			synchronized (this.jainListeners) {
				this.jainListeners.add(listener);
			}
		}
	}

	public JainMgcpStack getJainMgcpStack() {
		return this.runningStack;
	}

	public void removeJainMgcpListener(JainMgcpListener listener) {
		if (listener instanceof JainMgcpExtendedListener) {
			synchronized (this.jainMobicentsListeners) {
				this.jainMobicentsListeners.remove((JainMgcpExtendedListener) listener);
			}
		} else {
			synchronized (this.jainListeners) {
				this.jainListeners.remove(listener);
			}
		}
	}

	public synchronized void sendMgcpEvents(JainMgcpEvent[] events) throws IllegalArgumentException {
		for (JainMgcpEvent event : events) {

			// For any onther than CRCX wildcard does not count?
			if (event instanceof JainMgcpCommandEvent) {
				// SENDING REQUEST
				JainMgcpCommandEvent commandEvent = (JainMgcpCommandEvent) event;

				// This is for TCK
				if (commandEvent.getTransactionHandle() < 1) {
					commandEvent.setTransactionHandle(this.getUniqueTransactionHandler());
				}

				TransactionHandler handle = null;
				switch (commandEvent.getObjectIdentifier()) {

				case Constants.CMD_AUDIT_CONNECTION:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending EndpointConfiguration object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new AuditConnectionHandler(this.runningStack);
					break;

				case Constants.CMD_AUDIT_ENDPOINT:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending EndpointConfiguration object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new AuditEndpointHandler(this.runningStack);
					break;

				case Constants.CMD_CREATE_CONNECTION:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending CreateConnection object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new CreateConnectionHandler(this.runningStack);
					break;

				case Constants.CMD_DELETE_CONNECTION:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending DeleteConnection object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new DeleteConnectionHandler(this.runningStack);
					break;

				case Constants.CMD_ENDPOINT_CONFIGURATION:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending EndpointConfiguration object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new EndpointConfigurationHandler(this.runningStack);
					break;

				case Constants.CMD_MODIFY_CONNECTION:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending ModifyConnection object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new ModifyConnectionHandler(this.runningStack);
					break;

				case Constants.CMD_NOTIFICATION_REQUEST:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending NotificationRequest object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new NotificationRequestHandler(this.runningStack);
					break;

				case Constants.CMD_NOTIFY:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending Notify object to NotifiedEntity"
								+ ((Notify) commandEvent).getNotifiedEntity());
					}
					handle = new NotifyHandler(this.runningStack);
					break;

				case Constants.CMD_RESP_UNKNOWN:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending ResponseUnknown object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new RespUnknownHandler(this.runningStack);
					break;

				case Constants.CMD_RESTART_IN_PROGRESS:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending RestartInProgress object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new RestartInProgressHandler(this.runningStack);
					break;

				default:
					throw new IllegalArgumentException("Could not send type of the message yet");
				}
				handle.setCommand(true);
				handle.setCommandEvent(commandEvent);

				handle.run();

			} else {

				// SENDING RESPONSE
				int tid = event.getTransactionHandle();
				TransactionHandler handler = (TransactionHandler) runningStack.getLocalTransactions().get(
						Integer.valueOf(tid));

				if (handler != null) {
					handler.setCommand(false);
					handler.setResponseEvent((JainMgcpResponseEvent) event);
					handler.run();

				} else {
					logger.error("The TransactionHandler not found for TransactionHandle " + tid
							+ " May be the Tx timed out. Event = " + (JainMgcpResponseEvent) event);
				}

			}
		}

	}

	public int getUniqueTransactionHandler() {
		// retreives current counter value and sets next one
		int current;
		int next;
		do {
			current = transactionHandleCounter.get();
			next = (current == MAX_TRANSACTION_HANDLE_ID ? MIN_TRANSACTION_HANDLE_ID : current + 1);
		} while (!transactionHandleCounter.compareAndSet(current, next));

		return current;
	}

	public void processMgcpResponseEvent(JainMgcpResponseEvent response, JainMgcpEvent command) {
		synchronized (this.jainListeners) {
			for (JainMgcpListener listener : this.jainListeners) {
				listener.processMgcpResponseEvent(response);
			}
		}

		synchronized (this.jainMobicentsListeners) {
			for (JainMgcpListener listener : this.jainMobicentsListeners) {
				listener.processMgcpResponseEvent(response);
			}
		}

	}

	public void processMgcpCommandEvent(JainMgcpCommandEvent command) {
		synchronized (this.jainListeners) {
			for (JainMgcpListener listener : this.jainListeners) {
				listener.processMgcpCommandEvent(command);
			}
		}

		synchronized (this.jainMobicentsListeners) {
			for (JainMgcpListener listener : this.jainMobicentsListeners) {
				listener.processMgcpCommandEvent(command);
			}
		}
	}

	public void processTxTimeout(JainMgcpCommandEvent command) {
		// notify RA
		// ra.processTxTimeout(command);
		synchronized (this.jainMobicentsListeners) {
			for (JainMgcpExtendedListener listener : this.jainMobicentsListeners) {
				listener.transactionTxTimedOut(command);
			}
		}
	}

	public void processRxTimeout(JainMgcpCommandEvent command) {
		// notify RA
		// ra.processRxTimeout(command);
		synchronized (this.jainMobicentsListeners) {
			for (JainMgcpExtendedListener listener : this.jainMobicentsListeners) {
				listener.transactionRxTimedOut(command);
			}
		}
		// reply to server
		JainMgcpResponseEvent response = null;
		// FIXME - how to change o return code of transaction timeout?!?
		switch (command.getObjectIdentifier()) {
		case Constants.CMD_AUDIT_CONNECTION:
			response = new AuditConnectionResponse(this, ReturnCode.Transient_Error);
			break;
		case Constants.CMD_AUDIT_ENDPOINT:
			response = new AuditEndpointResponse(this, ReturnCode.Transient_Error);
			break;
		case Constants.CMD_CREATE_CONNECTION:
			response = new CreateConnectionResponse(this, ReturnCode.Transient_Error, new ConnectionIdentifier(Long
					.toHexString(new Random(System.currentTimeMillis()).nextLong())));
			break;
		case Constants.CMD_DELETE_CONNECTION:
			response = new DeleteConnectionResponse(this, ReturnCode.Transient_Error);
			break;
		case Constants.CMD_ENDPOINT_CONFIGURATION:
			response = new DeleteConnectionResponse(this, ReturnCode.Transient_Error);
			break;
		case Constants.CMD_MODIFY_CONNECTION:
			response = new ModifyConnectionResponse(this, ReturnCode.Transient_Error);
			break;
		case Constants.CMD_NOTIFICATION_REQUEST:
			response = new NotificationRequestResponse(this, ReturnCode.Transient_Error);
			break;
		case Constants.CMD_NOTIFY:
			response = new NotifyResponse(this, ReturnCode.Transient_Error);
			break;
		case Constants.CMD_RESP_UNKNOWN:
			// FIXME - what response?!?
			response = new NotifyResponse(this, ReturnCode.Transient_Error);
			break;
		case Constants.CMD_RESTART_IN_PROGRESS:
			response = new RestartInProgressResponse(this, ReturnCode.Transient_Error);
			break;
		default:
			throw new IllegalArgumentException("Could not send type of the message yet");
		}
		response.setTransactionHandle(command.getTransactionHandle());
		JainMgcpEvent[] events = { response };
		sendMgcpEvents(events);
	}

	public CallIdentifier getUniqueCallIdentifier() {
		long current = -1;
		boolean b = true;
		while (b) {
			current = callIdentifierCounter.get();
			if (current == Long.MAX_VALUE) {
				b = !callIdentifierCounter.compareAndSet(current, 1);
			} else {
				b = !callIdentifierCounter.compareAndSet(current, current + 1);
			}
		}
		return new CallIdentifier(Long.toHexString(current));
	}

	public RequestIdentifier getUniqueRequestIdentifier() {
		long current = -1;
		boolean b = true;
		while (b) {
			current = requestIdentifierCounter.get();
			if (current == Long.MAX_VALUE) {
				b = !requestIdentifierCounter.compareAndSet(current, 1);
			} else {
				b = !requestIdentifierCounter.compareAndSet(current, current + 1);
			}
		}
		return new RequestIdentifier(Long.toHexString(current));
	}





	//Async part
	private LinkedList<JainMgcpEvent[]> asyncBuffer = new LinkedList<JainMgcpEvent[]>();
	
	

	public void sendAsyncMgcpEvents(JainMgcpEvent[] events)
			throws IllegalArgumentException {
		asyncBuffer.addLast(events);
		
	}

	public void flush() {
		
		JainMgcpEvent[] events;
		while( !asyncBuffer.isEmpty() )
		{
			events = this.asyncBuffer.removeFirst();
			this.sendMgcpEvents(events);
		}
	}

}
