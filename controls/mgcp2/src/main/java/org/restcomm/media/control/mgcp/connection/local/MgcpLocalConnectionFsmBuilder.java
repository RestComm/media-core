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

package org.restcomm.media.control.mgcp.connection.local;

import java.util.ArrayList;
import java.util.List;

import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpLocalConnectionFsmBuilder {

    private final StateMachineBuilder<MgcpLocalConnectionFsm, MgcpLocalConnectionState, MgcpLocalConnectionEvent, MgcpLocalConnectionTransitionContext> builder;

    public MgcpLocalConnectionFsmBuilder() {
        this.builder = StateMachineBuilderFactory.<MgcpLocalConnectionFsm, MgcpLocalConnectionState, MgcpLocalConnectionEvent, MgcpLocalConnectionTransitionContext> create(MgcpLocalConnectionFsmImpl.class, MgcpLocalConnectionState.class, MgcpLocalConnectionEvent.class, MgcpLocalConnectionTransitionContext.class, MgcpLocalConnectionContext.class);

        this.builder.onEntry(MgcpLocalConnectionState.IDLE);
        this.builder.externalTransition().from(MgcpLocalConnectionState.IDLE).to(MgcpLocalConnectionState.HALF_OPEN).on(MgcpLocalConnectionEvent.HALF_OPEN).perform(NotifyCallbackAction.INSTANCE);
        this.builder.externalTransition().from(MgcpLocalConnectionState.IDLE).to(MgcpLocalConnectionState.OPEN).on(MgcpLocalConnectionEvent.OPEN).perform(NotifyCallbackAction.INSTANCE);
        this.builder.externalTransition().from(MgcpLocalConnectionState.IDLE).toFinal(MgcpLocalConnectionState.CLOSED).on(MgcpLocalConnectionEvent.CLOSE).perform(NotifyCallbackAction.INSTANCE);

        this.builder.onEntry(MgcpLocalConnectionState.HALF_OPEN).perform(ScheduleTimeoutAction.INSTANCE);
        this.builder.externalTransition().from(MgcpLocalConnectionState.HALF_OPEN).to(MgcpLocalConnectionState.OPEN).on(MgcpLocalConnectionEvent.OPEN).perform(NotifyCallbackAction.INSTANCE);
        this.builder.externalTransition().from(MgcpLocalConnectionState.HALF_OPEN).to(MgcpLocalConnectionState.CORRUPTED).on(MgcpLocalConnectionEvent.FAILURE);
        this.builder.externalTransition().from(MgcpLocalConnectionState.HALF_OPEN).toFinal(MgcpLocalConnectionState.CLOSED).on(MgcpLocalConnectionEvent.CLOSE).perform(NotifyCallbackAction.INSTANCE);
        this.builder.externalTransition().from(MgcpLocalConnectionState.HALF_OPEN).toFinal(MgcpLocalConnectionState.CLOSED).on(MgcpLocalConnectionEvent.TIMEOUT);
        this.builder.onExit(MgcpLocalConnectionState.HALF_OPEN).perform(CancelTimeoutAction.INSTANCE);
        
        List<MgcpLocalConnectionAction> joinActions = new ArrayList<>(2);
        joinActions.add(JoinAction.INSTANCE);
        joinActions.add(NotifyCallbackAction.INSTANCE);

        List<MgcpLocalConnectionAction> updateModeActions = new ArrayList<>(2);
        updateModeActions.add(UpdateModeAction.INSTANCE);
        updateModeActions.add(NotifyCallbackAction.INSTANCE);

        this.builder.onEntry(MgcpLocalConnectionState.OPEN).perform(ScheduleTimeoutAction.INSTANCE);
        this.builder.internalTransition().within(MgcpLocalConnectionState.OPEN).on(MgcpLocalConnectionEvent.JOIN).perform(joinActions);
        this.builder.internalTransition().within(MgcpLocalConnectionState.OPEN).on(MgcpLocalConnectionEvent.UPDATE_MODE).perform(updateModeActions);
        this.builder.externalTransition().from(MgcpLocalConnectionState.OPEN).to(MgcpLocalConnectionState.CORRUPTED).on(MgcpLocalConnectionEvent.FAILURE);
        this.builder.externalTransition().from(MgcpLocalConnectionState.OPEN).toFinal(MgcpLocalConnectionState.CLOSED).on(MgcpLocalConnectionEvent.CLOSE).perform(NotifyCallbackAction.INSTANCE);
        this.builder.externalTransition().from(MgcpLocalConnectionState.OPEN).toFinal(MgcpLocalConnectionState.CLOSED).on(MgcpLocalConnectionEvent.TIMEOUT);
        this.builder.onExit(MgcpLocalConnectionState.OPEN).perform(CancelTimeoutAction.INSTANCE);

        this.builder.onEntry(MgcpLocalConnectionState.CORRUPTED).perform(NotifyCallbackAction.INSTANCE);
        this.builder.externalTransition().from(MgcpLocalConnectionState.CORRUPTED).toFinal(MgcpLocalConnectionState.CLOSED).on(MgcpLocalConnectionEvent.CLOSE).perform(NotifyCallbackAction.INSTANCE);

        this.builder.onEntry(MgcpLocalConnectionState.CLOSED).perform(UnjoinAction.INSTANCE);
    }

    public MgcpLocalConnectionFsm build(MgcpLocalConnectionContext context) {
        return this.builder.newStateMachine(MgcpLocalConnectionState.IDLE, context);
    }

}
