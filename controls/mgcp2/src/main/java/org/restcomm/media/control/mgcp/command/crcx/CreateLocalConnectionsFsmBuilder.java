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

import org.squirrelframework.foundation.fsm.StateMachineBuilder;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CreateLocalConnectionsFsmBuilder extends AbstractCreateConnectionFsmBuilder {

    @Override
    protected CreateConnectionState[] getExecutionStates() {
        return new CreateConnectionState[] {};
    }

    @Override
    protected void configureExecutionStates(StateMachineBuilder<CreateConnectionFsm, CreateConnectionState, CreateConnectionEvent, CreateConnectionContext> builder) {
        builder.onEntry(CreateConnectionState.CREATING_PRIMARY_CONNECTION).perform(CreateLocalPrimaryConnectionAction.INSTANCE);
        
        builder.onEntry(CreateConnectionState.OPENING_PRIMARY_CONNECTION).perform(OpenPrimaryConnectionAction.INSTANCE);
        
        builder.onEntry(CreateConnectionState.CREATING_SECONDARY_CONNECTION).perform(CreateLocalSecondaryConnectionAction.INSTANCE);
        
        builder.onEntry(CreateConnectionState.OPENING_SECONDARY_CONNECTION).perform(OpenSecondaryConnectionAction.INSTANCE);

        builder.onEntry(CreateConnectionState.JOINING_CONNECTIONS).perform(JoinConnectionsAction.INSTANCE);

        builder.onEntry(CreateConnectionState.UPDATING_PRIMARY_CONNECTION_MODE).perform(UpdatePrimaryConnectionModeAction.INSTANCE);
        
        builder.onEntry(CreateConnectionState.UPDATING_SECONDARY_CONNECTION_MODE).perform(UpdateSecondaryConnectionModeAction.INSTANCE);
        
        builder.onEntry(CreateConnectionState.REGISTERING_PRIMARY_CONNECTION).perform(RegisterPrimaryConnectionAction.INSTANCE);
        
        builder.onEntry(CreateConnectionState.REGISTERING_SECONDARY_CONNECTION).perform(RegisterSecondaryConnectionAction.INSTANCE);

        builder.onEntry(CreateConnectionState.EXECUTED).perform(ExecutedAction.INSTANCE);
    }

    @Override
    protected CreateConnectionState[] getRollbackStates() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void configureRollbackStates(
            StateMachineBuilder<CreateConnectionFsm, CreateConnectionState, CreateConnectionEvent, CreateConnectionContext> builder) {
        // TODO Auto-generated method stub
        
    }

}
