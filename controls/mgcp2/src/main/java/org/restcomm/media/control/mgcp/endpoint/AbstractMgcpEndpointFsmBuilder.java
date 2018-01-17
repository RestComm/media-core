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

package org.restcomm.media.control.mgcp.endpoint;

import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;

import java.util.List;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class AbstractMgcpEndpointFsmBuilder {

    protected final StateMachineBuilder<MgcpEndpointFsm, MgcpEndpointState, MgcpEndpointEvent, MgcpEndpointTransitionContext> builder;

    protected AbstractMgcpEndpointFsmBuilder() {
        this.builder = StateMachineBuilderFactory.<MgcpEndpointFsm, MgcpEndpointState, MgcpEndpointEvent, MgcpEndpointTransitionContext>create(MgcpEndpointFsmImpl.class, MgcpEndpointState.class, MgcpEndpointEvent.class, MgcpEndpointTransitionContext.class, MgcpEndpointContext.class);

        this.builder.onEntry(MgcpEndpointState.IDLE).perform(getDeactivationActions());
        this.builder.internalTransition().within(MgcpEndpointState.IDLE).on(MgcpEndpointEvent.REGISTER_CONNECTION).perform(RegisterConnectionAction.INSTANCE);
        this.builder.externalTransition().from(MgcpEndpointState.IDLE).to(MgcpEndpointState.ACTIVE).on(MgcpEndpointEvent.REGISTERED_CONNECTION).perform(getRegisteredConnectionActions());
        this.builder.externalTransition().from(MgcpEndpointState.IDLE).toFinal(MgcpEndpointState.TERMINATED).on(MgcpEndpointEvent.TERMINATE);

        this.builder.onEntry(MgcpEndpointState.ACTIVE).perform(getActivationActions());
        this.builder.internalTransition().within(MgcpEndpointState.ACTIVE).on(MgcpEndpointEvent.REQUEST_NOTIFICATION).perform(RequestNotificationAction.INSTANCE);
        this.builder.internalTransition().within(MgcpEndpointState.ACTIVE).on(MgcpEndpointEvent.REGISTER_CONNECTION).perform(RegisterConnectionAction.INSTANCE);
        this.builder.internalTransition().within(MgcpEndpointState.ACTIVE).on(MgcpEndpointEvent.REGISTERED_CONNECTION).perform(getRegisteredConnectionActions());
        this.builder.internalTransition().within(MgcpEndpointState.ACTIVE).on(MgcpEndpointEvent.UNREGISTER_CONNECTION).when(UnregisterAllConnectionsCondition.INSTANCE).perform(UnregisterAllConnectionsAction.INSTANCE);
        this.builder.internalTransition().within(MgcpEndpointState.ACTIVE).on(MgcpEndpointEvent.UNREGISTER_CONNECTION).when(UnregisterConnectionsByCallCondition.INSTANCE).perform(UnregisterConnectionsByCallAction.INSTANCE);
        this.builder.internalTransition().within(MgcpEndpointState.ACTIVE).on(MgcpEndpointEvent.UNREGISTER_CONNECTION).when(UnregisterConnectionCondition.INSTANCE).perform(UnregisterConnectionAction.INSTANCE);
        this.builder.internalTransition().within(MgcpEndpointState.ACTIVE).on(MgcpEndpointEvent.UNREGISTERED_CONNECTION).when(HasConnectionsCondition.INSTANCE).perform(getUnregisteredConnectionActions());
        this.builder.externalTransition().from(MgcpEndpointState.ACTIVE).to(MgcpEndpointState.IDLE).on(MgcpEndpointEvent.UNREGISTERED_CONNECTION).when(NoConnectionsCondition.INSTANCE).perform(getUnregisteredConnectionActions());

        this.builder.onEntry(MgcpEndpointState.TERMINATED).perform(ShutdownNotificationCenterAction.INSTANCE);
    }

    public MgcpEndpointFsm build(MgcpEndpointContext context) {
        return this.builder.newStateMachine(MgcpEndpointState.IDLE, context);
    }
    
    protected abstract List<MgcpEndpointAction> getActivationActions();

    protected abstract List<MgcpEndpointAction> getDeactivationActions();

    protected abstract List<MgcpEndpointAction> getRegisteredConnectionActions();

    protected abstract List<MgcpEndpointAction> getUnregisteredConnectionActions();

}
