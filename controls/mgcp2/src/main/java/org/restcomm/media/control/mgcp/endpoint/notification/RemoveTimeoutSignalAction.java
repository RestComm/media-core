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

import org.restcomm.media.control.mgcp.signal.MgcpSignal;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

/**
 * Unregisters a signal from the endpoint.
 * 
 * <p>
 * Raises an {@link NotificationCenterEvent#ALL_SIGNALS_COMPLETED} event if no more signals are active on the endpoint.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class RemoveTimeoutSignalAction extends NotificationCenterAction {

    static final RemoveTimeoutSignalAction INSTANCE = new RemoveTimeoutSignalAction();

    RemoveTimeoutSignalAction() {
        super();
    }

    @Override
    public void execute(NotificationCenterState from, NotificationCenterState to, NotificationCenterEvent event, NotificationCenterTransitionContext context, NotificationCenterFsm stateMachine) {
        final NotificationCenterContext globalContext = stateMachine.getContext();
        final MgcpSignal<?> signal = context.get(NotificationCenterTransitionParameter.SIGNAL, MgcpSignal.class);

        // Remove signal from list of active signals
        final Set<TimeoutSignal> activeSignals = globalContext.getTimeoutSignals();
        final boolean removed = activeSignals.remove(signal);

        // Move to next state when all signals are completed
        final boolean hasActiveBriefSignal = (globalContext.getActiveBriefSignal() != null);
        if (removed && activeSignals.isEmpty() && !hasActiveBriefSignal) {
            stateMachine.fire(NotificationCenterEvent.ALL_SIGNALS_COMPLETED, context);
        }
    }

}
