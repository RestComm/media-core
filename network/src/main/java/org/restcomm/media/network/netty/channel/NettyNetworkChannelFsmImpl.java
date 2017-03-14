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

import com.google.common.util.concurrent.FutureCallback;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NettyNetworkChannelFsmImpl extends AbstractStateMachine<NettyNetworkChannelFsm, NettyNetworkChannelState, NettyNetworkChannelEvent, NettyNetworkChannelContext> implements NettyNetworkChannelFsm {
    
    @Override
    public void enterOpen(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelContext context) {
        
        
    }

    @Override
    public void exitOpen(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelContext context) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void enterActive(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelContext context) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onActive(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelContext context) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void exitActive(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelContext context) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void enterClosed(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event,
            NettyNetworkChannelContext context) {
        // TODO Auto-generated method stub
        
    }
    
    static final class ChannelCallback implements ChannelFutureListener {

        private final FutureCallback<Void> observer;

        public ChannelCallback(FutureCallback<Void> observer) {
            super();
            this.observer = observer;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                this.observer.onSuccess(null);
            } else {
                this.observer.onFailure(future.cause());
            }

        }
    }

}
