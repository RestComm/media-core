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
 * Executes requested signals.
 * 
 * Input parameters:
 * <ul>
 * <li>PENDING_TIMEOUT_SIGNALS</li>
 * </ul>
 * </p>
 * <p>
 * Output parameters:
 * <ul>
 * <li>n/a</li>
 * </ul>
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class ExecutePendingSignalsAction extends NotificationCenterAction {

    private static final Logger log = Logger.getLogger(ExecutePendingSignalsAction.class);

    static final ExecutePendingSignalsAction INSTANCE = new ExecutePendingSignalsAction();

    ExecutePendingSignalsAction() {
        super();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(NotificationCenterState from, NotificationCenterState to, NotificationCenterEvent event, NotificationCenterTransitionContext context, NotificationCenterFsm stateMachine) {
        final NotificationCenterContext globalContext = stateMachine.getContext();

        // Execute all pending timeout signals in parallel
        final Set<TimeoutSignal> pendingTimeoutSignals = context.get(NotificationCenterTransitionParameter.PENDING_TIMEOUT_SIGNALS, Set.class);

        for (TimeoutSignal signal : pendingTimeoutSignals) {
            final TimeoutSignalExecutionCallback callback = new TimeoutSignalExecutionCallback(signal, stateMachine);
            signal.execute(callback);

            if (log.isDebugEnabled()) {
                log.debug("Endpoint " + globalContext.getEndpointId() + " started executing signal " + signal.toString());
            }
        }

        // Execute brief signals orderly
        final BriefSignal activeBriefSignal = globalContext.getActiveBriefSignal();

        if (activeBriefSignal == null) {
            final Queue<BriefSignal> pendingBriefSignals = globalContext.getPendingBriefSignals();
            if (!pendingBriefSignals.isEmpty()) {
                final BriefSignal signal = pendingBriefSignals.poll();
                final BriefSignalExecutionCallback callback = new BriefSignalExecutionCallback(signal, stateMachine);

                globalContext.setActiveBriefSignal(signal);
                signal.execute(callback);

                if (log.isDebugEnabled()) {
                    log.debug("Endpoint " + globalContext.getEndpointId() + " started executing signal " + signal.toString());
                }
            }
        }
    }

}
