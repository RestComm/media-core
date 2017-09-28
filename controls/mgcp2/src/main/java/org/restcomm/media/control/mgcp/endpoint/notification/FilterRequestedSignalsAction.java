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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.restcomm.media.control.mgcp.signal.BriefSignal;
import org.restcomm.media.control.mgcp.signal.MgcpSignal;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

/**
 * Filters requested signals into relevant collections that ease work down the action pipeline.
 * 
 * Input parameters:
 * <ul>
 * <li>REQUESTED_SIGNALS</li>
 * </ul>
 * </p>
 * <p>
 * Output parameters:
 * <ul>
 * <li>REQUESTED_BRIEF_SIGNALS</li>
 * <li>REQUESTED_TIMEOUT_SIGNALS</li>
 * <li>UNREQUESTED_TIMEOUT_SIGNALS</li>
 * <li>PENDING_TIMEOUT_SIGNALS</li>
 * </ul>
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class FilterRequestedSignalsAction extends NotificationCenterAction {

    static final FilterRequestedSignalsAction INSTANCE = new FilterRequestedSignalsAction();

    FilterRequestedSignalsAction() {
        super();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(NotificationCenterState from, NotificationCenterState to, NotificationCenterEvent event, NotificationCenterTransitionContext context, NotificationCenterFsm stateMachine) {
        final NotificationCenterContext globalContext = stateMachine.getContext();

        // Distinguish between Brief and Timeout events
        final List<MgcpSignal<?>> requestedSignals = context.get(NotificationCenterTransitionParameter.REQUESTED_SIGNALS, List.class);
        final Set<TimeoutSignal> requestedTimeoutSignals = new HashSet<>(requestedSignals.size());
        final List<BriefSignal> requestedBriefSignals = new ArrayList<>(requestedSignals.size());

        for (MgcpSignal<?> signal : requestedSignals) {
            if (signal instanceof BriefSignal) {
                requestedBriefSignals.add((BriefSignal) signal);
            } else if (signal instanceof TimeoutSignal) {
                requestedTimeoutSignals.add((TimeoutSignal) signal);
            }
        }

        // Replace requested timeout signals with instances of active ones
        // According to MGCP specification, the existing signal must continue its execution (instead of restarting the
        // operation)
        final Set<TimeoutSignal> activeTimeoutSignals = globalContext.getTimeoutSignals();
        for (TimeoutSignal timeoutSignal : activeTimeoutSignals) {
            final Iterator<TimeoutSignal> iterator = requestedTimeoutSignals.iterator();
            boolean requested = false;

            while (iterator.hasNext()) {
                final TimeoutSignal requestedTimeoutSignal = iterator.next();
                requested = timeoutSignal.equals(requestedTimeoutSignal);
                if (requested) {
                    iterator.remove();
                    break;
                }
            }

            if (requested) {
                requestedTimeoutSignals.add(timeoutSignal);
            }
        }

        // Collect active timeout signals that were not requested (and must be cancelled)
        SetView<TimeoutSignal> unrequestedTimeoutSignals = Sets.difference(activeTimeoutSignals, requestedTimeoutSignals);

        // Collect requested signals that are pending (not yet executing on endpoint)
        SetView<TimeoutSignal> pendingTimeoutSignals = Sets.difference(requestedTimeoutSignals, activeTimeoutSignals);

        // Output parameters to transition context
        context.set(NotificationCenterTransitionParameter.REQUESTED_BRIEF_SIGNALS, requestedBriefSignals);
        context.set(NotificationCenterTransitionParameter.REQUESTED_TIMEOUT_SIGNALS, requestedTimeoutSignals);
        context.set(NotificationCenterTransitionParameter.UNREQUESTED_TIMEOUT_SIGNALS, unrequestedTimeoutSignals);
        context.set(NotificationCenterTransitionParameter.PENDING_TIMEOUT_SIGNALS, pendingTimeoutSignals);
    }

}
