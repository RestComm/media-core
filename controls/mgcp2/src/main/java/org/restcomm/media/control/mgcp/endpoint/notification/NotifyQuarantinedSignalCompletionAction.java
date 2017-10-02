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
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

/**
 * Notifies the quarantine about the completion of a signal and its result.
 *
 * @author Henrique Rosa (henrique.rosa@telestax.com) on 02/10/2017
 */
class NotifyQuarantinedSignalCompletionAction extends NotificationCenterAction {

    private static final Logger log = Logger.getLogger(NotifyQuarantinedSignalCompletionAction.class);

    static final NotifyQuarantinedSignalCompletionAction INSTANCE = new NotifyQuarantinedSignalCompletionAction();

    private NotifyQuarantinedSignalCompletionAction() {
        super();
    }

    @Override
    public void execute(NotificationCenterState from, NotificationCenterState to, NotificationCenterEvent event, NotificationCenterTransitionContext context, NotificationCenterFsm stateMachine) {
        final NotificationCenterContext globalContext = stateMachine.getContext();
        final TimeoutSignal signal = context.get(NotificationCenterTransitionParameter.SIGNAL, TimeoutSignal.class);
        final MgcpEvent result = context.get(NotificationCenterTransitionParameter.SIGNAL_RESULT, MgcpEvent.class);

        // Check if signal belongs in quarantine
        final SignalQuarantine quarantine = globalContext.getQuarantine();

        if (quarantine.contains(signal)) {
            // Notify quarantine about signal completion
            quarantine.onSignalCompleted(signal, result);
            if (log.isDebugEnabled()) {
                log.debug("Quarantined signal " + signal + " from RQNT X:" + signal.getRequestId() + " completed with result " + event);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Signal " + signal + " from RQNT X:" + signal.getRequestId() + " is no longer registered. Notification dropped.");
            }
        }
    }
}
