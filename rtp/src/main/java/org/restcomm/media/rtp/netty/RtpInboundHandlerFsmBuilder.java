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

package org.restcomm.media.rtp.netty;

import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;

/**
 * Builds instances of {@link RtpInboundHandlerFsm}.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpInboundHandlerFsmBuilder {

    public static final RtpInboundHandlerFsmBuilder INSTANCE = new RtpInboundHandlerFsmBuilder();

    private final StateMachineBuilder<RtpInboundHandlerFsm, RtpInboundHandlerState, RtpInboundHandlerEvent, RtpInboundHandlerTransactionContext> builder;

    public RtpInboundHandlerFsmBuilder() {
        this.builder = StateMachineBuilderFactory.<RtpInboundHandlerFsm, RtpInboundHandlerState, RtpInboundHandlerEvent, RtpInboundHandlerTransactionContext> create(RtpInboundHandlerFsmImpl.class, RtpInboundHandlerState.class, RtpInboundHandlerEvent.class, RtpInboundHandlerTransactionContext.class, RtpInboundHandlerGlobalContext.class);

        this.builder.internalTransition().within(RtpInboundHandlerState.IDLE).on(RtpInboundHandlerEvent.MODE_CHANGED).callMethod("onModeChanged");
        this.builder.internalTransition().within(RtpInboundHandlerState.IDLE).on(RtpInboundHandlerEvent.FORMAT_CHANGED).callMethod("onFormatChanged");
        this.builder.externalTransition().from(RtpInboundHandlerState.IDLE).to(RtpInboundHandlerState.ACTIVATED).on(RtpInboundHandlerEvent.ACTIVATE);
        this.builder.externalTransition().from(RtpInboundHandlerState.IDLE).toFinal(RtpInboundHandlerState.DEACTIVATED).on(RtpInboundHandlerEvent.DEACTIVATE);
        
        this.builder.onEntry(RtpInboundHandlerState.ACTIVATED).callMethod("enterActivated");
        this.builder.internalTransition().within(RtpInboundHandlerState.ACTIVATED).on(RtpInboundHandlerEvent.MODE_CHANGED).callMethod("onModeChanged");
        this.builder.internalTransition().within(RtpInboundHandlerState.ACTIVATED).on(RtpInboundHandlerEvent.FORMAT_CHANGED).callMethod("onFormatChanged");
        this.builder.internalTransition().within(RtpInboundHandlerState.ACTIVATED).on(RtpInboundHandlerEvent.PACKET_RECEIVED).callMethod("onPacketReceived");
        this.builder.externalTransition().from(RtpInboundHandlerState.ACTIVATED).toFinal(RtpInboundHandlerState.DEACTIVATED).on(RtpInboundHandlerEvent.DEACTIVATE);
        this.builder.onExit(RtpInboundHandlerState.ACTIVATED).callMethod("exitActivated");

        this.builder.onEntry(RtpInboundHandlerState.DEACTIVATED).callMethod("enterDeactivated");
        this.builder.onExit(RtpInboundHandlerState.DEACTIVATED).callMethod("exitDeactivated");
    }

    public RtpInboundHandlerFsm build(RtpInboundHandlerGlobalContext context) {
        return this.builder.newStateMachine(RtpInboundHandlerState.IDLE, context);
    }

}
