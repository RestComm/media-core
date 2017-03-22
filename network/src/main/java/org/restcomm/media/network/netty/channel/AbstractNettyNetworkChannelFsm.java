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

import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

/**
 * Abstract implementation of {@link NettyNetworkChannelFsm}. Transition methods must be overridden by concrete implementation
 * as a way to define behavior in very specific points of FSM.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class AbstractNettyNetworkChannelFsm extends
        AbstractStateMachine<NettyNetworkChannelFsm, NettyNetworkChannelState, NettyNetworkChannelEvent, NettyNetworkChannelTransitionContext>
        implements NettyNetworkChannelFsm {

    @Override
    public void enterOpening(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelTransitionContext context) {
    }

    @Override
    public void exitOpening(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelTransitionContext context) {
    }

    @Override
    public void enterOpen(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelTransitionContext context) {
    }

    @Override
    public void exitOpen(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelTransitionContext context) {
    }

    @Override
    public void enterBinding(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelTransitionContext context) {
    }

    @Override
    public void exitBinding(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelTransitionContext context) {
    }

    @Override
    public void enterBound(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelTransitionContext context) {
    }

    @Override
    public void exitBound(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelTransitionContext context) {
    }

    @Override
    public void enterConnecting(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelTransitionContext context) {
    }

    @Override
    public void exitConnecting(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelTransitionContext context) {
    }

    @Override
    public void enterConnected(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelTransitionContext context) {
    }

    @Override
    public void exitConnected(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelTransitionContext context) {
    }

    @Override
    public void enterDisconnecting(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelTransitionContext context) {
    }

    @Override
    public void exitDisconnecting(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelTransitionContext context) {
    }

    @Override
    public void enterClosing(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelTransitionContext context) {
    }

    @Override
    public void exitClosing(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelTransitionContext context) {
    }

    @Override
    public void enterClosed(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelTransitionContext context) {
    }

}
