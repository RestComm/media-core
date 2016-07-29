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

import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.command.MgcpCommand;
import org.mobicents.media.control.mgcp.exception.DuplicateMgcpTransactionException;
import org.mobicents.media.control.mgcp.exception.MgcpTransactionNotFoundException;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;

/**
 * Manages a group of MGCP transactions.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpTransactionManager implements TransactionManager {

    private static final Logger log = Logger.getLogger(MgcpTransactionManager.class);

    // MGCP Components
    private final MgcpTransactionProvider transactionProvider;

    // MGCP Transaction Manager
    private final ConcurrentHashMap<Integer, MgcpTransaction> transactions;

    public MgcpTransactionManager(MgcpTransactionProvider transactionProvider) {
        // MGCP Components
        this.transactionProvider = transactionProvider;

        // MGCP Transaction Manager
        this.transactions = new ConcurrentHashMap<>(500);
    }

    private MgcpTransaction createTransaction(MgcpRequest request) throws DuplicateMgcpTransactionException {
        int transactionId = request.getTransactionId();
        
        // Create Transaction
        MgcpTransaction transaction;
        if (transactionId == 0) {
            // Transaction originated from within this Media Server (NTFY, for example)
            // to be sent out to call agent. A transaction ID must be generated
            transaction = this.transactionProvider.provideLocal();
            
            // Patch transaction ID
            request.setTransactionId(transaction.getId());
            transactionId = transaction.getId();
        } else {
            // Transaction originated from the remote call agent
            transaction = this.transactionProvider.provideRemote(transactionId);
        }

        // Register Transaction
        MgcpTransaction old = this.transactions.putIfAbsent(transaction.getId(), transaction);

        // Ensure transaction is not duplicate
        if (old != null) {
            throw new DuplicateMgcpTransactionException("Transaction " + transactionId + " already exists.");
        }

        if (log.isDebugEnabled()) {
            log.debug("Created new transaction " + transactionId + " to process request");
        }

        return transaction;
    }

    boolean contains(int transactionId) {
        return this.transactions.containsKey(transactionId);
    }

    @Override
    public void process(MgcpRequest request, MgcpCommand command) throws DuplicateMgcpTransactionException {
        createTransaction(request);
        if(command != null) {
            command.execute(request);
        }
    }

    @Override
    public void process(MgcpResponse response) throws MgcpTransactionNotFoundException {
        MgcpTransaction transaction = this.transactions.remove(response.getTransactionId());
        if (transaction == null) {
            throw new MgcpTransactionNotFoundException("Could not find transaction " + response.getTransactionId());
        } else if (log.isDebugEnabled()) {
            log.debug("Closed transaction " + response.getTransactionId() + " with code " + response.getCode());
        }
    }

}
