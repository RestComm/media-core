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

import static org.restcomm.media.control.mgcp.command.dlcx.DeleteConnectionState.*;
import static org.restcomm.media.control.mgcp.command.dlcx.DeleteConnectionEvent.*;

import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class DeleteConnectionFsmBuilder {

    private final StateMachineBuilder<DeleteConnectionFsm, DeleteConnectionState, DeleteConnectionEvent, DeleteConnectionContext> builder;

    public DeleteConnectionFsmBuilder() {
        builder = StateMachineBuilderFactory.<DeleteConnectionFsm, DeleteConnectionState, DeleteConnectionEvent, DeleteConnectionContext> create(DeleteConnectionFsmImpl.class, DeleteConnectionState.class, DeleteConnectionEvent.class, DeleteConnectionContext.class);

        builder.onEntry(VALIDATING_PARAMETERS).perform(ValidateParametersAction.INSTANCE);
        builder.externalTransition().from(VALIDATING_PARAMETERS).to(EXECUTING).on(VALIDATED_PARAMETERS);
        builder.externalTransition().from(VALIDATING_PARAMETERS).to(FAILED).on(FAILURE);

        builder.defineSequentialStatesOn(EXECUTING, UNREGISTERING_CONNECTIONS, CLOSING_CONNECTIONS, EXECUTED);
        builder.externalTransition().from(EXECUTING).toFinal(SUCCEEDED).on(SUCCESS);
        builder.externalTransition().from(EXECUTING).toFinal(FAILED).on(FAILURE);

        builder.onEntry(UNREGISTERING_CONNECTIONS).perform(UnregisterConnectionsAction.INSTANCE);
        builder.localTransition().from(UNREGISTERING_CONNECTIONS).to(CLOSING_CONNECTIONS).on(UNREGISTERED_CONNECTIONS).when(HasUnregisteredConnectionsCondition.INSTANCE);
        builder.localTransition().from(UNREGISTERING_CONNECTIONS).to(EXECUTED).on(SUCCESS).when(NoUnregisteredConnectionsCondition.INSTANCE);

        builder.onEntry(CLOSING_CONNECTIONS).perform(CloseConnectionsAction.INSTANCE);
        builder.internalTransition().within(CLOSING_CONNECTIONS).on(CLOSED_CONNECTION).perform(IncrementClosedConnectionCountAction.INSTANCE);
        builder.localTransition().from(CLOSING_CONNECTIONS).to(EXECUTED).on(CLOSED_CONNECTIONS);

        builder.onEntry(EXECUTED).perform(ExecutedAction.INSTANCE);

        builder.onEntry(SUCCEEDED).perform(NotifySuccessAction.INSTANCE);

        builder.onEntry(FAILED).perform(NotifyFailureAction.INSTANCE);
    }

    public DeleteConnectionFsm build() {
        return this.builder.newStateMachine(VALIDATING_PARAMETERS);
    }

}
