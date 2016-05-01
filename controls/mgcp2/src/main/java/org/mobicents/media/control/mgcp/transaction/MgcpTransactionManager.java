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
import org.mobicents.media.control.mgcp.message.MgcpMessage;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;

/**
 * Manages a group of MGCP transactions.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpTransactionManager implements MgcpTransactionListener {

    private final AtomicInteger idGenerator;
    private final MgcpCommandProvider commandProvider;
    private final Map<Integer, MgcpTransaction> transactions;
    
    private final int minId;
    private final int maxId;

    public MgcpTransactionManager(int minId, int maxId) {
        this.idGenerator = new AtomicInteger(minId);
        this.commandProvider = new MgcpCommandProvider();
        this.transactions = new ConcurrentHashMap<>(500);
        this.minId = minId;
        this.maxId = maxId;
    }
    
    private synchronized void verifyIdRange() {
        if(this.idGenerator.get() > maxId) {
            this.idGenerator.set(this.minId);
        }
    }
    
    private int generateId() {
        verifyIdRange();
        return this.idGenerator.getAndIncrement();
    }
    
    public boolean isLocal(int transactionId) {
        return transactionId >= this.minId && transactionId <= this.maxId;
    }

    public MgcpTransaction createTransaction() {
        MgcpTransaction transaction = new MgcpTransaction(this.commandProvider);
        transaction.setId(generateId());
        transaction.setListener(this);
        return transaction;
    }
    
    public MgcpTransaction findTransaction(int transactionId) {
        return this.transactions.get(transactionId);
    }

    public void process(MgcpMessage message) {
        if (message.isRequest()) {
            processRequest((MgcpRequest) message);
        } else {
            processResponse((MgcpResponse) message);
        }
    }

    public void processRequest(MgcpRequest request) {
        int transactionId = request.getTransactionId();

        if (this.transactions.containsKey(transactionId)) {
            // TODO send erroneous response
        } else {
            // Create new transaction
            MgcpTransaction transaction = createTransaction();
            this.transactions.put(transactionId, transaction);
            // Execute request
            transaction.process(request);
        }
    }

    public void processResponse(MgcpResponse response) {
        // Fetch ongoing transaction
        int transactionId = response.getTransactionId();
        MgcpTransaction transaction = this.transactions.get(response.getTransactionId());

        // Terminate transaction
        if (transaction == null) {
            // TODO send erroneous message
        } else {
            transaction.process(response);
            this.transactions.remove(transactionId);
        }
    }

    @Override
    public void onTransactionComplete(MgcpTransaction transaction) {
        this.transactions.remove(transaction.getId());
    }

}
