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

import org.mobicents.media.control.mgcp.command.MgcpCommandProvider;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpTransaction {

    private final MgcpCommandProvider commandProvider;
    
    private int id;
    private String hexId;
    private boolean completed;
    private MgcpRequest request;
    private MgcpResponse response;

    public MgcpTransaction(MgcpCommandProvider commandProvider) {
        this.commandProvider = commandProvider;
        
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
    
    public void process(MgcpRequest request) throws IllegalStateException {
        if(this.request != null) {
            throw new IllegalStateException("Transaction is already processing a request.");
        }
        
        this.request = request;
        MgcpCommand command = this.commandProvider.provide(request.getRequestType());
        MgcpResponse response = command.execute(request);
        // TODO send response to channel
    }
    
    public void process(MgcpResponse request) throws IllegalStateException {
        if(this.request == null) {
            throw new IllegalStateException("Transaction has not yet proccessed a request.");
        }
        // TODO process response
    }
    
    

}
