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
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

import java.util.Set;

/**
 * Quarantines the current set of timeout signals.
 *
 * @author Henrique Rosa (henrique.rosa@telestax.com) on 02/10/2017
 */
class QuarantineSignalsAction extends NotificationCenterAction {

    private static final Logger log = Logger.getLogger(QuarantineSignalsAction.class);

    static final QuarantineSignalsAction INSTANCE = new QuarantineSignalsAction();

    QuarantineSignalsAction() {
        super();
    }

    @Override
    public void execute(NotificationCenterState from, NotificationCenterState to, NotificationCenterEvent event, NotificationCenterTransitionContext context, NotificationCenterFsm stateMachine) {
        final NotificationCenterContext globalContext = stateMachine.getContext();

        SignalQuarantine quarantine = globalContext.getQuarantine();

        // Close outstanding quarantine
        if (quarantine != null) {
            if(log.isDebugEnabled()) {
                log.debug("Erased " +  quarantine.toString());
            }
            quarantine.close();
        }

        // Quarantine current signals
        final String requestId = globalContext.getRequestId();
        final Set<TimeoutSignal> timeoutSignals = globalContext.getTimeoutSignals();

        quarantine = new SignalQuarantine(requestId, timeoutSignals);
        globalContext.setQuarantine(quarantine);

        if(log.isDebugEnabled()) {
            log.debug(quarantine.toString());
        }
    }
}
