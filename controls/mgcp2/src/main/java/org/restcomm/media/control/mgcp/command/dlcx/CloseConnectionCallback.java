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

package org.restcomm.media.control.mgcp.command.dlcx;

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CloseConnectionCallback implements FutureCallback<Void> {

    private static final Logger log = Logger.getLogger(CloseConnectionCallback.class);

    private final MgcpConnection connection;
    private final DeleteConnectionContext context;
    private final DeleteConnectionFsm fsm;

    public CloseConnectionCallback(MgcpConnection connection, DeleteConnectionContext context, DeleteConnectionFsm fsm) {
        this.connection = connection;
        this.context = context;
        this.fsm = fsm;
    }

    @Override
    public void onSuccess(Void result) {
        this.fsm.fire(DeleteConnectionEvent.CLOSED_CONNECTION, context);
    }

    @Override
    public void onFailure(Throwable t) {
        final int transactionId = this.context.getTransactionId();
        final String connectionId = this.connection.getHexIdentifier();
        log.error("Could not close connection " + connectionId + " during MGCP transaction " + transactionId, t);

        this.fsm.fire(DeleteConnectionEvent.CLOSED_CONNECTION, this.context);
    }

}
