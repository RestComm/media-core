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

        this.builder.externalTransition().from(RtpSessionState.IDLE).to(RtpSessionState.OPENING).on(RtpSessionEvent.OPEN);
        
        this.builder.onEntry(RtpSessionState.OPENING).callMethod("enterOpening");
        this.builder.defineSequentialStatesOn(RtpSessionState.OPENING, RtpSessionState.ALLOCATING, RtpSessionState.BINDING, RtpSessionState.OPENED);
        this.builder.externalTransition().from(RtpSessionState.OPENING).to(RtpSessionState.OPEN).on(RtpSessionEvent.OPENED);
        this.builder.externalTransition().from(RtpSessionState.OPENING).toFinal(RtpSessionState.CLOSED).on(RtpSessionEvent.CLOSE);
        this.builder.onExit(RtpSessionState.OPENING).callMethod("exitOpening");
        
        this.builder.onEntry(RtpSessionState.ALLOCATING).callMethod("enterAllocating");
        this.builder.localTransition().from(RtpSessionState.ALLOCATING).to(RtpSessionState.BINDING).on(RtpSessionEvent.ALLOCATED);
        this.builder.onExit(RtpSessionState.ALLOCATING).callMethod("exitAllocating");

        this.builder.onEntry(RtpSessionState.BINDING).callMethod("enterBinding");
        this.builder.localTransition().from(RtpSessionState.BINDING).toFinal(RtpSessionState.OPENED).on(RtpSessionEvent.BOUND);
        this.builder.onExit(RtpSessionState.BINDING).callMethod("exitBinding");

        this.builder.onEntry(RtpSessionState.OPENED).callMethod("enterOpened");
        this.builder.onExit(RtpSessionState.OPENED).callMethod("exitOpened");

        this.builder.onEntry(RtpSessionState.OPEN).callMethod("enterOpen");
        this.builder.internalTransition().within(RtpSessionState.OPEN).on(RtpSessionEvent.UPDATE_MODE).callMethod("onUpdateMode");
        this.builder.externalTransition().from(RtpSessionState.OPEN).to(RtpSessionState.NEGOTIATING).on(RtpSessionEvent.NEGOTIATE);
        this.builder.externalTransition().from(RtpSessionState.OPEN).toFinal(RtpSessionState.CLOSED).on(RtpSessionEvent.CLOSE);
        this.builder.onExit(RtpSessionState.OPEN).callMethod("exitOpen");

        this.builder.onEntry(RtpSessionState.NEGOTIATING).callMethod("enterNegotiating");
        this.builder.defineSequentialStatesOn(RtpSessionState.NEGOTIATING, RtpSessionState.NEGOTIATING_FORMATS, RtpSessionState.CONNECTING, RtpSessionState.NEGOTIATED);
        this.builder.externalTransition().from(RtpSessionState.NEGOTIATING).to(RtpSessionState.ESTABLISHED).on(RtpSessionEvent.NEGOTIATED);
        this.builder.externalTransition().from(RtpSessionState.NEGOTIATING).toFinal(RtpSessionState.CLOSED).on(RtpSessionEvent.CLOSE);
        this.builder.onExit(RtpSessionState.NEGOTIATING).callMethod("exitNegotiating");

        this.builder.onEntry(RtpSessionState.NEGOTIATING_FORMATS).callMethod("enterNegotiatingFormats");
        this.builder.localTransition().from(RtpSessionState.NEGOTIATING_FORMATS).to(RtpSessionState.CONNECTING).on(RtpSessionEvent.NEGOTIATED_FORMATS);
        this.builder.onExit(RtpSessionState.NEGOTIATING_FORMATS).callMethod("exitNegotiatingFormats");

        this.builder.onEntry(RtpSessionState.CONNECTING).callMethod("enterConnecting");
        this.builder.localTransition().from(RtpSessionState.CONNECTING).toFinal(RtpSessionState.NEGOTIATED).on(RtpSessionEvent.CONNECTED);
        this.builder.onExit(RtpSessionState.CONNECTING).callMethod("exitConnecting");
        
        this.builder.onEntry(RtpSessionState.NEGOTIATED).callMethod("enterNegotiated");
        this.builder.onExit(RtpSessionState.NEGOTIATED).callMethod("exitNegotiated");
        
        this.builder.onEntry(RtpSessionState.ESTABLISHED).callMethod("enterEstablished");
        this.builder.internalTransition().within(RtpSessionState.ESTABLISHED).on(RtpSessionEvent.UPDATE_MODE).callMethod("onUpdateMode");
        this.builder.externalTransition().from(RtpSessionState.ESTABLISHED).to(RtpSessionState.NEGOTIATING).on(RtpSessionEvent.NEGOTIATE);
        this.builder.externalTransition().from(RtpSessionState.ESTABLISHED).toFinal(RtpSessionState.CLOSED).on(RtpSessionEvent.CLOSE);
        this.builder.onExit(RtpSessionState.ESTABLISHED).callMethod("exitEstablished");
        
        this.builder.onEntry(RtpSessionState.CLOSED).callMethod("enterClosed");
    }

    public RtpSessionFsm build(RtpSessionContext context) {
        return this.builder.newStateMachine(RtpSessionState.IDLE, context);
    }

}
