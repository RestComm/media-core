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

import java.util.ArrayList;
import java.util.List;

import org.restcomm.media.control.mgcp.signal.MgcpSignal;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

/**
 * Filters all active TO signals that were not requested in a new RQNT.
 * 
 * Input parameters:
 * <ul>
 * <li>REQUESTED_SIGNALS</li>
 * </ul>
 * </p>
 * <p>
 * Output parameters:
 * <ul>
 * <li>UNREQUESTED_SIGNALS
 * <li>
 * </ul>
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class FilterUnrequestedTimeoutSignalsAction extends NotificationCenterAction {

    static final FilterUnrequestedTimeoutSignalsAction INSTANCE = new FilterUnrequestedTimeoutSignalsAction();

    FilterUnrequestedTimeoutSignalsAction() {
        super();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(NotificationCenterState from, NotificationCenterState to, NotificationCenterEvent event, NotificationCenterTransitionContext context, NotificationCenterFsm stateMachine) {
        final NotificationCenterContext globalContext = stateMachine.getContext();
        final List<TimeoutSignal> timeoutSignals = globalContext.getTimeoutSignals();
        final List<MgcpSignal<?>> requestedSignals = context.get(NotificationCenterTransitionParameter.REQUESTED_SIGNALS, List.class);

        // Cancel all ongoing TO signals that are not listed in new request
        final List<TimeoutSignal> unrequestedSignals;

        if (requestedSignals.isEmpty()) {
            // No active TO signal was requested
            unrequestedSignals = new ArrayList<>(timeoutSignals);
        } else {
            // Filter TO signals
            unrequestedSignals = filterUnrequested(requestedSignals, timeoutSignals);
        }
        context.set(NotificationCenterTransitionParameter.UNREQUESTED_SIGNALS, unrequestedSignals);
    }

    private List<TimeoutSignal> filterUnrequested(List<MgcpSignal<?>> requestedSignals, List<TimeoutSignal> currentSignals) {
        // Filter unlisted TO signals only
        final ArrayList<TimeoutSignal> unrequestedSignals = new ArrayList<>(currentSignals.size());

        for (TimeoutSignal currentSignal : currentSignals) {
            boolean requested = false;
            for (int i = 0; i < requestedSignals.size(); i++) {
                final MgcpSignal<?> requestedSignal = requestedSignals.get(i);
                requested = requestedSignal.equals(currentSignal);
                if (requested) {
                    // IMPORTANT !! Replace requested event with ongoing one
                    requestedSignals.add(i, currentSignal);
                    break;
                }
            }
            if (!requested) {
                unrequestedSignals.add(currentSignal);
            }
        }
        return unrequestedSignals;
    }

}
