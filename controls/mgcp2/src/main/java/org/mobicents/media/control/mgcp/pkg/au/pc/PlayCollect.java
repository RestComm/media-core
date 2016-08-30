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

import java.util.Map;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.pkg.AbstractMgcpSignal;
import org.mobicents.media.control.mgcp.pkg.SignalType;
import org.mobicents.media.control.mgcp.pkg.au.AudioPackage;
import org.mobicents.media.control.mgcp.pkg.au.OperationComplete;
import org.mobicents.media.control.mgcp.pkg.au.OperationFailed;
import org.mobicents.media.control.mgcp.pkg.au.ReturnCode;
import org.mobicents.media.control.mgcp.pkg.au.SignalParameters;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.dtmf.DtmfEvent;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.spi.player.Player;
import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * Plays a prompt and collects DTMF digits entered by a user.
 * 
 * <p>
 * If no digits are entered or an invalid digit pattern is entered, the user may be reprompted and given another chance to enter
 * a correct pattern of digits. The following digits are supported: 0-9, *, #, A, B, C, D.
 * </p>
 * 
 * <p>
 * By default PlayCollect does not play an initial prompt, makes only one attempt to collect digits, and therefore functions as
 * a simple Collect operation.<br>
 * Various special purpose keys, key sequences, and key sets can be defined for use during the PlayCollect operation.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayCollect extends AbstractMgcpSignal {

    private static final Logger log = Logger.getLogger(PlayCollect.class);

    // Finite State Machine
    private PlayCollectFSM fsm;

    // Media Components
    private final DtmfDetector detector;
    final DtmfDetectorListener detectorListener;

    // Configuration Parameters
    private final char endInputKey;

    // Execution Context
    private final StringBuilder collectedDigits;
    private long lastDigitCollectedOn;

    public PlayCollect(Player player, DtmfDetector detector, Map<String, String> parameters,
            ListeningScheduledExecutorService executor) {
        super(AudioPackage.PACKAGE_NAME, "pc", SignalType.TIME_OUT, parameters);

        // Finite State Machine
        StateMachineBuilder<PlayCollectFSM, PlayCollectState, PlayCollectEvent, PlayCollectContext> builder = StateMachineBuilderFactory
                .<PlayCollectFSM, PlayCollectState, PlayCollectEvent, PlayCollectContext> create(StateMachine.class,
                        PlayCollectState.class, PlayCollectEvent.class, PlayCollectContext.class);

        // this.fsm = builder.newStateMachine(PlayCollectState.READY);

        // Media Components
        this.detector = detector;
        this.detectorListener = new DetectorListener();

        // Configuration Parameters
        this.endInputKey = '#';

        // Execution Context
        this.collectedDigits = new StringBuilder("");
        this.lastDigitCollectedOn = 0L;
    }

    @Override
    protected boolean isParameterSupported(String name) {
        // Check if parameter is valid
        SignalParameters parameter = SignalParameters.fromSymbol(name);
        if (parameter == null) {
            return false;
        }

        // Check if parameter is supported
        switch (parameter) {
            case INITIAL_PROMPT:
            case REPROMPT:
            case NO_DIGITS_REPROMPT:
            case FAILURE_ANNOUNCEMENT:
            case SUCCESS_ANNOUNCEMENT:
            case NON_INTERRUPTIBLE_PLAY:
            case SPEED:
            case VOLUME:
            case CLEAR_DIGIT_BUFFER:
            case MAXIMUM_NUM_DIGITS:
            case MINIMUM_NUM_DIGITS:
            case DIGIT_PATTERN:
            case FIRST_DIGIT_TIMER:
            case INTER_DIGIT_TIMER:
            case EXTRA_DIGIT_TIMER:
            case RESTART_KEY:
            case REINPUT_KEY:
            case RETURN_KEY:
            case POSITION_KEY:
            case STOP_KEY:
            case START_INPUT_KEY:
            case END_INPUT_KEY:
            case INCLUDE_END_INPUT_KEY:
            case NUMBER_OF_ATTEMPTS:
                return true;

            default:
                return false;
        }
    }

    @Override
    public void execute() {
        final PlayCollectState currentState = this.fsm.getCurrentState();
        switch (currentState) {
            case READY:
                fsm.fire(PlayCollectEvent.EXECUTE, new EmptyContext());
                break;

            default:
                throw new IllegalStateException("Signal is already being executed.");
        }
    }

    @Override
    public void cancel() {
        final PlayCollectState currentState = this.fsm.getCurrentState();
        switch (currentState) {
            case READY:
            case COLLECTING:
                fsm.fire(PlayCollectEvent.CANCEL, new EmptyContext());
                break;

            default:
                throw new IllegalStateException("Signal has already terminated.");
        }
    }

    private final class DetectorListener implements DtmfDetectorListener {

        @Override
        public void process(DtmfEvent event) {
            fsm.fire(PlayCollectEvent.DTMF_TONE, new DtmfToneContext(event.getTone().charAt(0)));
        }

    }

    private class StateMachine
            extends AbstractStateMachine<PlayCollectFSM, PlayCollectState, PlayCollectEvent, PlayCollectContext>
            implements PlayCollectFSM {
        
        public StateMachine() {
            super();
        }

        @Override
        public void enterCollecting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
                PlayCollectContext context) {
            try {
                detector.addListener(detectorListener);
                detector.activate();
            } catch (TooManyListenersException e) {
                // TODO fail signal
            }
        }

        @Override
        public void exitCollecting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
                PlayCollectContext context) {
//            detector.removeListener(detectorListener);
//            detector.deactivate();
        }

        @Override
        public void onCollecting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
                PlayCollectContext context) {
            final DtmfToneContext toneContext = (DtmfToneContext) context;
            final char tone = toneContext.getTone();

//            if (endInputKey == tone) {
//                // TODO fix attempts
//                SuccessContext successContext = new SuccessContext(collectedDigits.toString(), 1, ReturnCode.SUCCESS.code());
//                fsm.fire(PlayCollectEvent.SUCCEED, successContext);
//            } else {
//                lastDigitCollectedOn = System.currentTimeMillis();
//                collectedDigits.append(tone);
//            }
        }

        @Override
        public void enterCanceled(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
                PlayCollectContext context) {
            log.info("Canceled!!!");
            // TODO Auto-generated method stub
        }

        @Override
        public void enterSucceeded(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
                PlayCollectContext context) {
//            final SuccessContext successContext = (SuccessContext) context;
//            final OperationComplete operationComplete = new OperationComplete(getSymbol(), successContext.getReturnCode());
//            operationComplete.setParameter("na", String.valueOf(successContext.getAttempts()));
//            operationComplete.setParameter("dc", successContext.getCollectedDigits());
//            PlayCollect.this.notify(PlayCollect.this, operationComplete);
        }

        @Override
        public void enterFailed(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
                PlayCollectContext context) {
//            final FailureContext failureContext = (FailureContext) context;
//            final OperationFailed operationFailed = new OperationFailed(getSymbol(), failureContext.getReturnCode());
//            PlayCollect.this.notify(this, operationFailed);
        }

    }

}
