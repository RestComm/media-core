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

package org.restcomm.media.rtp.connection;

import java.util.ArrayList;
import java.util.List;

import org.squirrelframework.foundation.fsm.Action;
import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpConnectionFsmBuilder {

    public static final RtpConnectionFsmBuilder INSTANCE = new RtpConnectionFsmBuilder();

    private final StateMachineBuilder<RtpConnectionFsm, RtpConnectionState, RtpConnectionEvent, RtpConnectionTransitionContext> builder;

    public RtpConnectionFsmBuilder() {
        this.builder = StateMachineBuilderFactory.<RtpConnectionFsm, RtpConnectionState, RtpConnectionEvent, RtpConnectionTransitionContext> create(RtpConnectionFsmImpl.class, RtpConnectionState.class, RtpConnectionEvent.class, RtpConnectionTransitionContext.class, RtpConnectionContext.class);

        this.builder.onExit(RtpConnectionState.IDLE).perform(SetCallFlowAction.INSTANCE);
        this.builder.externalTransition().from(RtpConnectionState.IDLE).to(RtpConnectionState.PARSING_REMOTE_SDP).on(RtpConnectionEvent.OPEN);
        this.builder.externalTransition().from(RtpConnectionState.IDLE).toFinal(RtpConnectionState.CLOSED).on(RtpConnectionEvent.CLOSE);

        this.builder.onEntry(RtpConnectionState.PARSING_REMOTE_SDP).perform(ParseRemoteDescriptionAction.INSTANCE);
        this.builder.externalTransition().from(RtpConnectionState.PARSING_REMOTE_SDP).to(RtpConnectionState.ALLOCATING_SESSION).on(RtpConnectionEvent.PARSED_REMOTE_SDP).when(NoActiveSessionCondition.INSTANCE);
        this.builder.externalTransition().from(RtpConnectionState.PARSING_REMOTE_SDP).to(RtpConnectionState.CORRUPTED).on(RtpConnectionEvent.FAILURE);
        this.builder.externalTransition().from(RtpConnectionState.PARSING_REMOTE_SDP).to(RtpConnectionState.CLOSED).on(RtpConnectionEvent.CLOSE);

        this.builder.onEntry(RtpConnectionState.ALLOCATING_SESSION).perform(AllocateSessionAction.INSTANCE);
        this.builder.externalTransition().from(RtpConnectionState.ALLOCATING_SESSION).to(RtpConnectionState.NEGOTIATING_SESSION).on(RtpConnectionEvent.ALLOCATED_SESSION).when(HasRemoteDescriptionCondition.INSTANCE);
        this.builder.externalTransition().from(RtpConnectionState.ALLOCATING_SESSION).to(RtpConnectionState.CORRUPTED).on(RtpConnectionEvent.FAILURE);
        this.builder.externalTransition().from(RtpConnectionState.ALLOCATING_SESSION).to(RtpConnectionState.CLOSING).on(RtpConnectionEvent.CLOSE);

        this.builder.onEntry(RtpConnectionState.NEGOTIATING_SESSION).perform(NegotiateSessionAction.INSTANCE);
        this.builder.externalTransition().from(RtpConnectionState.NEGOTIATING_SESSION).to(RtpConnectionState.GENERATING_LOCAL_SDP).on(RtpConnectionEvent.SESSION_NEGOTIATED);
        this.builder.externalTransition().from(RtpConnectionState.NEGOTIATING_SESSION).to(RtpConnectionState.CORRUPTED).on(RtpConnectionEvent.FAILURE);
        this.builder.externalTransition().from(RtpConnectionState.NEGOTIATING_SESSION).to(RtpConnectionState.CLOSING).on(RtpConnectionEvent.CLOSE);

        this.builder.onEntry(RtpConnectionState.GENERATING_LOCAL_SDP).perform(GenerateLocalSdpAction.INSTANCE);
        this.builder.externalTransition().from(RtpConnectionState.GENERATING_LOCAL_SDP).to(RtpConnectionState.OPEN).on(RtpConnectionEvent.GENERATED_LOCAL_SDP);
        this.builder.externalTransition().from(RtpConnectionState.GENERATING_LOCAL_SDP).to(RtpConnectionState.CORRUPTED).on(RtpConnectionEvent.FAILURE);
        this.builder.externalTransition().from(RtpConnectionState.GENERATING_LOCAL_SDP).to(RtpConnectionState.CLOSING).on(RtpConnectionEvent.CLOSE);

        this.builder.onEntry(RtpConnectionState.OPEN).perform(NotifyOpenAction.INSTANCE);
        this.builder.externalTransition().from(RtpConnectionState.OPEN).to(RtpConnectionState.CLOSING).on(RtpConnectionEvent.CLOSE);

        this.builder.onEntry(RtpConnectionState.CORRUPTED).perform(NotifyCorruptAction.INSTANCE);
        this.builder.externalTransition().from(RtpConnectionState.CORRUPTED).to(RtpConnectionState.CLOSING).on(RtpConnectionEvent.CLOSE).when(HasActiveSessionCondition.INSTANCE);
        this.builder.externalTransition().from(RtpConnectionState.CORRUPTED).to(RtpConnectionState.CLOSED).on(RtpConnectionEvent.CLOSE).when(NoActiveSessionCondition.INSTANCE);
        
        this.builder.onEntry(RtpConnectionState.CLOSING).perform(CloseAction.INSTANCE);
        this.builder.externalTransition().from(RtpConnectionState.CLOSING).toFinal(RtpConnectionState.CLOSED).on(RtpConnectionEvent.SESSION_CLOSED);
        
        List<Action<RtpConnectionFsm, RtpConnectionState, RtpConnectionEvent, RtpConnectionTransitionContext>> closeActions = new ArrayList<>(2);
        closeActions.add(NotifyClosedAction.INSTANCE);
        closeActions.add(CleanupContextAction.INSTANCE);
        this.builder.onEntry(RtpConnectionState.CLOSED).perform(closeActions);
    }

    public RtpConnectionFsm build(RtpConnectionContext context) {
        return this.builder.newStateMachine(RtpConnectionState.IDLE, context);
    }

}
