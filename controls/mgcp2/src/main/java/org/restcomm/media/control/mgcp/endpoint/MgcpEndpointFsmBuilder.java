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

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpEndpointFsmBuilder {

    private final StateMachineBuilder<MgcpEndpointFsm, MgcpEndpointState, MgcpEndpointEvent, MgcpEndpointTransitionContext> builder;

    public MgcpEndpointFsmBuilder() {
        this.builder = StateMachineBuilderFactory.create(MgcpEndpointFsm.class, MgcpEndpointState.class, MgcpEndpointEvent.class, MgcpEndpointTransitionContext.class, MgcpEndpointContext.class);

        this.builder.onEntry(MgcpEndpointState.IDLE);
        this.builder.internalTransition().within(MgcpEndpointState.IDLE).on(MgcpEndpointEvent.REGISTER_CONNECTION).perform(RegisterConnectionAction.INSTANCE);
        this.builder.externalTransition().from(MgcpEndpointState.IDLE).to(MgcpEndpointState.ACTIVE).on(MgcpEndpointEvent.REGISTERED_CONNECTION);
        this.builder.externalTransition().from(MgcpEndpointState.IDLE).to(MgcpEndpointState.TERMINATED).on(MgcpEndpointEvent.TERMINATE);

        this.builder.onEntry(MgcpEndpointState.ACTIVE);
        this.builder.internalTransition().within(MgcpEndpointState.ACTIVE).on(MgcpEndpointEvent.REGISTER_CONNECTION).perform(RegisterConnectionAction.INSTANCE);
        this.builder.internalTransition().within(MgcpEndpointState.ACTIVE).on(MgcpEndpointEvent.REGISTERED_CONNECTION);
        this.builder.internalTransition().within(MgcpEndpointState.ACTIVE).on(MgcpEndpointEvent.UNREGISTER_CONNECTION).when(UnregisterAllConnectionsCondition.INSTANCE).perform(UnregisterAllConnectionsAction.INSTANCE);
        this.builder.internalTransition().within(MgcpEndpointState.ACTIVE).on(MgcpEndpointEvent.UNREGISTER_CONNECTION).when(UnregisterConnectionsByCallCondition.INSTANCE).perform(UnregisterConnectionsByCallAction.INSTANCE);
        this.builder.internalTransition().within(MgcpEndpointState.ACTIVE).on(MgcpEndpointEvent.UNREGISTER_CONNECTION).when(UnregisterConnectionCondition.INSTANCE).perform(UnregisterConnectionAction.INSTANCE);
        this.builder.internalTransition().within(MgcpEndpointState.ACTIVE).on(MgcpEndpointEvent.UNREGISTERED_CONNECTION).when(HasConnectionsCondition.INSTANCE);
        this.builder.externalTransition().from(MgcpEndpointState.ACTIVE).to(MgcpEndpointState.IDLE).on(MgcpEndpointEvent.UNREGISTERED_CONNECTION).when(NoConnectionsCondition.INSTANCE);

        this.builder.onEntry(MgcpEndpointState.TERMINATED);

    }

    public MgcpEndpointFsm build(MgcpEndpointContext context) {
        return null;
    }

}
