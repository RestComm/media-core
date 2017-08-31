/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.control.mgcp.command.crcx;

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UnregisterConnectionCallback implements FutureCallback<MgcpConnection> {
    
    private static final Logger log = Logger.getLogger(UnregisterConnectionCallback.class);

    private final CreateConnectionFsm fsm;
    private final CreateConnectionContext context;
    private final boolean primary;

    public UnregisterConnectionCallback(boolean primary, CreateConnectionFsm fsm, CreateConnectionContext context) {
        this.fsm = fsm;
        this.context = context;
        this.primary = primary;
    }

    @Override
    public void onSuccess(MgcpConnection result) {
        // Continue with cleanup
        this.fsm.fire(CreateConnectionEvent.CONNECTION_UNREGISTERED, this.context);
    }

    @Override
    public void onFailure(Throwable t) {
        // Log error
        final int transactionId = context.getTransactionId();
        final String connectionHexId = getConnectionHexId();
        
        log.warn("Could not unregister connection " + connectionHexId + " safely during tx=" + transactionId + " execution. Reason: " + t.getMessage());
        
        // Continue with cleanup
        this.fsm.fire(CreateConnectionEvent.CONNECTION_UNREGISTERED, this.context);
    }
    
    private String getConnectionHexId() {
        MgcpConnection connection = this.primary ? this.context.getPrimaryConnection() : this.context.getSecondaryConnection();
        return connection.getHexIdentifier();
    }

}
