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

package org.restcomm.media.network.netty.channel;

import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.StateMachineConfiguration;

/**
 * Builder for {@link NettyNetworkChannelFsm}. Use the {@link NettyNetworkChannelFsmBuilder#INSTANCE} to invoke the FSM builder.
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NettyNetworkChannelFsmBuilder {

    public static final NettyNetworkChannelFsmBuilder INSTANCE = new NettyNetworkChannelFsmBuilder();

    private final StateMachineBuilder<NettyNetworkChannelFsm, NettyNetworkChannelState, NettyNetworkChannelEvent, NettyNetworkChannelTransitionContext> builder;

    private NettyNetworkChannelFsmBuilder() {
        this.builder = StateMachineBuilderFactory
                .<NettyNetworkChannelFsm, NettyNetworkChannelState, NettyNetworkChannelEvent, NettyNetworkChannelTransitionContext> create(
                        AsyncNettyNetworkChannelFsm.class, NettyNetworkChannelState.class, NettyNetworkChannelEvent.class,
                        NettyNetworkChannelTransitionContext.class, NettyNetworkChannelGlobalContext.class);
        
        this.builder.externalTransition().from(NettyNetworkChannelState.UNINITIALIZED).to(NettyNetworkChannelState.OPENING).on(NettyNetworkChannelEvent.OPEN);
        
        this.builder.onEntry(NettyNetworkChannelState.OPENING).callMethod("enterOpening");
        this.builder.externalTransition().from(NettyNetworkChannelState.OPENING).to(NettyNetworkChannelState.OPEN).on(NettyNetworkChannelEvent.OPENED);
        this.builder.externalTransition().from(NettyNetworkChannelState.OPENING).to(NettyNetworkChannelState.CLOSED).on(NettyNetworkChannelEvent.CLOSE);
        this.builder.transit().from(NettyNetworkChannelState.OPEN).to(NettyNetworkChannelState.CLOSED).onAny();
        
        this.builder.onExit(NettyNetworkChannelState.OPENING).callMethod("exitOpening");
        
        this.builder.onEntry(NettyNetworkChannelState.OPEN).callMethod("enterOpen");
        this.builder.externalTransition().from(NettyNetworkChannelState.OPEN).to(NettyNetworkChannelState.BINDING).on(NettyNetworkChannelEvent.BIND);
        this.builder.externalTransition().from(NettyNetworkChannelState.OPEN).to(NettyNetworkChannelState.CLOSING).on(NettyNetworkChannelEvent.CLOSE);
        this.builder.onExit(NettyNetworkChannelState.OPEN).callMethod("exitOpen");

        this.builder.onEntry(NettyNetworkChannelState.BINDING).callMethod("enterBinding");
        this.builder.externalTransition().from(NettyNetworkChannelState.BINDING).to(NettyNetworkChannelState.BOUND).on(NettyNetworkChannelEvent.BOUND);
        this.builder.externalTransition().from(NettyNetworkChannelState.BINDING).to(NettyNetworkChannelState.CLOSING).on(NettyNetworkChannelEvent.CLOSE);
        this.builder.onExit(NettyNetworkChannelState.BINDING).callMethod("exitBinding");

        this.builder.onEntry(NettyNetworkChannelState.BOUND).callMethod("enterBound");
        this.builder.externalTransition().from(NettyNetworkChannelState.BOUND).to(NettyNetworkChannelState.CONNECTING).on(NettyNetworkChannelEvent.CONNECT);
        this.builder.externalTransition().from(NettyNetworkChannelState.BOUND).to(NettyNetworkChannelState.CLOSING).on(NettyNetworkChannelEvent.CLOSE);
        this.builder.onExit(NettyNetworkChannelState.BOUND).callMethod("exitBound");
        
        this.builder.onEntry(NettyNetworkChannelState.CONNECTING).callMethod("enterConnecting");
        this.builder.externalTransition().from(NettyNetworkChannelState.CONNECTING).to(NettyNetworkChannelState.CONNECTED).on(NettyNetworkChannelEvent.CONNECTED);
        this.builder.externalTransition().from(NettyNetworkChannelState.CONNECTING).to(NettyNetworkChannelState.CLOSING).on(NettyNetworkChannelEvent.CLOSE);
        this.builder.onExit(NettyNetworkChannelState.CONNECTING).callMethod("exitConnecting");
        
        this.builder.onEntry(NettyNetworkChannelState.CONNECTED).callMethod("enterConnected");
        this.builder.externalTransition().from(NettyNetworkChannelState.CONNECTED).to(NettyNetworkChannelState.DISCONNECTING).on(NettyNetworkChannelEvent.DISCONNECT);
        this.builder.externalTransition().from(NettyNetworkChannelState.CONNECTED).to(NettyNetworkChannelState.CLOSING).on(NettyNetworkChannelEvent.CLOSE);
        this.builder.onExit(NettyNetworkChannelState.CONNECTED).callMethod("exitConnected");

        this.builder.onEntry(NettyNetworkChannelState.DISCONNECTING).callMethod("enterDisconnecting");
        this.builder.externalTransition().from(NettyNetworkChannelState.DISCONNECTING).to(NettyNetworkChannelState.BOUND).on(NettyNetworkChannelEvent.DISCONNECTED);
        this.builder.externalTransition().from(NettyNetworkChannelState.DISCONNECTING).to(NettyNetworkChannelState.CLOSING).on(NettyNetworkChannelEvent.CLOSE);
        this.builder.onExit(NettyNetworkChannelState.DISCONNECTING).callMethod("exitDisconnecting");

        this.builder.onEntry(NettyNetworkChannelState.CLOSING).callMethod("enterClosing");
        this.builder.externalTransition().from(NettyNetworkChannelState.CLOSING).to(NettyNetworkChannelState.CLOSED).on(NettyNetworkChannelEvent.CLOSED);
        this.builder.onExit(NettyNetworkChannelState.CLOSING).callMethod("exitClosing");
        
        this.builder.onEntry(NettyNetworkChannelState.CLOSED).callMethod("enterClosed");
        this.builder.defineFinalState(NettyNetworkChannelState.CLOSED);
    }

    public NettyNetworkChannelFsm build(NettyNetworkChannelGlobalContext context) {
        return this.builder.newStateMachine(NettyNetworkChannelState.UNINITIALIZED, StateMachineConfiguration.getInstance().enableDebugMode(false), context);
    }

}
