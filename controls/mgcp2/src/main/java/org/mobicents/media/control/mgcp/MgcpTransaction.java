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

package org.mobicents.media.control.mgcp;

import org.mobicents.media.control.mgcp.command.AbstractMgcpCommand;
import org.mobicents.media.control.mgcp.command.MgcpCommandProvider;
import org.mobicents.media.control.mgcp.listener.MgcpTransactionListener;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpTransaction {

    private MgcpTransactionListener listener;

    // MGCP Core Components
    private final MgcpCommandProvider commands;

    // MGCP Transaction State
    private int id;
    private boolean completed;
    private MgcpRequest request;
    private MgcpResponse response;

    public MgcpTransaction(MgcpCommandProvider commands) {
        // MGCP Core Components
        this.commands = commands;

        // MGCP Transaction State
        this.id = 0;
        this.completed = false;
        this.request = null;
        this.response = null;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
        // TODO set hexId
    }

    public void setListener(MgcpTransactionListener listener) {
        this.listener = listener;
    }

    public void process(MgcpMessage message) throws IllegalStateException {
        if (message.isRequest()) {
            processRequest((MgcpRequest) message);
        } else {
            processResponse((MgcpResponse) message);
        }
    }

    private void processRequest(MgcpRequest request) throws IllegalStateException {
        if (this.request != null) {
            throw new IllegalStateException("Transaction is already processing a request.");
        }

        this.request = request;
        AbstractMgcpCommand command = this.commands.provide(request);
        command.execute(request);
    }

    private void processResponse(MgcpResponse response) throws IllegalStateException {
        if (this.request == null) {
            throw new IllegalStateException("Transaction has not yet proccessed a request.");
        }
        
        this.response = response;
        // TODO process response
    }

}
