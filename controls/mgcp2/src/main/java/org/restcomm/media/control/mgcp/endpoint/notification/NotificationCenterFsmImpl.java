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

package org.restcomm.media.control.mgcp.endpoint.notification;

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.command.mdcx.ModifyConnectionEvent;
import org.squirrelframework.foundation.fsm.StateMachineStatus;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NotificationCenterFsmImpl extends
        AbstractStateMachine<NotificationCenterFsm, NotificationCenterState, NotificationCenterEvent, NotificationCenterTransitionContext>
        implements NotificationCenterFsm {

    private static final Logger log = Logger.getLogger(NotificationCenterFsmImpl.class);

    private final NotificationCenterContext context;

    public NotificationCenterFsmImpl(NotificationCenterContext context) {
        super();
        this.context = context;
    }

    @Override
    public NotificationCenterContext getContext() {
        return this.context;
    }

    @Override
    protected void afterTransitionCausedException(NotificationCenterState fromState, NotificationCenterState toState, NotificationCenterEvent event, NotificationCenterTransitionContext context) {
        final Throwable t = this.getLastException().getTargetException();
        log.error("Unexpected error in MGCP transaction " + context.getTransactionId() + ". Aborting transaction.", t);

        this.setStatus(StateMachineStatus.IDLE);
        // this.fire(NotificationCenterEvent.FAILURE, context);
        // TODO rollback NotificationCenterFsm
    }
    
    @Override
    protected void afterTransitionDeclined(NotificationCenterState fromState, NotificationCenterEvent event, NotificationCenterTransitionContext context) {
        final IllegalStateException error = new IllegalStateException("Operation " + event.name() + " not allowed on state " + fromState);
        FutureCallback<?> callback = context.get(NotificationCenterTransitionParameter.CALLBACK, FutureCallback.class);
        callback.onFailure(error);
    }

}
