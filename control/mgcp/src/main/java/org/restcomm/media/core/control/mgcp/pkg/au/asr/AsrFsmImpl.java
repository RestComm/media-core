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

package org.restcomm.media.core.control.mgcp.pkg.au.asr;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.core.asr.AsrEngine;
import org.restcomm.media.core.asr.AsrEngineListener;
import org.restcomm.media.core.asr.InputTimeoutDetector;
import org.restcomm.media.core.asr.InputTimeoutListener;
import org.restcomm.media.core.control.mgcp.pkg.MgcpEventSubject;
import org.restcomm.media.core.control.mgcp.pkg.au.*;
import org.restcomm.media.core.drivers.asr.AsrDriverConfigurationException;
import org.restcomm.media.core.drivers.asr.AsrDriverException;
import org.restcomm.media.core.drivers.asr.UnknownAsrDriverException;
import org.restcomm.media.core.resource.dtmf.DtmfEvent;
import org.restcomm.media.core.resource.dtmf.DtmfEventObserver;
import org.restcomm.media.core.resource.dtmf.DtmfEventSubject;
import org.restcomm.media.core.resource.dtmf.DtmfSinkFacade;
import org.restcomm.media.core.resource.vad.VoiceActivityDetectorListener;
import org.restcomm.media.core.spi.ResourceUnavailableException;
import org.restcomm.media.core.spi.listener.TooManyListenersException;
import org.restcomm.media.core.spi.player.Player;
import org.restcomm.media.core.spi.player.PlayerEvent;
import org.restcomm.media.core.spi.player.PlayerListener;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author anikiforov
 */
public class AsrFsmImpl extends AbstractStateMachine<AsrFsm, AsrState, AsrEvent, AsrContext> implements AsrFsm {

    private static final Logger log = LogManager.getLogger(AsrFsmImpl.class);

    // Media Components
    private final DtmfEventSubject detector;
    private final DtmfEventObserver detectorObserver;

    private final Player player;
    private final PlayerListener playerListener;

    // Execution Context
    private final AsrContext context;

    private final AsrEngine asrEngine;

    private final InputTimeoutDetector inputTimeoutDetector;

    // Nullable - use supportAsr to check
    private final AsrEngineListener asrEngineListener;

    public InputTimeoutListener getInputTimeoutDetectorListener() { // Method to use only in unit tests
        return inputTimeoutDetectorListener;
    }

    private final InputTimeoutListener inputTimeoutDetectorListener;
    private final MgcpEventSubject mgcpEventSubject;
    private final ListeningScheduledExecutorService executor;

    public AsrFsmImpl(DtmfEventSubject detector, Player player, AsrEngine asrEngine, MgcpEventSubject mgcpEventSubject,
            ListeningScheduledExecutorService executor, AsrContext context) {
        super();

        this.asrEngine = asrEngine;

        // Media Components
        this.detector = detector;
        this.detectorObserver = new DetectorObserver();

        this.player = player;
        this.playerListener = new AudioPlayerListener();

        // Execution Context
        this.context = context;

        this.mgcpEventSubject = mgcpEventSubject;
        this.executor = executor;

        this.asrEngineListener = new LocalAsrEngineListener();
        this.inputTimeoutDetectorListener = new LocalInputTimeoutListener();
        inputTimeoutDetector = new InputTimeoutDetector(executor);
    }

    private void playAnnouncement(String url, long delay) {
        try {
            this.player.setInitialDelay(delay);
            this.player.setURL(url);
            this.player.activate();
        } catch (MalformedURLException e) {
            log.warn("Could not play malformed segment " + url);
            context.setReturnCode(ReturnCode.BAD_AUDIO_ID.code());
            fire(AsrEvent.FAIL, context);
        } catch (ResourceUnavailableException e) {
            log.warn("Could not play unavailable segment " + url);
            context.setReturnCode(ReturnCode.BAD_AUDIO_ID.code());
            fire(AsrEvent.FAIL, context);
        }
    }

