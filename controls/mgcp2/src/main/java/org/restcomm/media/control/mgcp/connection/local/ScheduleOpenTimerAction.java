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

package org.restcomm.media.control.mgcp.connection.local;

import org.apache.log4j.Logger;
import org.squirrelframework.foundation.fsm.AnonymousAction;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ScheduleOpenTimerAction extends
        AnonymousAction<MgcpLocalConnectionFsm, MgcpLocalConnectionState, MgcpLocalConnectionEvent, MgcpLocalConnectionTransitionContext> {

    private static final Logger log = Logger.getLogger(ScheduleOpenTimerAction.class);

    @Override
    public void execute(MgcpLocalConnectionState from, MgcpLocalConnectionState to, MgcpLocalConnectionEvent event, MgcpLocalConnectionTransitionContext context, MgcpLocalConnectionFsm stateMachine) {
        final MgcpLocalConnectionContext globalContext = stateMachine.getContext();
        final ListeningScheduledExecutorService executor = context.get(MgcpLocalConnectionParameter.SCHEDULER, ListeningScheduledExecutorService.class);
        int timeout = globalContext.getTimeout();

        if(timeout > 0) {
//            executor.sc
        }

        if (log.isDebugEnabled()) {
            final String identifier = globalContext.getHexIdentifier();

            log.debug("Local MGCP connection " + identifier + " ");

        }

    }

}
