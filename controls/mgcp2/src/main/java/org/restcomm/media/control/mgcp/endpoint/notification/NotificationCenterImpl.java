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

import org.restcomm.media.control.mgcp.command.NotificationRequest;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NotificationCenterImpl implements NotificationCenter {

    private final NotificationCenterFsm fsm;

    public NotificationCenterImpl(NotificationCenterFsm fsm) {
        this.fsm = fsm;
        this.fsm.start();
    }

    @Override
    public void requestNotification(NotificationRequest request, FutureCallback<Void> callback) {
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.REQUEST_IDENTIFIER, request.getRequestIdentifier());
        txContext.set(NotificationCenterTransitionParameter.NOTIFIED_ENTITY, request.getNotifiedEntity());
        txContext.set(NotificationCenterTransitionParameter.REQUESTED_EVENTS, request.getRequestedEvents());
        txContext.set(NotificationCenterTransitionParameter.REQUESTED_SIGNALS, request.getRequestedSignals());
        txContext.set(NotificationCenterTransitionParameter.NOTIFIED_ENTITY, request.getNotifiedEntity());
        txContext.set(NotificationCenterTransitionParameter.CALLBACK, callback);

        this.fsm.fire(NotificationCenterEvent.NOTIFICATION_REQUEST, txContext);
    }

    @Override
    public void shutdown(FutureCallback<Void> callback) {
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.CALLBACK, callback);

        this.fsm.fire(NotificationCenterEvent.STOP, txContext);
    }

}
