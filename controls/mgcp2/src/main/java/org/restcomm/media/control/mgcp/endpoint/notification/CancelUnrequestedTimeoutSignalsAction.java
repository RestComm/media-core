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

import java.util.Set;

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

/**
 * Cancels all active signals that were not requested in a new RQNT.
 * <p>
 * Input parameters:
 * <ul>
 * <li>UNREQUESTED_TIMEOUT_SIGNALS</li>
 * </ul>
 * </p>
 * <p>
 * Output parameters:
 * <ul>
 * <li>n/a
 * <li>
 * </ul>
 * </p>
 *
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
class CancelUnrequestedTimeoutSignalsAction extends NotificationCenterAction {

    private static final Logger log = Logger.getLogger(CancelUnrequestedTimeoutSignalsAction.class);

    static final CancelUnrequestedTimeoutSignalsAction INSTANCE = new CancelUnrequestedTimeoutSignalsAction();

    CancelUnrequestedTimeoutSignalsAction() {
        super();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(NotificationCenterState from, NotificationCenterState to, NotificationCenterEvent event, NotificationCenterTransitionContext context, NotificationCenterFsm stateMachine) {
        final Set<TimeoutSignal> unrequestedSignals = context.get(NotificationCenterTransitionParameter.UNREQUESTED_TIMEOUT_SIGNALS, Set.class);

        if (log.isDebugEnabled() && !unrequestedSignals.isEmpty()) {
            final String endpointId = stateMachine.getContext().getEndpointId();
            log.debug("Endpoint " + endpointId + " canceled active timeout signals " + unrequestedSignals.toString());
        }

        // Cancel all ongoing TO signals that are not listed in new request
        cancelAll(unrequestedSignals, stateMachine);
    }

    private void cancel(TimeoutSignal signal, NotificationCenterFsm stateMachine) {
        final TimeoutSignalCancellationCallback callback = new TimeoutSignalCancellationCallback(signal, stateMachine);
        signal.cancel(callback);
    }

    private void cancelAll(Set<TimeoutSignal> signals, NotificationCenterFsm stateMachine) {
        for (TimeoutSignal signal : signals) {
            cancel(signal, stateMachine);
        }
    }

}
