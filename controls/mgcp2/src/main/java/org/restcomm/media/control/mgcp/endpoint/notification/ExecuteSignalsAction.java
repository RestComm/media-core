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
 * <li>n/a</li>
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
class ExecuteSignalsAction extends NotificationCenterAction {
    
    private static final Logger log = Logger.getLogger(ExecuteSignalsAction.class);

    static final ExecuteSignalsAction INSTANCE = new ExecuteSignalsAction();

    ExecuteSignalsAction() {
        super();
    }

    @Override
    public void execute(NotificationCenterState from, NotificationCenterState to, NotificationCenterEvent event, NotificationCenterTransitionContext context, NotificationCenterFsm stateMachine) {
        final NotificationCenterContext globalContext = stateMachine.getContext();
        final Queue<BriefSignal> briefSignals = globalContext.getBriefSignals();
        final Set<TimeoutSignal> timeoutSignals = globalContext.getTimeoutSignals();

        // Execute all timeout signals in parallel
        for (TimeoutSignal signal : timeoutSignals) {
            final TimeoutSignalExecutionCallback callback = new TimeoutSignalExecutionCallback(signal, stateMachine);
            signal.execute(callback);
            
            if(log.isDebugEnabled()) {
                log.debug("Endpoint " + globalContext.getEndpoint().getEndpointId() + " started executing signal " +  signal.toString());
            }
        }

        // Execute brief signals orderly
        if (!briefSignals.isEmpty()) {
            final BriefSignal signal = briefSignals.poll();
            final BriefSignalExecutionCallback callback = new BriefSignalExecutionCallback(signal, stateMachine);
            signal.execute(callback);
            
            if(log.isDebugEnabled()) {
                log.debug("Endpoint " + globalContext.getEndpoint().getEndpointId() + " started executing signal " +  signal.toString());
            }
        }
    }

}
