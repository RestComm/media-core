/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.mobicents.media.control.mgcp.pkg.au.pc;

import org.apache.log4j.Logger;
import org.squirrelframework.foundation.fsm.AnonymousAction;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class EvaluateInputAction extends AnonymousAction<PlayCollectFsm, PlayCollectState, Object, PlayCollectContext> {

    private static final Logger log = Logger.getLogger(EvaluateInputAction.class);

    static final EvaluateInputAction INSTANCE = new EvaluateInputAction();

    private EvaluateInputAction() {
        super();
    }

    @Override
    public void execute(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context, PlayCollectFsm stateMachine) {
        if (log.isTraceEnabled()) {
            log.trace("Evaluating collected digits: " + context.getCollectedDigits() + ", attempt: " + context.getAttempt());
        }

        if (context.countCollectedDigits() < context.getMinimumDigits()) {
            // Minimum digits not collected
            stateMachine.fire(PlayCollectEvent.FAIL, context);
        } else {
            stateMachine.fire(PlayCollectEvent.SUCCEED, context);
        }

    }

}
