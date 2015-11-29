/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.server.mgcp.transaction;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.mobicents.media.server.mgcp.MgcpRequest;
import org.mobicents.media.server.mgcp.MgcpResponse;
import org.mobicents.media.server.scheduler.Scheduler;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpTransactionManager implements MgcpTransactionListener {

    private static final Logger LOGGER = Logger.getLogger(MgcpTransactionManager.class);

    private final ConcurrentHashMap<Integer, MgcpTransaction> transactions;
    private final Scheduler scheduler;

    public MgcpTransactionManager(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.transactions = new ConcurrentHashMap<Integer, MgcpTransaction>();
    }

    public void execute(MgcpRequest request) {
        MgcpTransaction transaction = createTransaction(request.getTransactionId());
        LOGGER.info("Executing " + request.toString());
        this.scheduler.submit(transaction);
    }

    public void execute(MgcpResponse response) {
        // TODO process incoming MGCP response
    }

    private MgcpTransaction createTransaction(int transactionId) {
        MgcpTransaction transaction = new MgcpTransaction(transactionId);
        MgcpTransaction oldValue = this.transactions.putIfAbsent(transactionId, transaction);

        if (oldValue == null) {
            return transaction;
        }
        // TODO throw exception
        return null;
    }

    @Override
    public void onTransactionEvent(MgcpTransactionEvent event) {
        MgcpTransaction transaction = event.getTransaction();
        this.transactions.remove(transaction.getId());
        
        if(event.isSuccessfull()) {
            LOGGER.info("Transaction " + transaction + " completed successfully.");
        } else {
            LOGGER.warn("Transaction " + transaction + " failed.");
        }
        
        // TODO send response to MGCP channel so it can forward it to remote peer
        transaction.getResponse();
    }

}
