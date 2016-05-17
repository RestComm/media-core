/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
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

package org.mobicents.media.control.mgcp.transaction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.mobicents.media.control.mgcp.command.MgcpCommandProvider;
import org.mobicents.media.control.mgcp.listener.MgcpTransactionListener;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessage;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mobicents.media.control.mgcp.network.MgcpChannel;

/**
 * Manages a group of MGCP transactions.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpTransactionManager implements MgcpTransactionListener {

    // MGCP Components
    private final MgcpCommandProvider commandProvider;
    private final MgcpChannel channel;

    // MGCP Transaction Manager
    private final Map<Integer, MgcpTransaction> transactions;
    private final AtomicInteger idGenerator;
    private final int minId;
    private final int maxId;

    public MgcpTransactionManager(int minId, int maxId, MgcpChannel channel, MgcpCommandProvider commandProvider) {
        // MGCP Components
        this.commandProvider = commandProvider;
        this.channel = channel;

        // MGCP Transaction Manager
        this.idGenerator = new AtomicInteger(minId);
        this.transactions = new ConcurrentHashMap<>(500);
        this.minId = minId;
        this.maxId = maxId;
    }

    private synchronized void verifyIdRange() {
        if (this.idGenerator.get() > maxId) {
            this.idGenerator.set(this.minId);
        }
    }

    private int generateId() {
        verifyIdRange();
        return this.idGenerator.getAndIncrement();
    }

    private boolean isLocal(int transactionId) {
        return transactionId >= this.minId && transactionId <= this.maxId;
    }

    private MgcpTransaction createTransaction() {
        return createTransaction(generateId());
    }

    private MgcpTransaction createTransaction(int transactionId) {
        MgcpTransaction transaction = new MgcpTransaction(this.commandProvider, this.channel, this);
        transaction.setId(generateId());
        return transaction;
    }

    private MgcpTransaction findTransaction(int transactionId) {
        return this.transactions.get(transactionId);
    }
    
    public void process(MgcpMessage message, MessageDirection direction) {
        int transactionId = message.getTransactionId();
        
        if(message.isRequest()) {
            // Create new transaction to process incoming request
            MgcpTransaction transaction = createTransaction();
            transaction.processRequest((MgcpRequest) message, MessageDirection.INBOUND);
        } else {
            MgcpTransaction transaction = findTransaction(transactionId);
            if(transaction == null) {
                // Send erroneous response
                MgcpResponse response = new MgcpResponse();
                response.setTransactionId(transactionId);
                response.setCode(MgcpResponseCode.PROTOCOL_ERROR.code());
                response.setMessage("Transaction " + transactionId + " was aborted and no longer exists");
                this.channel.queue(response.toString().getBytes());
            } else {
                transaction.processResponse((MgcpResponse) message);
            }
        }
    }

    @Override
    public void onTransactionComplete(MgcpTransaction transaction) {
        this.transactions.remove(transaction.getId());
    }

}
