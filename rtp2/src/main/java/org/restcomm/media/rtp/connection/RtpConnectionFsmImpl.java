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

import com.google.common.util.concurrent.FutureCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.squirrelframework.foundation.fsm.StateMachineStatus;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class RtpConnectionFsmImpl
        extends AbstractStateMachine<RtpConnectionFsm, RtpConnectionState, RtpConnectionEvent, RtpConnectionTransitionContext>
        implements RtpConnectionFsm {

    private static final Logger log = LogManager.getLogger(RtpConnectionFsmImpl.class);

    private final RtpConnectionContext context;

    public RtpConnectionFsmImpl(RtpConnectionContext context) {
        super();
        this.context = context;
    }

    @Override
    public RtpConnectionContext getContext() {
        return this.context;
    }

    @Override
    protected void afterTransitionDeclined(RtpConnectionState fromState, RtpConnectionEvent event, RtpConnectionTransitionContext context) {
        log.warn("RTP Connection " + getContext().getCname() + " declined transition from state " + fromState + " on " + event + " event.");

        final FutureCallback callback = context.get(RtpConnectionTransitionParameter.CALLBACK, FutureCallback.class);
        if (callback != null) {
            callback.onFailure(new IllegalStateException("Cannot transition from " + fromState + " on " + event + " event."));
        }
    }

    @Override
    protected void afterTransitionCausedException(RtpConnectionState fromState, RtpConnectionState toState, RtpConnectionEvent event, RtpConnectionTransitionContext context) {
        final Throwable t = this.getLastException().getTargetException();

        log.error("Unexpected problem in RTP Connection " + getContext().getCname(), t);

        this.setStatus(StateMachineStatus.IDLE);
        this.fireImmediate(RtpConnectionEvent.FAILURE, context);
    }

}
