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

package org.restcomm.media.control.mgcp.controller.fsm;

import org.restcomm.media.control.mgcp.controller.fsm.action.BindChannelAction;
import org.restcomm.media.control.mgcp.controller.fsm.action.CloseChannelAction;
import org.restcomm.media.control.mgcp.controller.fsm.action.OpenChannelAction;
import org.restcomm.media.control.mgcp.controller.fsm.transition.MgcpControllerTransitionContext;
import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpControllerFsmBuilder {

    public static final MgcpControllerFsmBuilder INSTANCE = new MgcpControllerFsmBuilder();

    private final StateMachineBuilder<MgcpControllerFsm, MgcpControllerState, MgcpControllerEvent, MgcpControllerTransitionContext> builder;

    private MgcpControllerFsmBuilder() {
        this.builder = StateMachineBuilderFactory.<MgcpControllerFsm, MgcpControllerState, MgcpControllerEvent, MgcpControllerTransitionContext> create(MgcpControllerFsmImpl.class, MgcpControllerState.class, MgcpControllerEvent.class, MgcpControllerTransitionContext.class, MgcpControllerGlobalContext.class);

        this.builder.externalTransition().from(MgcpControllerState.UNINITIALIZED).to(MgcpControllerState.ACTIVATING).on(MgcpControllerEvent.ACTIVATE);

        this.builder.defineSequentialStatesOn(MgcpControllerState.ACTIVATING, MgcpControllerState.OPENING_CHANNEL, MgcpControllerState.BINDING_CHANNEL);
        this.builder.onEntry(MgcpControllerState.OPENING_CHANNEL).perform(new OpenChannelAction());
        this.builder.localTransition().from(MgcpControllerState.OPENING_CHANNEL).to(MgcpControllerState.BINDING_CHANNEL).on(MgcpControllerEvent.CHANNEL_OPENED);
        this.builder.onEntry(MgcpControllerState.BINDING_CHANNEL).perform(new BindChannelAction());

        this.builder.externalTransition().from(MgcpControllerState.ACTIVATING).to(MgcpControllerState.ACTIVATED).on(MgcpControllerEvent.CHANNEL_BOUND);
        this.builder.externalTransition().from(MgcpControllerState.ACTIVATING).to(MgcpControllerState.DEACTIVATING).on(MgcpControllerEvent.DEACTIVATE);
        this.builder.externalTransition().from(MgcpControllerState.ACTIVATED).to(MgcpControllerState.DEACTIVATING).on(MgcpControllerEvent.DEACTIVATE);

        this.builder.defineSequentialStatesOn(MgcpControllerState.DEACTIVATING, MgcpControllerState.CLOSING_CHANNEL);
        this.builder.onEntry(MgcpControllerState.CLOSING_CHANNEL).perform(new CloseChannelAction());
        this.builder.externalTransition().from(MgcpControllerState.DEACTIVATING).to(MgcpControllerState.DEACTIVATED).on(MgcpControllerEvent.DEACTIVATED);
        this.builder.defineFinalState(MgcpControllerState.DEACTIVATED);
    }

    public MgcpControllerFsm build(MgcpControllerGlobalContext context) {
        return this.builder.newStateMachine(MgcpControllerState.UNINITIALIZED, context);
    }

}
