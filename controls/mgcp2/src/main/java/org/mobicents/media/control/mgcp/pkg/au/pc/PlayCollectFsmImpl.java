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
import org.mobicents.media.control.mgcp.pkg.MgcpEventSubject;
import org.mobicents.media.control.mgcp.pkg.au.OperationComplete;
import org.mobicents.media.control.mgcp.pkg.au.OperationFailed;
import org.mobicents.media.control.mgcp.pkg.au.ReturnCode;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayCollectFsmImpl extends AbstractStateMachine<PlayCollectFsm, PlayCollectState, Object, PlayCollectContext>
        implements PlayCollectFsm {

    private static final Logger log = Logger.getLogger(PlayCollectFsmImpl.class);

    private final MgcpEventSubject mgcpEventSubject;

    public PlayCollectFsmImpl(MgcpEventSubject mgcpEventSubject) {
        super();
        this.mgcpEventSubject = mgcpEventSubject;
    }

    @Override
    public void enterCollecting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        final DtmfDetector dtmfDetector = context.getDetector();
        final DtmfDetectorListener dtmfDetectorListener = context.getDetectorListener();

        try {
            // Activate DTMF detector and bind listener
            dtmfDetector.addListener(dtmfDetectorListener);
            dtmfDetector.activate();
        } catch (TooManyListenersException e) {
            log.error("Too many DTMF listeners", e);
        }
    }

    @Override
    public void exitCollecting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        final DtmfDetector dtmfDetector = context.getDetector();
        final DtmfDetectorListener dtmfDetectorListener = context.getDetectorListener();

        // Deactivate DTMF detector and release listener
        dtmfDetector.removeListener(dtmfDetectorListener);
        dtmfDetector.deactivate();
    }

    @Override
    public void onCollecting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        final char tone = ((DtmfToneEvent) event).tone();
        final char endInputKey = context.getEndInputKey();

        if (log.isInfoEnabled()) {
            log.info("Received tone " + tone);
        }

        if (endInputKey == tone) {
            if (context.hasDigitPattern()) {
                // Check if list of collected digits match the digit pattern
                if (context.getCollectedDigits().matches(context.getDigitPattern())) {
                    // Append endInputKey (if required)
                    if (context.getIncludeEndInputKey()) {
                        context.collectDigit(tone);
                    }

                    // Fire success event
                    context.setReturnCode(ReturnCode.SUCCESS.code());
                    fire(SuccessEvent.INSTANCE, context);
                } else {
                    // TODO fix attempts

                    // Fire failure event
                    context.setReturnCode(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code());
                    fire(FailureEvent.INSTANCE, context);
                }
            } else {
                // Check if minimum number of digits was collected
                if (context.getMinimumDigits() <= context.countCollectedDigits()) {
                    // Minimum number of digits was collected
                    // Append endInputKey (if required)
                    if (context.getIncludeEndInputKey()) {
                        context.collectDigit(tone);
                    }

                    // Fire success event
                    context.setReturnCode(ReturnCode.SUCCESS.code());
                    fire(SuccessEvent.INSTANCE, context);
                } else {
                    // Minimum number of digits was NOT collected

                    // TODO fix attempts

                    // Fire failure event
                    context.setReturnCode(ReturnCode.MAX_ATTEMPTS_EXCEEDED.code());
                    fire(FailureEvent.INSTANCE, context);
                }
            }
        } else {
            // Make sure first digit matches StartInputKey
            if (context.countCollectedDigits() == 0 && context.getStartInputKeys().indexOf(tone) == -1) {
                log.info("Dropping tone " + tone + " because it does not match any of StartInputKeys "
                        + context.getStartInputKeys());
                return;
            }

            // Append tone to list of collected digits
            context.collectDigit(tone);

            // Verify if number of max digits was reached
            if (!context.hasDigitPattern() && context.getMaximumDigits() == context.countCollectedDigits()) {
                // Fire success event
                context.setReturnCode(ReturnCode.SUCCESS.code());
                fire(SuccessEvent.INSTANCE, context);
            } else {
                // TODO start interdigit timer
            }
        }
    }

    @Override
    public void enterCanceled(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        log.info("Canceled!!!");
        // TODO Auto-generated method stub
    }

    @Override
    public void enterSucceeded(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        final int returnCode = context.getReturnCode();
        final String collectedDigits = context.getCollectedDigits();
        final int attempt = context.getAttempt();

        final OperationComplete operationComplete = new OperationComplete(PlayCollect.SYMBOL, returnCode);
        operationComplete.setParameter("na", String.valueOf(attempt));
        operationComplete.setParameter("dc", collectedDigits);
        this.mgcpEventSubject.notify(this.mgcpEventSubject, operationComplete);
    }

    @Override
    public void enterFailed(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        final OperationFailed operationFailed = new OperationFailed(PlayCollect.SYMBOL, context.getReturnCode());
        this.mgcpEventSubject.notify(this.mgcpEventSubject, operationFailed);
    }

}