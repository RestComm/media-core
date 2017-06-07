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

package org.restcomm.media.rtp.session;

import org.restcomm.media.rtp.RtpSessionContext;
import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;

/**
 * Builds Finite State Machine instances for RTP Session.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpSessionFsmBuilder {

    public static final RtpSessionFsmBuilder INSTANCE = new RtpSessionFsmBuilder();
    
    private final StateMachineBuilder<RtpSessionFsm, RtpSessionState, RtpSessionEvent, RtpSessionTransactionContext> builder;

    private RtpSessionFsmBuilder() {
        this.builder = StateMachineBuilderFactory.<RtpSessionFsm, RtpSessionState, RtpSessionEvent, RtpSessionTransactionContext>create(RtpSessionFsmImpl.class, RtpSessionState.class, RtpSessionEvent.class, RtpSessionTransactionContext.class, RtpSessionContext.class);

        this.builder.defineSequentialStatesOn(RtpSessionState.SETUP, RtpSessionState.BINDING, RtpSessionState.CONNECTING);
        this.builder.externalTransition().from(RtpSessionState.SETUP).toFinal(RtpSessionState.CLOSED).on(RtpSessionEvent.CLOSE);
        
        this.builder.onEntry(RtpSessionState.BINDING).callMethod("enterBinding");
        this.builder.localTransition().from(RtpSessionState.BINDING).to(RtpSessionState.CONNECTING).on(RtpSessionEvent.CONNECT);
        this.builder.onExit(RtpSessionState.BINDING).callMethod("exitBinding");

        this.builder.onEntry(RtpSessionState.CONNECTING).callMethod("enterConnecting");
        this.builder.localTransition().from(RtpSessionState.CONNECTING).to(RtpSessionState.OPEN).on(RtpSessionEvent.OPEN);
        this.builder.onExit(RtpSessionState.CONNECTING).callMethod("exitConnecting");

        this.builder.onEntry(RtpSessionState.OPEN).callMethod("enterOpen");
        this.builder.externalTransition().from(RtpSessionState.OPEN).to(RtpSessionState.NEGOTIATING).on(RtpSessionEvent.NEGOTIATE);
        this.builder.externalTransition().from(RtpSessionState.OPEN).toFinal(RtpSessionState.CLOSED).on(RtpSessionEvent.CLOSE);
        this.builder.onExit(RtpSessionState.OPEN).callMethod("exitOpen");

        this.builder.onEntry(RtpSessionState.NEGOTIATING).callMethod("enterNegotiating");
        this.builder.externalTransition().from(RtpSessionState.NEGOTIATING).to(RtpSessionState.OPEN).on(RtpSessionEvent.OPEN);
        this.builder.externalTransition().from(RtpSessionState.NEGOTIATING).toFinal(RtpSessionState.CLOSED).on(RtpSessionEvent.CLOSE);
        this.builder.onExit(RtpSessionState.NEGOTIATING).callMethod("exitNegotiating");
        
        this.builder.onEntry(RtpSessionState.CLOSED).callMethod("enterClosed");
    }

    public RtpSessionFsm build(RtpSessionContext context) {
        return this.builder.newStateMachine(RtpSessionState.SETUP, context);
    }

}
