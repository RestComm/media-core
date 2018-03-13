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

package org.restcomm.media.core.control.mgcp.controller.fsm.action;

import java.net.SocketAddress;

import org.restcomm.media.core.control.mgcp.controller.fsm.MgcpControllerEvent;
import org.restcomm.media.core.control.mgcp.controller.fsm.MgcpControllerFsm;
import org.restcomm.media.core.control.mgcp.controller.fsm.MgcpControllerState;
import org.restcomm.media.core.control.mgcp.controller.fsm.transition.MgcpControllerTransitionContext;
import org.squirrelframework.foundation.fsm.Action;
import org.squirrelframework.foundation.fsm.AnonymousAction;

import com.google.common.util.concurrent.FutureCallback;

/**
 * {@link Action} that binds the MGCP Controller's channel to a local address.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class BindChannelAction extends AnonymousAction<MgcpControllerFsm, MgcpControllerState, MgcpControllerEvent, MgcpControllerTransitionContext> {

    @Override
    public void execute(MgcpControllerState from, MgcpControllerState to, MgcpControllerEvent event, MgcpControllerTransitionContext context, MgcpControllerFsm stateMachine) {
        final SocketAddress address = stateMachine.getContext().getBindAddress();
        final BindChannelCallback callback = new BindChannelCallback(stateMachine);
        stateMachine.getContext().getChannel().bind(address, callback);
    }
    
    private class BindChannelCallback implements FutureCallback<Void> {

        private final MgcpControllerFsm fsm;

        public BindChannelCallback(MgcpControllerFsm fsm) {
            super();
            this.fsm = fsm;
        }

        @Override
        public void onSuccess(Void result) {
            this.fsm.fire(MgcpControllerEvent.CHANNEL_BOUND);
        }

        @Override
        public void onFailure(Throwable t) {
            this.fsm.fire(MgcpControllerEvent.DEACTIVATE);
        }
    }

}
