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
public class CancelAction extends AnonymousAction<PlayCollectFsm, PlayCollectState, Object, PlayCollectContext> {

    private static final Logger log = Logger.getLogger(CancelAction.class);

    static final CancelAction INSTANCE = new CancelAction();

    private CancelAction() {
        super();
    }

    @Override
    public void execute(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context, PlayCollectFsm stateMachine) {
        if (log.isDebugEnabled()) {
            log.debug("Evaluating collected digits upon event cancelation.");
        }

        if (context.hasDigitPattern()) {
            // Check if list of collected digits match the digit pattern
            if (context.getCollectedDigits().matches(context.getDigitPattern())) {
                // Digit Collection succeeded
                stateMachine.fire(PlayCollectEvent.SUCCEED, context);
            } else {
                // Digit Pattern did not match collected digits. Fail.
                context.setReturnCode(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code());
                stateMachine.fire(PlayCollectEvent.FAIL, context);
            }
        } else {
            final int digitCount = context.countCollectedDigits();
            
            // Check if minimum number of digits was collected
            if (context.getMinimumDigits() <= digitCount) {
                // Minimum number of digits was collected
                // Digit Collection succeeded
                stateMachine.fire(PlayCollectEvent.SUCCEED, context);
            } else {
                // Minimum number of digits was NOT collected. Fail.
                if(digitCount == 0) {
                    context.setReturnCode(ReturnCode.NO_DIGITS.code());
                } else {
                    context.setReturnCode(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code());
                }
                stateMachine.fire(PlayCollectEvent.FAIL, context);
            }
        }

    }

}
