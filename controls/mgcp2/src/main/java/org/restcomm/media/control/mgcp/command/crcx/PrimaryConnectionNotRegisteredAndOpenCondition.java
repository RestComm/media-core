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

import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.squirrelframework.foundation.fsm.AnonymousCondition;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class PrimaryConnectionNotRegisteredAndOpenCondition extends AnonymousCondition<CreateConnectionContext> implements CreateConnectionCondition {

    static final PrimaryConnectionNotRegisteredAndOpenCondition INSTANCE = new PrimaryConnectionNotRegisteredAndOpenCondition();
    
    PrimaryConnectionNotRegisteredAndOpenCondition() {
        super();
    }
    
    @Override
    public boolean isSatisfied(CreateConnectionContext context) {
        final MgcpEndpoint endpoint = context.getPrimaryEndpoint();
        final MgcpConnection connection = context.getPrimaryConnection();
        final int callId = connection.getCallIdentifier();
        final int connectionId = connection.getIdentifier();
        
        return !endpoint.isRegistered(callId, connectionId) && connection.isOpen();
    }

}
