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
import org.restcomm.media.control.mgcp.command.rqnt.NotificationRequest;
import org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenter;
import org.squirrelframework.foundation.fsm.AnonymousAction;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com) on 27/11/2017
 */
class RequestNotificationAction extends AnonymousAction<MgcpEndpointFsm, MgcpEndpointState, MgcpEndpointEvent, MgcpEndpointTransitionContext> implements MgcpEndpointAction {

    static final RequestNotificationAction INSTANCE = new RequestNotificationAction();

    @Override
    public void execute(MgcpEndpointState from, MgcpEndpointState to, MgcpEndpointEvent event, MgcpEndpointTransitionContext context, MgcpEndpointFsm stateMachine) {
        final NotificationCenter notificationCenter = stateMachine.getContext().getNotificationCenter();
        final FutureCallback callback = context.get(MgcpEndpointParameter.CALL_ID, FutureCallback.class);
        final NotificationRequest notificationRequest = context.get(MgcpEndpointParameter.REQUESTED_NOTIFICATION, NotificationRequest.class);

        notificationCenter.requestNotification(notificationRequest, callback);
    }

}
