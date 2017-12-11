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

import java.util.Queue;
import java.util.Set;

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.signal.BriefSignal;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
class CancelAllSignalsAction extends NotificationCenterAction {

    private static final Logger log = Logger.getLogger(CancelAllSignalsAction.class);

    static final CancelAllSignalsAction INSTANCE = new CancelAllSignalsAction();

    CancelAllSignalsAction() {
        super();
    }

    @Override
    public void execute(NotificationCenterState from, NotificationCenterState to, NotificationCenterEvent event, NotificationCenterTransitionContext context, NotificationCenterFsm stateMachine) {
        final NotificationCenterContext globalContext = stateMachine.getContext();

        // Cancel pending brief signals
        final Queue<BriefSignal> briefSignals = globalContext.getPendingBriefSignals();

        if (log.isDebugEnabled() && !briefSignals.isEmpty()) {
            final String endpointId = globalContext.getEndpointId();
            log.debug("Endpoint " + endpointId + " canceled pending brief signals " + briefSignals.toString());
        }

        briefSignals.clear();

        // Cancel active timeout signals
        final Set<TimeoutSignal> timeoutSignals = globalContext.getTimeoutSignals();

        if (log.isDebugEnabled() && !timeoutSignals.isEmpty()) {
            final String endpointId = globalContext.getEndpointId();
            log.debug("Endpoint " + endpointId + " canceled active timeout signals " + timeoutSignals.toString());
        }

        for (TimeoutSignal signal : timeoutSignals) {
            final TimeoutSignalCancellationCallback callback = new TimeoutSignalCancellationCallback(signal, stateMachine);
            signal.cancel(callback);
        }
    }

}
