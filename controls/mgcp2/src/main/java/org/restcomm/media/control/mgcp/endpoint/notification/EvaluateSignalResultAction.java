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

import java.util.Iterator;
import java.util.Set;

import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.signal.MgcpSignal;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class EvaluateSignalResultAction extends NotificationCenterAction {

    static final EvaluateSignalResultAction INSTANCE = new EvaluateSignalResultAction();

    EvaluateSignalResultAction() {
        super();
    }

    @Override
    public void execute(NotificationCenterState from, NotificationCenterState to, NotificationCenterEvent event, NotificationCenterTransitionContext context, NotificationCenterFsm stateMachine) {
        final NotificationCenterContext globalContext = stateMachine.getContext();
        final Set<TimeoutSignal> signals = globalContext.getTimeoutSignals();
        final MgcpSignal<?> signal = context.get(NotificationCenterTransitionParameter.SIGNAL, MgcpSignal.class);
        final MgcpEvent mgcpEvent = context.get(NotificationCenterTransitionParameter.SIGNAL_RESULT, MgcpEvent.class);

        // Remove signal from active TO list
        final boolean removed = signals.remove(signal);

        if (removed) {
            // Verify if endpoint is listening for such event
            final String mgcpEventName = mgcpEvent.getPackage() + "/" + mgcpEvent.getSymbol();
            final boolean eventRequested = globalContext.isEventRequested(mgcpEventName);

            if (eventRequested) {
                // Requested event was raised.
                // Cancel remaining Timeout signals
                final Iterator<TimeoutSignal> signalsIterator = signals.iterator();
                while (signalsIterator.hasNext()) {
                    TimeoutSignal timeoutSignal = signalsIterator.next();

                    // Unregister active signal
                    signalsIterator.remove();

                    // Cancel signal execution
                    final FutureCallback<MgcpEvent> callback = new TimeoutSignalCancellationCallback(timeoutSignal,
                            stateMachine);
                    timeoutSignal.cancel(callback);
                }
            } else {
                // If no requested event is raised, then allow remaining signals to continue execution.
            }
            
            // TODO Evaluate if there are more signals to stay active or to move to IDLE
        } else {
            // Ignore signals that are not listed as active
        }

    }

}
