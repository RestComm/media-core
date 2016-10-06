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
import org.mobicents.media.control.mgcp.pkg.au.ReturnCode;
import org.squirrelframework.foundation.fsm.AnonymousAction;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class EvaluateDtmf extends AnonymousAction<PlayCollectFsm, PlayCollectState, Object, PlayCollectContext> {
    
    private static final Logger log = Logger.getLogger(EvaluateDtmf.class);
    
    public static EvaluateDtmf INSTANCE = new EvaluateDtmf();
    
    private EvaluateDtmf() {
        super();
    }

    @Override
    public void execute(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context, PlayCollectFsm stateMachine) {
        if (log.isDebugEnabled()) {
            log.debug("Evaluating collected digits");
        }

        if (context.hasDigitPattern()) {
            // Check if list of collected digits match the digit pattern
            if (context.getCollectedDigits().matches(context.getDigitPattern())) {
                // Digit Collection succeeded
                if (context.hasSuccessAnnouncement()) {
                    stateMachine.fire(PlayCollectEvent.PLAY_SUCCESS, context);
                } else {
                    stateMachine.fire(PlayCollectEvent.SUCCEED, context);
                }
            } else {
                // Retry if more attempts are available. If not, fail.
                if (context.hasMoreAttempts()) {
                    // Clear digits and play reprompt
                    if (context.countCollectedDigits() == 0) {
                        stateMachine.fire(PlayCollectEvent.NO_DIGITS_REPROMPT, context);
                    } else {
                        stateMachine.fire(PlayCollectEvent.REPROMPT, context);
                    }
                } else {
                    // Fire failure event
                    context.setReturnCode(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code());
                    if (context.hasFailureAnnouncement()) {
                        stateMachine.fire(PlayCollectEvent.PLAY_FAILURE, context);
                    } else {
                        stateMachine.fire(PlayCollectEvent.FAIL, context);
                    }
                }
            }
        } else {
            // Check if minimum number of digits was collected
            if (context.getMinimumDigits() <= context.countCollectedDigits()) {
                // Minimum number of digits was collected
                // Digit Collection succeeded
                if (context.hasSuccessAnnouncement()) {
                    stateMachine.fire(PlayCollectEvent.PLAY_SUCCESS, context);
                } else {
                    stateMachine.fire(PlayCollectEvent.SUCCEED, context);
                }
            } else {
                // Minimum number of digits was NOT collected
                // Retry if more attempts are available. If not, fail.
                if (context.hasMoreAttempts()) {
                    // Clear digits and play reprompt
                    if (context.countCollectedDigits() == 0) {
                        stateMachine.fire(PlayCollectEvent.NO_DIGITS_REPROMPT, context);
                    } else {
                        stateMachine.fire(PlayCollectEvent.REPROMPT, context);
                    }
                } else {
                    // Fire failure event
                    context.setReturnCode(ReturnCode.MAX_ATTEMPTS_EXCEEDED.code());
                    if (context.hasFailureAnnouncement()) {
                        stateMachine.fire(PlayCollectEvent.PLAY_FAILURE, context);
                    } else {
                        stateMachine.fire(PlayCollectEvent.FAIL, context);
                    }
                }
            }
        }
    }

}
