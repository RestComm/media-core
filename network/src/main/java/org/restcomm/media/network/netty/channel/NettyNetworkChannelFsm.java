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

import org.squirrelframework.foundation.fsm.StateMachine;

/**
 * Finite State Machine for {@link AsyncNettyNetworkChannel}.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface NettyNetworkChannelFsm extends StateMachine<NettyNetworkChannelFsm, NettyNetworkChannelState, NettyNetworkChannelEvent, NettyNetworkChannelTransitionContext> {

    void enterOpening(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context);

    void exitOpening(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context);

    void enterOpen(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context);

    void exitOpen(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context);

    void enterBinding(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context);

    void exitBinding(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context);

    void enterBound(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context);

    void exitBound(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context);

    void enterConnecting(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context);

    void exitConnecting(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context);

    void enterConnected(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context);

    void exitConnected(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context);

    void enterDisconnecting(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context);

    void exitDisconnecting(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context);

    void enterClosing(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context);

    void exitClosing(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context);

    void enterClosed(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context);

}
