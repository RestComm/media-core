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

import static org.restcomm.media.control.mgcp.command.crcx.CreateConnectionState.*;
import static org.restcomm.media.control.mgcp.command.crcx.CreateConnectionEvent.*;

/**
 * Creates an state machine for a CRCX Command that creates two local connections and joins them.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class CreateRemoteConnectionFsmBuilder extends AbstractCreateConnectionFsmBuilder {

    static final CreateRemoteConnectionFsmBuilder INSTANCE = new CreateRemoteConnectionFsmBuilder();

    CreateRemoteConnectionFsmBuilder() {
        super();
    }

    @Override
    protected CreateConnectionState[] getExecutionStates() {
        return new CreateConnectionState[] { CREATING_PRIMARY_CONNECTION, HALF_OPENING_PRIMARY_CONNECTION, OPENING_PRIMARY_CONNECTION, UPDATING_PRIMARY_CONNECTION_MODE, REGISTERING_PRIMARY_CONNECTION, EXECUTED_COMMAND };
    }

    @Override
    protected void configureExecutionStates(StateMachineBuilder<CreateConnectionFsm, CreateConnectionState, CreateConnectionEvent, CreateConnectionContext> builder) {
        builder.onEntry(CREATING_PRIMARY_CONNECTION).perform(CreateRemotePrimaryConnectionAction.INSTANCE);
        builder.localTransition().from(CREATING_PRIMARY_CONNECTION).to(HALF_OPENING_PRIMARY_CONNECTION).on(CONNECTION_CREATED).when(NoRemoteSessionDescriptionCondition.INSTANCE);
        builder.localTransition().from(CREATING_PRIMARY_CONNECTION).to(OPENING_PRIMARY_CONNECTION).on(CONNECTION_CREATED).when(HasRemoteSessionDescriptionCondition.INSTANCE);;

        builder.onEntry(HALF_OPENING_PRIMARY_CONNECTION).perform(HalfOpenPrimaryConnectionAction.INSTANCE);
        builder.localTransition().from(HALF_OPENING_PRIMARY_CONNECTION).to(UPDATING_PRIMARY_CONNECTION_MODE).on(CONNECTION_OPENED);

        builder.onEntry(OPENING_PRIMARY_CONNECTION).perform(OpenPrimaryConnectionAction.INSTANCE);
        builder.localTransition().from(OPENING_PRIMARY_CONNECTION).to(UPDATING_PRIMARY_CONNECTION_MODE).on(CONNECTION_OPENED);

        builder.onEntry(UPDATING_PRIMARY_CONNECTION_MODE).perform(UpdatePrimaryConnectionModeAction.INSTANCE);
        builder.localTransition().from(UPDATING_PRIMARY_CONNECTION_MODE).to(REGISTERING_PRIMARY_CONNECTION).on(CONNECTION_MODE_UPDATED);

        builder.onEntry(REGISTERING_PRIMARY_CONNECTION).perform(RegisterPrimaryConnectionAction.INSTANCE);
        builder.localTransition().from(REGISTERING_PRIMARY_CONNECTION).to(EXECUTED_COMMAND).on(CONNECTION_REGISTERED);

        builder.onEntry(EXECUTED_COMMAND).perform(ExecutedAction.INSTANCE);
    }

    @Override
    protected CreateConnectionState[] getRollbackStates() {
        return new CreateConnectionState[] {UNREGISTERING_PRIMARY_CONNECTION, CLOSING_PRIMARY_CONNECTION, UNREGISTERING_SECONDARY_CONNECTION, CLOSING_SECONDARY_CONNECTION, CreateConnectionState.ROLLED_BACK};
    }

    @Override
    protected void configureRollbackStates(StateMachineBuilder<CreateConnectionFsm, CreateConnectionState, CreateConnectionEvent, CreateConnectionContext> builder) {
        builder.localTransition().from(ROLLING_BACK).to(UNREGISTERING_PRIMARY_CONNECTION).on(ROLLBACK).when(PrimaryConnectionRegisteredCondition.INSTANCE);
        builder.localTransition().from(ROLLING_BACK).to(CLOSING_PRIMARY_CONNECTION).on(ROLLBACK).when(PrimaryConnectionNotRegisteredAndOpenCondition.INSTANCE);
        builder.localTransition().from(ROLLING_BACK).to(CreateConnectionState.ROLLED_BACK).on(ROLLBACK).when(PrimaryConnectionClosedCondition.INSTANCE);
        
        builder.onEntry(UNREGISTERING_PRIMARY_CONNECTION).perform(UnregisterPrimaryConnectionAction.INSTANCE);
        builder.localTransition().from(UNREGISTERING_PRIMARY_CONNECTION).to(CLOSING_PRIMARY_CONNECTION).on(CONNECTION_UNREGISTERED);

        builder.onEntry(CLOSING_PRIMARY_CONNECTION).perform(ClosePrimaryConnectionAction.INSTANCE);
        builder.localTransition().from(CLOSING_PRIMARY_CONNECTION).to(CreateConnectionState.ROLLED_BACK).on(CONNECTION_CLOSED);
        
        builder.onEntry(CreateConnectionState.ROLLED_BACK).perform(RolledBackAction.INSTANCE);
    }

}
