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

import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

/**
 * Cancels all active signals that were not requested in a new RQNT.
 * 
 * Input parameters:
 * <ul>
 * <li>UNREQUESTED_SIGNALS</li>
 * </ul>
 * </p>
 * <p>
 * Output parameters:
 * <ul>
 * <li>UNREQUESTED_SIGNALS<li>
 * </ul>
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class CancelUnrequestedSignalsAction extends NotificationCenterAction {

    static final CancelUnrequestedSignalsAction INSTANCE = new CancelUnrequestedSignalsAction();

    CancelUnrequestedSignalsAction() {
        super();
    }

    @Override
    public void execute(NotificationCenterState from, NotificationCenterState to, NotificationCenterEvent event, NotificationCenterTransitionContext context, NotificationCenterFsm stateMachine) {
        final NotificationCenterContext globalContext = stateMachine.getContext();
        final TimeoutSignal[] unrequestedSignals = context.get(NotificationCenterTransitionParameter.UNREQUESTED_SIGNALS, TimeoutSignal[].class);

        // Cancel all pending BR signals
        globalContext.getPendingBriefSignals().clear();

        // Cancel all ongoing TO signals that are not listed in new request
        cancelAll(unrequestedSignals, stateMachine);
    }

    private void cancel(TimeoutSignal signal, NotificationCenterFsm stateMachine) {
        final TimeoutSignalCancellationCallback callback = new TimeoutSignalCancellationCallback(signal, stateMachine);
        signal.cancel(callback);
    }

    private void cancelAll(TimeoutSignal[] signals, NotificationCenterFsm stateMachine) {
        for (TimeoutSignal signal : signals) {
            cancel(signal, stateMachine);
        }
    }

}