    @Override
    public void enterPlayCollect(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered PLAY_COLLECT state");
        }
        List<String> hotWords = context.getParams().getHotWords();
        if (hotWords == null) {
            return;
        }
        if (hotWords.size() > 50) {
            log.warn("HotWords limit exceeded. There are more than 50 phrases");
            context.setReturnCode(ReturnCode.VARIABLE_VALUE_OUT_OF_RANGE.code());
            fire(AsrEvent.DRIVER_ERROR, context);
            return;
        }
        for (String hint : hotWords) {
            if (hint.length() > 100) {
                log.warn("HotWords limit exceeded. Hint with more than 100 characters found");
                context.setReturnCode(ReturnCode.VARIABLE_VALUE_OUT_OF_RANGE.code());
                fire(AsrEvent.DRIVER_ERROR, context);
                return;
            }
        }
    }

    @Override
    public void exitPlayCollect(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PLAY_COLLECT state");
        }
        deactivateAsrEngine();
    }

    @Override
    public void enterLoadingPlaylist(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered LOADING PLAYLIST state");
        }

        if (event == null) {
            final Playlist prompt = context.getInitialPrompt();
            if (prompt.isEmpty()) {
                fire(AsrEvent.NO_PROMPT, context);
            } else {
                fire(AsrEvent.PROMPT, context);
            }
        } else {
            fire(AsrEvent.NO_PROMPT, context);
        }
    }

    @Override
    public void exitLoadingPlaylist(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited LOADING PLAYLIST state");
        }
    }

    @Override
    public void enterPrompting(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered PROMPTING state");
        }

        final Playlist prompt = context.getInitialPrompt();
        final String track = prompt.next();
        try {
            this.player.addListener(this.playerListener);
            playAnnouncement(track, 0L);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(AsrEvent.FAIL, context);
        }
    }

    @Override
    public void onPrompting(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On PROMPTING state");
        }

        final Playlist prompt = context.getInitialPrompt();
        final String track = prompt.next();

        if (track.isEmpty()) {
            // No more announcements to play
            fire(AsrEvent.END_PROMPT, context);
        } else {
            playAnnouncement(track, 10 * 100);
        }
    }

    @Override
    public void exitPrompting(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PROMPTING state");
        }

        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }

    @Override
    public void enterPrompted(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        inputTimeoutDetector.startWaitingForFirstInput();
    }

    @Override
    public void enterCollecting(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered COLLECTING state");
        }

        if (context.isEndInputKeySpecified()) {
            // Activate DTMF detector and bind observer
            ((DtmfSinkFacade) this.detector).activate();
            this.detector.observe(this.detectorObserver);
        }

        AsrContext.Parameters params = context.getParams();
        try {
            this.asrEngine.configure(params.getDriver(), params.getLang(), params.getHotWords());

            // We are turning speech detection on
            this.asrEngine.startSpeechDetection(new LocalSpeechDetectorListener());

            inputTimeoutDetector.startSession(inputTimeoutDetectorListener, params.getWaitingTimeForInput(),
                    params.getMaximumRecognitionTime(), params.getPostSpeechTimer());
            this.asrEngine.setListener(asrEngineListener);
            this.asrEngine.activate();
        } catch (final UnknownAsrDriverException | AsrDriverConfigurationException e) {
            log.warn(e.getMessage());
            context.setReturnCode(ReturnCode.PROVISIONING_ERROR.code());
            fire(AsrEvent.DRIVER_ERROR, context);
        }
    }

    @Override
    public void onTextRecognized(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (context.needPartialResult()) {
            final OperationComplete operationComplete = new OperationComplete(AsrSignal.SYMBOL, ReturnCode.PARTIAL_SUCCESS.code());
            operationComplete.setParameter(ReturnParameters.ASR_RESULT.symbol(), new String(Hex.encodeHex(context.getLastRecognizedText().getBytes())));
            mgcpEventSubject.notify(mgcpEventSubject, operationComplete);
        }
    }

    @Override
    public void onCollecting(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On COLLECTING state [digits=" + context.getCollectedDigits() + "]");
        }

        if (context.isMixedInputSupported()) {
            fire(AsrEvent.END_PROMPT, context);
        }

        final char tone = context.getLastTone();

        if (context.getParams().getEndInputKey() == tone) {
            fireEndInput();
        } else if (context.isMixedInputSupported()) {
            // Append tone to list of collected digits
            context.collectDigit(tone);

            // Stop collecting if maximum number of digits was reached.
            // Only verified if no Digit Pattern was defined.
            if (!context.hasDigitPattern() && context.countCollectedDigits() == context.getMaximumNumDigits()) {
                fireEndInput();
            } else {
                final int interDigitTimerInMilliseconds = context.getParams().getPostSpeechTimer();
                // Start interdigit timer
                if (log.isTraceEnabled()) {
                    log.trace("Scheduled Inter Digit Timer to fire in " + interDigitTimerInMilliseconds + " ms");
                }
                this.executor.schedule(new DetectorTimer(context), interDigitTimerInMilliseconds, TimeUnit.MILLISECONDS);
            }

        }
    }

    @Override
    public void exitCollecting(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited COLLECTING state");
        }

        // We are turning speech detection off
        this.asrEngine.stopSpeechDetection();

        if (context.isEndInputKeySpecified()) {
            ((DtmfSinkFacade) this.detector).deactivate();
            this.detector.forget(this.detectorObserver);
        }
    }

    private void deactivateAsrEngine() {
        this.asrEngine.setListener(null);
        this.asrEngine.deactivate();
    }

    @Override
    public void enterWaitingForResponse(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered WAITING_FOR_RESPONSE state");
        }
        if (context.isDigitsOnlyMode()) {
            // We do not wait for response from ASR service
            fire(AsrEvent.WAITING_FOR_RESPONSE_TIMEOUT, context);
        } else {
            long timeToWaitInNanoseconds = getTimeToWaitForResponseInNanoseconds();
            if (timeToWaitInNanoseconds > 0) {
                if (log.isTraceEnabled()) {
                    log.trace("Starting WAITING_FOR_RESPONSE timeout: " + timeToWaitInNanoseconds + " nanoseconds");
                }
                this.executor.schedule(new WaitingForResponseTimer(), timeToWaitInNanoseconds, TimeUnit.NANOSECONDS);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("WAITING_FOR_RESPONSE timeout is already expired");
                }
                fire(AsrEvent.WAITING_FOR_RESPONSE_TIMEOUT, context);
            }
        }
    }

    @Override
    public void exitWaitingForResponse(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited WAITING_FOR_RESPONSE state");
        }
    }

    @Override
    public void enterEvaluating(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered EVALUATING state.");
        }
        if (context.isDigitsOnlyMode()) {
            final int digitCount = context.countCollectedDigits();
            if (context.hasDigitPattern()) {
                // Succeed if digit pattern matches. Otherwise retry
                if (context.getCollectedDigits().matches(context.getDigitPattern())) {
                    fire(AsrEvent.SUCCEED, context);
                } else {
                    fire(AsrEvent.PATTERN_MISMATCH, context);
                }
            } else if (digitCount < context.getParams().getMinimumNumDigits()) {
                // Minimum digits not met
                fire(AsrEvent.PATTERN_MISMATCH, context);
            } else {
                // Pattern validation was successful
                fire(AsrEvent.SUCCEED, context);
            }
        } else if (!StringUtils.isEmpty(context.getFinalRecognizedText())) {
            fire(AsrEvent.SUCCEED, context);
        } else {
            fire(AsrEvent.NO_RECOGNIZED_TEXT, context);
        }
    }

    @Override
    public void exitEvaluating(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited EVALUATING state");
        }
    }

    @Override
    public void enterCanceled(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered CANCELED state");
        }
        deactivateAsrEngine();
        if (!StringUtils.isEmpty(context.getFinalRecognizedText())) {
            fire(AsrEvent.SUCCEED, context);
        } else {
            context.setReturnCode(ReturnCode.NO_SPEECH.code());
            fire(AsrEvent.FAIL, context);
        }
    }

    @Override
    public void exitCanceled(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited CANCELED state");
        }
    }

    @Override
    public void enterSucceeding(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered SUCCEEDING state");
        }

        final Playlist prompt = context.getSuccessAnnouncement();
        if (prompt.isEmpty()) {
            fire(AsrEvent.NO_PROMPT, context);
        } else {
            fire(AsrEvent.PROMPT, context);
        }
    }

    @Override
    public void exitSucceeding(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered SUCCEEDING state");
        }
    }

    @Override
    public void enterPlayingSuccess(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered PLAYING SUCCESS state");
        }

        final Playlist prompt = context.getSuccessAnnouncement();
        final String track = prompt.next();
        try {
            this.player.addListener(this.playerListener);
            playAnnouncement(track, 0L);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(AsrEvent.FAIL, context);
        }
    }

    @Override
    public void onPlayingSuccess(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On PLAYING SUCCESS state");
        }

        final Playlist prompt = context.getSuccessAnnouncement();
        final String track = prompt.next();

        if (track.isEmpty()) {
            // No more announcements to play
            fire(AsrEvent.END_PROMPT, context);
        } else {
            playAnnouncement(track, 10 * 100);
        }
    }

    @Override
    public void exitPlayingSuccess(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PLAYING SUCCESS state");
        }

        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }

    @Override
    public void enterSucceeded(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered SUCCEEDED state");
        }
        final OperationComplete operationComplete = new OperationComplete(AsrSignal.SYMBOL, ReturnCode.SUCCESS.code());
        if (context.isDigitsOnlyMode()) {
            operationComplete.setParameter(ReturnParameters.DIGITS_COLLECTED.symbol(), context.getCollectedDigits());
        } else {
            operationComplete.setParameter(ReturnParameters.ASR_RESULT.symbol(),
                    new String(Hex.encodeHex(context.getFinalRecognizedText().getBytes())));
        }
        mgcpEventSubject.notify(mgcpEventSubject, operationComplete);
    }

    @Override
    public void enterFailing(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered FAILING state");
        }

        switch (event) {
            case NO_RECOGNIZED_TEXT:
                context.setReturnCode(ReturnCode.NO_SPEECH.code());
                break;
            case PATTERN_MISMATCH:
                context.setReturnCode(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code());
                break;
            default:
                context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
                break;
        }

        final Playlist prompt = context.getFailureAnnouncement();
        if (prompt.isEmpty()) {
            fire(AsrEvent.NO_PROMPT, context);
        } else {
            fire(AsrEvent.PROMPT, context);
        }
    }

    @Override
    public void exitFailing(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited FAILING state");
        }
    }

    @Override
    public void enterPlayingFailure(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered PLAYING FAILURE state");
        }

        final Playlist prompt = context.getFailureAnnouncement();
        final String track = prompt.next();
        try {
            this.player.addListener(this.playerListener);
            playAnnouncement(track, 0L);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(AsrEvent.FAIL, context);
        }
    }

    @Override
    public void onPlayingFailure(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On PLAYING FAILURE state");
        }

        final Playlist prompt = context.getFailureAnnouncement();
        final String track = prompt.next();

        if (track.isEmpty()) {
            // No more announcements to play
            fire(AsrEvent.END_PROMPT, context);
        } else {
            playAnnouncement(track, 10 * 100);
        }
    }

    @Override
    public void exitPlayingFailure(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PLAYING FAILURE state");
        }

        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }

    @Override
    public void enterFailed(AsrState from, AsrState to, AsrEvent event, AsrContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered FAILED state");
        }
        final OperationFailed operationFailed = new OperationFailed(AsrSignal.SYMBOL, context.getReturnCode());
        mgcpEventSubject.notify(mgcpEventSubject, operationFailed);
    }

    private void fireEndInput() {
        if (getTimeToWaitForResponseInNanoseconds() > 0) {
            fire(AsrEvent.END_INPUT, context);
        } else {
            fire(AsrEvent.END_INPUT_WITHOUT_WAITING_FOR_RESPONSE, context);
        }
    }

    private long getTimeToWaitForResponseInNanoseconds() {
        final int responseTimeoutInMilliseconds = asrEngine.getResponseTimeoutInMilliseconds();
        final long responseTimeoutInNanoseconds = TimeUnit.MILLISECONDS.toNanos(responseTimeoutInMilliseconds);
        final long idleTimeInNanoseconds = inputTimeoutDetector.getIdleTimeInNanoseconds();
        final long timeToWaitInNanoseconds = (idleTimeInNanoseconds >= 0 ? responseTimeoutInNanoseconds - idleTimeInNanoseconds
                : idleTimeInNanoseconds);
        if (log.isTraceEnabled()) {
            log.trace("getTimeToWaitForResponseInNanoseconds: timeToWaitInNanoseconds = " + timeToWaitInNanoseconds
                    + ", responseTimeout=" + responseTimeoutInMilliseconds + " milliseconds");
        }
        return timeToWaitInNanoseconds;
    }

    private boolean isStillCollecting() {
        return AsrState.PLAY_COLLECT.equals(getCurrentState());
    }

    private final class LocalInputTimeoutListener implements InputTimeoutListener {
        @Override
        public void onPreSpeechTimer() {
            if (log.isTraceEnabled()) {
                log.trace("onPreSpeechTimer");
            }
            fire(AsrEvent.TIMEOUT, context);
        }

        @Override
        public void onPostSpeechTimer() {
            if (log.isTraceEnabled()) {
                log.trace("onPostSpeechTimer");
            }
            if (!context.isDigitsOnlyMode() && isStillCollecting()) {
                fireEndInput();
            }
        }

        @Override
        public void onMaximumRecognitionTime() {
            if (log.isTraceEnabled() && isStillCollecting()) {
                log.trace("onMaximumRecognitionTime");
            }
            fireEndInput();
        }
    }

    private final class LocalSpeechDetectorListener implements VoiceActivityDetectorListener {
        @Override
        public void onVoiceActivityDetected() {
            if (log.isTraceEnabled() && isStillCollecting()) {
                log.trace("onVoiceActivityDetected");
            }
            inputTimeoutDetector.processInput();
        }
    }

    private final class LocalAsrEngineListener implements AsrEngineListener {
        @Override
        public void onSpeechRecognized(final String text, final boolean isFinal) {
            if (StringUtils.isEmpty(text)) {
                if (log.isTraceEnabled()) {
                    log.trace("Recognized text is empty. Ignore it");
                }
                return;
            }
            if (log.isTraceEnabled()) {
                log.trace("onSpeechRecognized: text=" + text + ", isFinal=" + isFinal);
            }
            if (isStillCollecting()) {
                if (!context.isDigitsOnlyMode()) {
                    context.appendRecognizedText(text, isFinal);
                    fire(AsrEvent.RECOGNIZED_TEXT, context);
                } else if (log.isTraceEnabled()) {
                    log.trace("We are in DigitsOnly mode, so we ignore recognized text");
                }
            }
        }

        @Override
        public void onDriverError(AsrDriverException error) {
            if (log.isTraceEnabled()) {
                log.trace("onDriverError", error);
            }
            if (isStillCollecting()) {
                if (!context.isDigitsOnlyMode()) {
                    context.setReturnCode(ReturnCode.PROVISIONING_ERROR.code());
                    fire(AsrEvent.DRIVER_ERROR, context);
                } else if (log.isTraceEnabled()) {
                    log.trace("We are in DigitsOnly mode, so we ignore driver error");
                }
            }
        }
    }

    /**
     * Observes for DTMF events raised by the DTMF Detector.
     *
     * @author Henrique Rosa (henrique.rosa@telestax.com)
     */
    private final class DetectorObserver implements DtmfEventObserver {

        @Override
        public void onDtmfEvent(DtmfEvent event) {
            final char tone = event.getTone().charAt(0);
            context.setLastTone(tone);
            AsrFsmImpl.this.fire(AsrEvent.DTMF_TONE, AsrFsmImpl.this.context);
        }

    }

    /**
     * Listen to Play events raised by the Player.
     *
     * @author Henrique Rosa (henrique.rosa@telestax.com)
     */
    private final class AudioPlayerListener implements PlayerListener {

        @Override
        public void process(PlayerEvent event) {
            switch (event.getID()) {
                case PlayerEvent.STOP:
                    AsrFsmImpl.this.fire(AsrEvent.NEXT_TRACK, AsrFsmImpl.this.context);
                    break;

                case PlayerEvent.FAILED:
                    // TODO handle player failure
                    break;

                default:
                    break;
            }
        }
    }

    private final class WaitingForResponseTimer implements Runnable {
        @Override
        public void run() {
            if (log.isTraceEnabled()) {
                log.trace("WAITING_FOR_RESPONSE timout is reached");
            }
            if (isStillCollecting()) {
                fire(AsrEvent.WAITING_FOR_RESPONSE_TIMEOUT, context);
            }
        }

    }

    /**
     * Timer that defines interval the system will wait for user's input. May interrupt Collect process.
     */
    private final class DetectorTimer implements Runnable {

        private final long timestamp;
        private final AsrContext context;

        DetectorTimer(AsrContext context) {
            this.timestamp = System.currentTimeMillis();
            this.context = context;
        }

        @Override
        public void run() {
            if (context.getLastCollectedDigitOn() <= this.timestamp) {
                if (isStillCollecting()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Timing out collect operation! " + context.getLastCollectedDigitOn() + " <= " + this.timestamp);
                    }
                    fire(AsrEvent.TIMEOUT, context);
                }
            } else if (log.isTraceEnabled()) {
                log.trace("Aborting timeout operation because a tone has been received in the meantime.");
            }
        }

    }

}
