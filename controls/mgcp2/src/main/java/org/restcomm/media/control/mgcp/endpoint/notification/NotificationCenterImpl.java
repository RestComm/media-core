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

import com.google.common.util.concurrent.FutureCallback;
import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.command.rqnt.NotificationRequest;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpEventObserver;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class NotificationCenterImpl implements NotificationCenter {

    private final NotificationCenterFsm fsm;
    private final NotificationCenterContext context;

    public NotificationCenterImpl(NotificationCenterFsm fsm, NotificationCenterContext context) {
        this.context = context;
        this.fsm = fsm;
        this.fsm.start();
    }

    @Override
    public String getRequestId() {
        return this.context.getRequestId();
    }

    @Override
    public NotifiedEntity getNotifiedEntity() {
        return this.context.getNotifiedEntity();
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
    public void endSignal(String requestId, String signal, FutureCallback<MgcpEvent> callback) {
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.REQUEST_IDENTIFIER, requestId);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, signal);
        txContext.set(NotificationCenterTransitionParameter.CALLBACK, callback);

        this.fsm.fire(NotificationCenterEvent.QUERY_QUARANTINED, txContext);
    }

    @Override
    public void shutdown(FutureCallback<Void> callback) {
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.CALLBACK, callback);

        this.fsm.fire(NotificationCenterEvent.STOP, txContext);
    }

    @Override
    public void observe(MgcpEventObserver observer) {
        this.fsm.observe(observer);
    }

    @Override
    public void forget(MgcpEventObserver observer) {
        this.fsm.forget(observer);
    }

    @Override
    public void notify(Object originator, MgcpEvent event) {
        this.fsm.notify(originator, event);
    }

}
