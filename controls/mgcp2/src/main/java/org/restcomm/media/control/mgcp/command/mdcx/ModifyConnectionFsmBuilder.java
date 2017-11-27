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

package org.restcomm.media.control.mgcp.command.mdcx;

import static org.restcomm.media.control.mgcp.command.mdcx.ModifyConnectionState.*;
import static org.restcomm.media.control.mgcp.command.mdcx.ModifyConnectionEvent.*;

import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ModifyConnectionFsmBuilder {

    public static final ModifyConnectionFsmBuilder INSTANCE = new ModifyConnectionFsmBuilder();

    private final StateMachineBuilder<ModifyConnectionFsm, ModifyConnectionState, ModifyConnectionEvent, ModifyConnectionContext> builder;

    private ModifyConnectionFsmBuilder() {
        this.builder = StateMachineBuilderFactory.<ModifyConnectionFsm, ModifyConnectionState, ModifyConnectionEvent, ModifyConnectionContext> create(ModifyConnectionFsmImpl.class, ModifyConnectionState.class, ModifyConnectionEvent.class, ModifyConnectionContext.class);

        this.builder.onEntry(VALIDATING_PARAMETERS).perform(ValidateParametersAction.INSTANCE);
        this.builder.externalTransition().from(VALIDATING_PARAMETERS).to(EXECUTING).on(VALIDATED_PARAMETERS);
        this.builder.externalTransition().from(VALIDATING_PARAMETERS).to(FAILED).on(FAILURE);

        this.builder.onEntry(EXECUTING).perform(StartExecutingAction.INSTANCE);
        this.builder.defineNoInitSequentialStatesOn(EXECUTING, MODIFYING_CONNECTION, UPDATING_CONNECTION_MODE, ModifyConnectionState.EXECUTED);
        this.builder.localTransition().from(EXECUTING).to(MODIFYING_CONNECTION).on(EXECUTE).when(HasRemoteDescriptionCondition.INSTANCE);
        this.builder.localTransition().from(EXECUTING).to(UPDATING_CONNECTION_MODE).on(EXECUTE).when(HasModeAndNoRemoteDescriptionCondition.INSTANCE);
        this.builder.localTransition().from(EXECUTING).to(ModifyConnectionState.EXECUTED).on(EXECUTE).when(NoModeAndNoRemoteDescriptionCondition.INSTANCE);
        this.builder.externalTransition().from(EXECUTING).toFinal(SUCCEEDED).on(ModifyConnectionEvent.EXECUTED);
        this.builder.externalTransition().from(EXECUTING).toFinal(FAILED).on(FAILURE);

        this.builder.onEntry(MODIFYING_CONNECTION).perform(NegotiateConnectionAction.INSTANCE);
        this.builder.localTransition().from(MODIFYING_CONNECTION).to(UPDATING_CONNECTION_MODE).on(MODIFIED_CONNECTION).when(HasModeCondition.INSTANCE);
        this.builder.localTransition().from(MODIFYING_CONNECTION).to(ModifyConnectionState.EXECUTED).on(MODIFIED_CONNECTION).when(NoModeCondition.INSTANCE);

        this.builder.onEntry(UPDATING_CONNECTION_MODE).perform(UpdateConnectionModeAction.INSTANCE);
        this.builder.localTransition().from(UPDATING_CONNECTION_MODE).to(ModifyConnectionState.EXECUTED).on(UPDATED_CONNECTION_MODE);

        this.builder.onEntry(ModifyConnectionState.EXECUTED).perform(StopExecutingAction.INSTANCE);

        this.builder.onEntry(UPDATING_CONNECTION_MODE).perform(UpdateConnectionModeAction.INSTANCE);

        this.builder.onEntry(SUCCEEDED).perform(RespondSuccessAction.INSTANCE);

        this.builder.onEntry(FAILED).perform(RespondFailureAction.INSTANCE);
    }
    
    public ModifyConnectionFsm build() {
        return this.builder.newStateMachine(VALIDATING_PARAMETERS);
    }

}
