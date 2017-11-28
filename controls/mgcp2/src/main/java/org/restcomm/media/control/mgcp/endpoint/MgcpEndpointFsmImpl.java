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

package org.restcomm.media.control.mgcp.endpoint;

import com.google.common.util.concurrent.FutureCallback;
import org.apache.log4j.Logger;
import org.squirrelframework.foundation.fsm.StateMachineStatus;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class MgcpEndpointFsmImpl extends AbstractStateMachine<MgcpEndpointFsm, MgcpEndpointState, MgcpEndpointEvent, MgcpEndpointTransitionContext> implements MgcpEndpointFsm {

    private static final Logger log = Logger.getLogger(MgcpEndpointFsmImpl.class);

    private final MgcpEndpointContext context;

    @Override
    protected void afterTransitionCausedException(MgcpEndpointState fromState, MgcpEndpointState toState, MgcpEndpointEvent event, MgcpEndpointTransitionContext context) {
        final Throwable t = this.getLastException().getTargetException();

        log.error("Endpoint " + this.context.getEndpointId() + " caught unexpected error.", t);

        // Recover from error
        this.setStatus(StateMachineStatus.IDLE);

        // Warn callback that operation failed
        final FutureCallback callback = context.get(MgcpEndpointParameter.CALLBACK, FutureCallback.class);
        if (callback != null) {
            callback.onFailure(t);
        }
    }

    @Override
    protected void afterTransitionDeclined(MgcpEndpointState fromState, MgcpEndpointEvent event, MgcpEndpointTransitionContext context) {
        final FutureCallback<?> callback = context.get(MgcpEndpointParameter.CALLBACK, FutureCallback.class);

        if (callback != null) {
            // Warn callback that requested operation failed
            final IllegalStateException error = new IllegalStateException("Operation " + event.name() + " not allowed on state " + fromState);
            callback.onFailure(error);
        }
    }

    public MgcpEndpointFsmImpl(MgcpEndpointContext context) {
        super();
        this.context = context;
    }

    @Override
    public MgcpEndpointContext getContext() {
        return this.context;
    }

}
