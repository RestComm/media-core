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
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com) on 28/11/2017
 */
class RaiseQuarantinedEventAction extends NotificationCenterAction {

    static final RaiseQuarantinedEventAction INSTANCE = new RaiseQuarantinedEventAction();

    @SuppressWarnings("unchecked")
    @Override
    public void execute(NotificationCenterState from, NotificationCenterState to, NotificationCenterEvent event, NotificationCenterTransitionContext context, NotificationCenterFsm stateMachine) {
        final SignalQuarantine quarantine = stateMachine.getContext().getQuarantine();
        final FutureCallback<MgcpEvent> callback = context.get(NotificationCenterTransitionParameter.CALLBACK, FutureCallback.class);
        final String requestId = context.get(NotificationCenterTransitionParameter.REQUEST_IDENTIFIER, String.class);
        final String signal = context.get(NotificationCenterTransitionParameter.SIGNAL, String.class);

        if (quarantine.getRequestId().equals(requestId)) {
            quarantine.getSignalResult(signal, callback);
        } else {
            callback.onFailure(new IllegalArgumentException("MGCP RQNT (X:" + requestId + ") is not quarantined. Current RQNT is  X:" + quarantine.getRequestId()));
        }
    }
}
