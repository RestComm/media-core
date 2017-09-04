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
public class CreateLocalConnectionsFsmBuilder extends AbstractCreateConnectionFsmBuilder {

    public static final CreateLocalConnectionsFsmBuilder INSTANCE = new CreateLocalConnectionsFsmBuilder();

    CreateLocalConnectionsFsmBuilder() {
        super();
    }

    @Override
    protected CreateConnectionState[] getExecutionStates() {
        return new CreateConnectionState[] { CREATING_PRIMARY_CONNECTION, OPENING_PRIMARY_CONNECTION,
                CREATING_SECONDARY_CONNECTION, OPENING_SECONDARY_CONNECTION, JOINING_CONNECTIONS,
                UPDATING_PRIMARY_CONNECTION_MODE, UPDATING_SECONDARY_CONNECTION_MODE, REGISTERING_PRIMARY_CONNECTION,
                REGISTERING_SECONDARY_CONNECTION, EXECUTED_COMMAND };
    }

    @Override
    protected void configureExecutionStates(StateMachineBuilder<CreateConnectionFsm, CreateConnectionState, CreateConnectionEvent, CreateConnectionContext> builder) {
        builder.onEntry(CREATING_PRIMARY_CONNECTION).perform(CreateLocalPrimaryConnectionAction.INSTANCE);
        builder.localTransition().from(CREATING_PRIMARY_CONNECTION).to(OPENING_PRIMARY_CONNECTION).on(CONNECTION_CREATED);

        builder.onEntry(OPENING_PRIMARY_CONNECTION).perform(OpenPrimaryConnectionAction.INSTANCE);
        builder.localTransition().from(OPENING_PRIMARY_CONNECTION).to(CREATING_SECONDARY_CONNECTION).on(CONNECTION_OPENED);

        builder.onEntry(CREATING_SECONDARY_CONNECTION).perform(CreateLocalSecondaryConnectionAction.INSTANCE);
        builder.localTransition().from(CREATING_SECONDARY_CONNECTION).to(OPENING_SECONDARY_CONNECTION).on(CONNECTION_CREATED);

        builder.onEntry(OPENING_SECONDARY_CONNECTION).perform(OpenSecondaryConnectionAction.INSTANCE);
        builder.localTransition().from(OPENING_SECONDARY_CONNECTION).to(JOINING_CONNECTIONS).on(CONNECTION_OPENED);

        builder.onEntry(JOINING_CONNECTIONS).perform(JoinConnectionsAction.INSTANCE);
        builder.localTransition().from(JOINING_CONNECTIONS).to(UPDATING_PRIMARY_CONNECTION_MODE).on(CONNECTIONS_JOINED);

        builder.onEntry(UPDATING_PRIMARY_CONNECTION_MODE).perform(UpdatePrimaryConnectionModeAction.INSTANCE);
        builder.localTransition().from(UPDATING_PRIMARY_CONNECTION_MODE).to(UPDATING_SECONDARY_CONNECTION_MODE).on(CONNECTION_MODE_UPDATED);

        builder.onEntry(UPDATING_SECONDARY_CONNECTION_MODE).perform(UpdateSecondaryConnectionModeAction.INSTANCE);
        builder.localTransition().from(UPDATING_SECONDARY_CONNECTION_MODE).to(REGISTERING_PRIMARY_CONNECTION).on(CONNECTION_MODE_UPDATED);

        builder.onEntry(REGISTERING_PRIMARY_CONNECTION).perform(RegisterPrimaryConnectionAction.INSTANCE);
        builder.localTransition().from(REGISTERING_PRIMARY_CONNECTION).to(REGISTERING_SECONDARY_CONNECTION).on(CONNECTION_REGISTERED);

        builder.onEntry(REGISTERING_SECONDARY_CONNECTION).perform(RegisterSecondaryConnectionAction.INSTANCE);
        builder.localTransition().from(REGISTERING_SECONDARY_CONNECTION).to(EXECUTED_COMMAND).on(CONNECTION_REGISTERED);

        builder.onEntry(EXECUTED_COMMAND).perform(ExecutedAction.INSTANCE);
    }

    @Override
    protected CreateConnectionState[] getRollbackStates() {
        return new CreateConnectionState[] {UNREGISTERING_PRIMARY_CONNECTION, CLOSING_PRIMARY_CONNECTION, UNREGISTERING_SECONDARY_CONNECTION, CLOSING_SECONDARY_CONNECTION, CreateConnectionState.ROLLED_BACK};
    }

    @Override
    protected void configureRollbackStates(StateMachineBuilder<CreateConnectionFsm, CreateConnectionState, CreateConnectionEvent, CreateConnectionContext> builder) {
        builder.localTransition().from(ROLLING_BACK).to(UNREGISTERING_PRIMARY_CONNECTION).on(ROLLBACK).when(PrimaryConnectionRegisteredCondition.INSTANCE);
        builder.localTransition().from(ROLLING_BACK).to(CLOSING_PRIMARY_CONNECTION).on(ROLLBACK).when(PrimaryConnectionNotRegisteredAndOpenedCondition.INSTANCE);
        builder.localTransition().from(ROLLING_BACK).to(CreateConnectionState.ROLLED_BACK).on(ROLLBACK).when(PrimaryConnectionClosedCondition.INSTANCE);
        
        builder.onEntry(UNREGISTERING_PRIMARY_CONNECTION).perform(UnregisterPrimaryConnectionAction.INSTANCE);
        builder.localTransition().from(UNREGISTERING_PRIMARY_CONNECTION).to(CLOSING_PRIMARY_CONNECTION).on(CONNECTION_UNREGISTERED);

        builder.onEntry(CLOSING_PRIMARY_CONNECTION).perform(ClosePrimaryConnectionAction.INSTANCE);
        builder.localTransition().from(CLOSING_PRIMARY_CONNECTION).to(UNREGISTERING_SECONDARY_CONNECTION).on(CONNECTION_CLOSED).when(SecondaryConnectionRegisteredCondition.INSTANCE);
        builder.localTransition().from(CLOSING_PRIMARY_CONNECTION).to(CreateConnectionState.CLOSING_SECONDARY_CONNECTION).on(CONNECTION_CLOSED).when(SecondaryConnectionNotRegisteredAndOpenedCondition.INSTANCE);
        builder.localTransition().from(CLOSING_PRIMARY_CONNECTION).to(CreateConnectionState.ROLLED_BACK).on(CONNECTION_CLOSED).when(SecondaryConnectionClosedCondition.INSTANCE);
        
        builder.onEntry(UNREGISTERING_SECONDARY_CONNECTION).perform(UnregisterSecondaryConnectionAction.INSTANCE);
        builder.localTransition().from(UNREGISTERING_SECONDARY_CONNECTION).to(CLOSING_SECONDARY_CONNECTION).on(CONNECTION_UNREGISTERED);
        
        builder.onEntry(CLOSING_SECONDARY_CONNECTION).perform(CloseSecondaryConnectionAction.INSTANCE);
        builder.localTransition().from(CLOSING_SECONDARY_CONNECTION).to(CreateConnectionState.ROLLED_BACK).on(CreateConnectionEvent.CONNECTION_CLOSED);
        
        builder.onEntry(CreateConnectionState.ROLLED_BACK).perform(RolledBackAction.INSTANCE);
    }

}
