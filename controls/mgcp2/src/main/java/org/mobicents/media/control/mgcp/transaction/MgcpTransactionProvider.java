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

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpTransactionProvider {
    
    private final MgcpTransactionNumberspace numberspace;

    public MgcpTransactionProvider(MgcpTransactionNumberspace numberspace) {
        this.numberspace = numberspace;
    }

    private int generateId() {
        return this.numberspace.generateId();
    }

    public boolean isLocal(int transactionId) {
        return transactionId >= this.numberspace.getMinimum() && transactionId <= this.numberspace.getMaximum();
    }
    
    private MgcpTransaction provide(int transactionId) {
        return new MgcpTransaction(transactionId);
    }

    public MgcpTransaction provideRemote(int transactionId) throws IllegalArgumentException {
        if(isLocal(transactionId)) {
            throw new IllegalArgumentException("Transaction ID " + transactionId + " is local, hence managed by this provider.");
        }
        return provide(transactionId);
    }
    
    public MgcpTransaction provideLocal() {
        return provide(generateId());
    }

}
