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
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
abstract class AbstractCreateConnectionFsmBuilder implements CreateConnectionFsmBuilder {
    
    private final StateMachineBuilder<CreateConnectionFsm, CreateConnectionState, CreateConnectionEvent, CreateConnectionContext> builder;
    
    public AbstractCreateConnectionFsmBuilder() {
        this.builder = StateMachineBuilderFactory.<CreateConnectionFsm, CreateConnectionState, CreateConnectionEvent, CreateConnectionContext>create(CreateConnectionFsmImpl.class, CreateConnectionState.class, CreateConnectionEvent.class, CreateConnectionContext.class);
        
        this.builder.onEntry(CreateConnectionState.VALIDATING_PARAMETERS).perform(ValidateParametersAction.INSTANCE);
        this.builder.externalTransition().from(CreateConnectionState.VALIDATING_PARAMETERS).to(CreateConnectionState.EXECUTING).on(CreateConnectionEvent.VALIDATED_PARAMETERS);
        this.builder.externalTransition().from(CreateConnectionState.VALIDATING_PARAMETERS).toFinal(CreateConnectionState.FAILED).on(CreateConnectionEvent.FAILURE);
        
        this.builder.defineSequentialStatesOn(CreateConnectionState.EXECUTING, getExecutionStates());
        this.builder.externalTransition().from(CreateConnectionState.EXECUTING).to(CreateConnectionState.ROLLING_BACK).on(CreateConnectionEvent.FAILURE);
        this.builder.externalTransition().from(CreateConnectionState.EXECUTING).toFinal(CreateConnectionState.SUCCEEDED).on(CreateConnectionEvent.EXECUTED);
        this.configureExecutionStates(this.builder);

        this.builder.onEntry(CreateConnectionState.ROLLING_BACK).perform(StartRollbackAction.INSTANCE);
        this.builder.defineNoInitSequentialStatesOn(CreateConnectionState.ROLLING_BACK, getRollbackStates());
        this.builder.externalTransition().from(CreateConnectionState.ROLLING_BACK).toFinal(CreateConnectionState.FAILED).on(CreateConnectionEvent.ROLLED_BACK);
        this.configureRollbackStates(this.builder);
        
        this.builder.onEntry(CreateConnectionState.SUCCEEDED).perform(RespondSuccessAction.INSTANCE);
        this.builder.onEntry(CreateConnectionState.FAILED).perform(RespondFailureAction.INSTANCE);
    }
    
    protected abstract CreateConnectionState[] getExecutionStates();
    
    protected abstract void configureExecutionStates(StateMachineBuilder<CreateConnectionFsm, CreateConnectionState, CreateConnectionEvent, CreateConnectionContext> builder);

    protected abstract CreateConnectionState[] getRollbackStates();

    protected abstract void configureRollbackStates(StateMachineBuilder<CreateConnectionFsm, CreateConnectionState, CreateConnectionEvent, CreateConnectionContext> builder);
    
    public CreateConnectionFsm build() {
        return this.builder.newStateMachine(CreateConnectionState.VALIDATING_PARAMETERS);
    }

}
