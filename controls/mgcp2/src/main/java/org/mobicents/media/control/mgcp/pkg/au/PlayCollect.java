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

package org.mobicents.media.control.mgcp.pkg.au;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.pkg.AbstractMgcpSignal;
import org.mobicents.media.control.mgcp.pkg.SignalType;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.dtmf.DtmfEvent;
import org.mobicents.media.server.spi.listener.Event;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerEvent;
import org.mobicents.media.server.spi.player.PlayerListener;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Monitor;
import com.google.common.util.concurrent.Monitor.Guard;

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

    /**
     * Specified in units of 100 milliseconds. Defaults to 10 (1 second).
     */
    private static final long INTERVAL = 10 * 1000000L;

    // Concurrency Components
    private final ListeningExecutorService executor;
    private final Monitor monitor;
    private final Guard hasEvents;

    // Media Components
    private final Player player;
    private final DtmfDetector detector;

    // Playlists
    private final Playlist initialPrompt;
    private final Playlist reprompt;
    private final Playlist noDigitsReprompt;
    private final Playlist failureAnnouncement;
    private final Playlist successAnnouncement;

    // Runtime Context
    private final PlayerProducer playerListener;
    private final DetectorProducer dtmfListener;
    private final EventConsumer consumer;

    private final Queue<Event<?>> events;
    private final AtomicInteger eventCount;
    private final StringBuilder sequence;
    private final AtomicInteger attempt;
    private final ConsumerCallback consumerCallback;

    public PlayCollect(Player player, DtmfDetector detector, Map<String, String> parameters,
            ListeningExecutorService executor) {
        super(AudioPackage.PACKAGE_NAME, "pc", SignalType.TIME_OUT, parameters);

        // Concurrency Components
        this.executor = executor;
        this.monitor = new Monitor(true);
        this.hasEvents = new Guard(this.monitor) {

            @Override
            public boolean isSatisfied() {
                return !PlayCollect.this.events.isEmpty();
            }

        };

        // Media Components
        this.player = player;
        this.detector = detector;

        // Playlists
        this.initialPrompt = new Playlist(getInitialPrompt(), 1);
        this.reprompt = new Playlist(getReprompt(), 1);
        this.noDigitsReprompt = new Playlist(getNoDigitsReprompt(), 1);
        this.failureAnnouncement = new Playlist(getFailureAnnouncement(), 1);
        this.successAnnouncement = new Playlist(getSuccessAnnouncement(), 1);

        // Runtime Context
        this.playerListener = new PlayerProducer();
        this.dtmfListener = new DetectorProducer();
        this.consumer = new EventConsumer();
        this.consumerCallback = new ConsumerCallback();

        this.events = new ConcurrentLinkedQueue<>();
        this.eventCount = new AtomicInteger(0);
        this.sequence = new StringBuilder();
        this.attempt = new AtomicInteger(0);
    }

    /**
     * The initial announcement prompting the user to either enter DTMF digits or to speak.
     * <p>
     * Consists of one or more audio segments.<br>
     * If not specified (the default), the event immediately begins digit collection or recording.
     * </p>
     * 
     * @return The array of audio prompts. Array will be empty if none is specified.
     */
    private String[] getInitialPrompt() {
        return Optional.fromNullable(getParameter(SignalParameters.INITIAL_PROMPT.symbol())).or("").split(",");
    }

    /**
     * Played after the user has made an error such as entering an invalid digit pattern or not speaking.
     * <p>
     * Consists of one or more audio segments. <b>Defaults to the Initial Prompt.</b>
     * </p>
     * 
     * @return The array of audio prompts. Array will be empty if none is specified.
     */
    private String[] getReprompt() {
        String segments = Optional.fromNullable(getParameter(SignalParameters.REPROMPT.symbol())).or("");
        if (segments.isEmpty()) {
            segments = Optional.fromNullable(getParameter(SignalParameters.INITIAL_PROMPT.symbol())).or("");
        }
        return segments.split(",");
    }

    /**
     * Played after the user has failed to enter a valid digit pattern during a PlayCollect event.
     * <p>
     * Consists of one or more audio segments. <b>Defaults to the Reprompt.</b>
     * </p>
     * 
     * @return The array of audio prompts. Array will be empty if none is specified.
     */
    private String[] getNoDigitsReprompt() {
        String segments = Optional.fromNullable(getParameter(SignalParameters.NO_DIGITS_REPROMPT.symbol())).or("");
        if (segments.isEmpty()) {
            segments = Optional.fromNullable(getParameter(SignalParameters.REPROMPT.symbol())).or("");
        }
        return segments.split(",");
    }

    /**
     * Played when all data entry attempts have failed.
     * <p>
     * Consists of one or more audio segments. No default.
     * </p>
     * 
     * @return The array of audio prompts. Array will be empty if none is specified.
     */
    private String[] getFailureAnnouncement() {
        return Optional.fromNullable(getParameter(SignalParameters.FAILURE_ANNOUNCEMENT.symbol())).or("").split(",");
    }

    /**
     * Played when all data entry attempts have succeeded.
     * <p>
     * Consists of one or more audio segments. No default.
     * </p>
     * 
     * @return The array of audio prompts. Array will be empty if none is specified.
     */
    private String[] getSuccessAnnouncement() {
        return Optional.fromNullable(getParameter(SignalParameters.FAILURE_ANNOUNCEMENT.symbol())).or("").split(",");
    }

    /**
     * If set to true, initial prompt is not interruptible by either voice or digits.
     * <p>
     * <b>Defaults to false.</b> Valid values are the text strings "true" and "false".
     * </p>
     * 
     * @return
     */
    private boolean getNonInterruptibleAudio() {
        String value = Optional.fromNullable(getParameter(SignalParameters.NON_INTERRUPTIBLE_PLAY.symbol())).or("false");
        return Boolean.parseBoolean(value);
    }

    /**
     * If set to true, clears the digit buffer before playing the initial prompt.
     * <p>
     * <b>Defaults to false.</b> Valid values are the text strings "true" and "false".
     * </p>
     * 
     * @return
     */
    private boolean getClearDigitBuffer() {
        String value = Optional.fromNullable(getParameter(SignalParameters.CLEAR_DIGIT_BUFFER.symbol())).or("false");
        return Boolean.parseBoolean(value);
    }

    /**
     * The minimum number of digits to collect.
     * <p>
     * <b>Defaults to one.</b> This parameter should not be specified if the Digit Pattern parameter is present.
     * </p>
     * 
     * @return
     */
    private int getMinimumDigits() {
        String value = Optional.fromNullable(getParameter(SignalParameters.MINIMUM_NUM_DIGITS.symbol())).or("1");
        return Integer.parseInt(value);
    }

    /**
     * The maximum number of digits to collect.
     * <p>
     * <b>Defaults to one.</b> This parameter should not be specified if the Digit Pattern parameter is present.
     * </p>
     * 
     * @return
     */
    private int getMaximumDigits() {
        String value = Optional.fromNullable(getParameter(SignalParameters.MAXIMUM_NUM_DIGITS.symbol())).or("1");
        return Integer.parseInt(value);
    }

    /**
     * A legal digit map as described in <a href="https://tools.ietf.org/html/rfc2885#section-7.1.14">section 7.1.14</a> of the
     * MEGACO protocol using the DTMF mappings associated with the Megaco DTMF Detection Package described in the Megaco
     * protocol document.
     * <p>
     * <b>This parameter should not be specified if one or both of the Minimum # Of Digits parameter and the Maximum Number Of
     * Digits parameter is present.</b>
     * </p>
     * 
     * @return The digit pattern or an empty String if not specified.
     */
    private String getDigitPattern() {
        String pattern = Optional.fromNullable(getParameter(SignalParameters.DIGIT_PATTERN.symbol())).or("");
        if (!pattern.isEmpty()) {
            // Replace pattern to comply with MEGACO digitMap
            pattern.replace(".", "+");
            pattern.replace("x", "\\d");
            pattern.replace("*", "\\*");
        }
        return pattern;
    }

    /**
     * The amount of time allowed for the user to enter the first digit.
     * <p>
     * Specified in units of 100 milliseconds. <b>Defaults to 50 (5 seconds).</b>
     * </p>
     * 
     * @return
     */
    private int getFirstDigitTimer() {
        String value = Optional.fromNullable(getParameter(SignalParameters.FIRST_DIGIT_TIMER.symbol())).or("50");
        return Integer.parseInt(value) * 100;
    }

    /**
     * The amount of time allowed for the user to enter each subsequent digit.
     * <p>
     * Specified units of 100 milliseconds seconds. <b>Defaults to 30 (3 seconds).</b>
     * </p>
     * 
     * @return
     */
    private int getInterDigitTimer() {
        String value = Optional.fromNullable(getParameter(SignalParameters.INTER_DIGIT_TIMER.symbol())).or("30");
        return Integer.parseInt(value) * 100;
    }

    /**
     * The amount of time to wait for a user to enter a final digit once the maximum expected amount of digits have been
     * entered.
     * <p>
     * Typically this timer is used to wait for a terminating key in applications where a specific key has been defined to
     * terminate input.
     * </p>
     * <p>
     * Specified in units of 100 milliseconds. </b>If not specified, this timer is not activated.</b>
     * </p>
     * 
     * @return
     */
    private int getExtraDigitTimer() {
        String value = Optional.fromNullable(getParameter(SignalParameters.EXTRA_DIGIT_TIMER.symbol())).or("");
        return Integer.parseInt(value) * 100;
    }

    /**
     * Defines a key sequence consisting of a command key optionally followed by zero or more keys. This key sequence has the
     * following action: discard any digits collected or recording in progress, replay the prompt, and resume digit collection
     * or recording.
     * <p>
     * <b>No default.</b> An application that defines more than one command key sequence, will typically use the same command
     * key for all command key sequences.
     * <p>
     * If more than one command key sequence is defined, then all key sequences must consist of a command key plus at least one
     * other key.
     * </p>
     * 
     * @return
     */
    private char getRestartKey() {
        String value = Optional.fromNullable(getParameter(SignalParameters.RESTART_KEY.symbol())).or("");
        return value.isEmpty() ? ' ' : value.charAt(0);
    }

    /**
     * Defines a key sequence consisting of a command key optionally followed by zero or more keys. This key sequence has the
     * following action: discard any digits collected or recordings in progress and resume digit collection or recording.
     * <p>
     * <b>No default.</b>
     * </p>
     * An application that defines more than one command key sequence, will typically use the same command key for all command
     * key sequences.
     * </p>
     * <p>
     * If more than one command key sequence is defined, then all key sequences must consist of a command key plus at least one
     * other key.
     * </p>
     * 
     * @return
     */
    private char getReinputKey() {
        String value = Optional.fromNullable(getParameter(SignalParameters.REINPUT_KEY.symbol())).or("");
        return value.isEmpty() ? ' ' : value.charAt(0);
    }

    /**
     * Defines a key sequence consisting of a command key optionally followed by zero or more keys. This key sequence has the
     * following action: terminate the current event and any queued event and return the terminating key sequence to the call
     * processing agent.
     * <p>
     * <b> No default.</b> An application that defines more than one command key sequence, will typically use the same command
     * key for all command key sequences.
     * <p>
     * If more than one command key sequence is defined, then all key sequences must consist of a command key plus at least one
     * other key.
     * </p>
     * 
     * @return
     */
    private char getReturnKey() {
        String value = Optional.fromNullable(getParameter(SignalParameters.RETURN_KEY.symbol())).or("");
        return value.isEmpty() ? ' ' : value.charAt(0);
    }

    /**
     * Defines a key with the following action. Stop playing the current announcement and resume playing at the beginning of the
     * first, last, previous, next, or the current segment of the announcement.
     * <p>
     * <b>No default. The actions for the position key are fst, lst, prv, nxt, and cur.</b>
     * </p>
     * 
     * @return
     */
    private char getPositionKey() {
        String value = Optional.fromNullable(getParameter(SignalParameters.POSITION_KEY.symbol())).or("");
        return value.isEmpty() ? ' ' : value.charAt(0);
    }

    /**
     * Defines a key with the following action. Terminate playback of the announcement.
     * <p>
     * <b>No default.</b>
     * </p>
     * 
     * @return
     */
    private char getStopKey() {
        String value = Optional.fromNullable(getParameter(SignalParameters.STOP_KEY.symbol())).or("");
        return value.isEmpty() ? ' ' : value.charAt(0);
    }

    /**
     * Defines a set of keys that are acceptable as the first digit collected. This set of keys can be specified to interrupt a
     * playing announcement or to not interrupt a playing announcement.
     * <p>
     * <b>The default key set is 0-9. The default behavior is to interrupt a playing announcement when a Start Input Key is
     * pressed.</b>
     * </p>
     * <p>
     * This behavior can be overidden for the initial prompt only by using the ni (Non-Interruptible Play) parameter.
     * Specification is a list of keys with no separators, e.g. 123456789#.
     * </p>
     * 
     * @return
     */
    private String getStartInputKeys() {
        return Optional.fromNullable(getParameter(SignalParameters.START_INPUT_KEY.symbol())).or("0-9");
    }

    /**
     * Specifies a key that signals the end of digit collection or voice recording.
     * <p>
     * <b>The default end input key is the # key.</b> To specify that no End Input Key be used the parameter is set to the
     * string "null".
     * <p>
     * <b>The default behavior not to return the End Input Key in the digits returned to the call agent.</b> This behavior can
     * be overridden by the Include End Input Key (eik) parameter.
     * </p>
     * 
     * @return
     */
    private char getEndInputKey() {
        String value = Optional.fromNullable(getParameter(SignalParameters.END_INPUT_KEY.symbol())).or("");
        return value.isEmpty() ? '#' : value.charAt(0);
    }

    /**
     * By default the End Input Key is not included in the collected digits returned to the call agent. If this parameter is set
     * to "true" then the End Input Key will be returned with the collected digits returned to the call agent.
     * <p>
     * <b>Default is "false".</b>
     * </p>
     * 
     * @return
     */
    private boolean getIncludeEndInputKey() {
        String value = Optional.fromNullable(getParameter(SignalParameters.INCLUDE_END_INPUT_KEY.symbol())).or("false");
        return Boolean.parseBoolean(value);
    }

    /**
     * The number of attempts the user needed to enter a valid digit pattern or to make a recording.
     * <p>
     * <b>Defaults to 1.</b> Also used as a return parameter to indicate the number of attempts the user made.
     * </p>
     * 
     * @return
     */
    private int getNumberOfAttempts() {
        String value = Optional.fromNullable(getParameter(SignalParameters.NUMBER_OF_ATTEMPTS.symbol())).or("1");
        return Integer.parseInt(value);
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

    private void startCollectPhase(int timeout) {
        // Register DTMF listener to receive incoming tones
        try {
            this.detector.addListener(this.dtmfListener);
        } catch (TooManyListenersException e) {
            log.error("Could not add listener to DTMF Detector: Too many listeners.");
        }

        // TODO set timer to expire in [timeout] ms

        // Activate DTMF Detector
        this.detector.activate();
        this.attempt.incrementAndGet();

        if (log.isInfoEnabled()) {
            log.info("Started collect phase. Attempt: " + this.attempt.get());
        }
    }

    private void stopCollectPhase() {
        this.detector.deactivate();
        this.detector.removeListener(this.dtmfListener);

        if (log.isInfoEnabled()) {
            log.info("Stopped collect phase.");
        }
    }

    private void fireOC(int code, int attempts, String digitsCollected) {
        final OperationComplete operationComplete = new OperationComplete(getSymbol(), code);
        operationComplete.setParameter("na", String.valueOf(attempts));
        operationComplete.setParameter("dc", digitsCollected);
        notify(this, operationComplete);
    }

    @Override
    public void execute() {
        if (this.executing.getAndSet(true)) {
            throw new IllegalStateException("Already executing.");
        }

        // Initialize event consumer thread
        ListenableFuture<?> future = this.executor.submit(this.consumer);
        Futures.addCallback(future, this.consumerCallback);

        String prompt = this.initialPrompt.next();
        if (!prompt.isEmpty()) {
            // XXX Start prompt phase
        } else {
            startCollectPhase(getFirstDigitTimer());
        }
    }

    @Override
    public void cancel() {
        if (!this.executing.getAndSet(false)) {
            throw new IllegalStateException("Already canceled.");
        }

    }

    /**
     * Listens to events coming from Player and puts them in the events queue.
     * 
     * @author Henrique Rosa (henrique.rosa@telestax.com)
     *
     */
    private class PlayerProducer implements PlayerListener {

        @Override
        public void process(PlayerEvent event) {
            if (PlayCollect.this.executing.get()) {
                PlayCollect.this.events.offer(event);
            }
        }

    }

    /**
     * Listens to events coming from DTMF Detector and puts them in the events queue.
     * 
     * @author Henrique Rosa (henrique.rosa@telestax.com)
     *
     */
    private class DetectorProducer implements DtmfDetectorListener {

        @Override
        public void process(DtmfEvent event) {
            if (PlayCollect.this.executing.get()) {
                PlayCollect.this.events.offer(event);
            }
        }

    }

    /**
     * Consumes events from the {@link PlayCollect#events} queue.
     * <p>
     * Runs on separate thread and waits until at least one event is available in the queue to be consumed.<b>Can be
     * interrupted.</b>
     * </p>
     * 
     * @author Henrique Rosa (henrique.rosa@telestax.com)
     *
     */
    private class EventConsumer implements Runnable {

        @Override
        public void run() {
            if (PlayCollect.this.executing.get()) {

                if (PlayCollect.this.monitor.enterIf(hasEvents)) {
                    try {
                        Event<?> event = PlayCollect.this.events.poll();
                        if (event != null) {
                            if (event instanceof DtmfEvent) {
                                onDtmfEvent((DtmfEvent) event);
                            } else if (event instanceof PlayerEvent) {
                                onPlayerEvent((PlayerEvent) event);
                            }
                        }
                    } finally {
                        PlayCollect.this.monitor.leave();
                    }
                }
            }
        }

        private void onDtmfEvent(DtmfEvent event) {
            final char tone = event.getTone().charAt(0);

            if (log.isInfoEnabled()) {
                log.info("Received tone " + event.getTone());
            }

            if (tone == getEndInputKey()) {
                // Stop collect phase if EndInput key was pressed
                PlayCollect.this.executing.set(false);
                stopCollectPhase();
                if (getIncludeEndInputKey()) {
                    PlayCollect.this.sequence.append(tone);
                }
                fireOC(ReturnCode.SUCCESS.code(), PlayCollect.this.attempt.get(), PlayCollect.this.sequence.toString());
            } else {
                // Collect tone and add it to list of pressed digits
                PlayCollect.this.sequence.append(tone);
            }

        }

        private void onPlayerEvent(PlayerEvent event) {
            // TODO implement onPlayerEvent
        }

    }

    /**
     * Listens to termination of {@link EventConsumer} execution.
     * 
     * @author Henrique Rosa (henrique.rosa@telestax.com)
     *
     */
    private class ConsumerCallback implements FutureCallback<Object> {

        @Override
        public void onSuccess(Object result) {
            if (PlayCollect.this.executing.get()) {
                ListenableFuture<?> future = executor.submit(consumer);
                Futures.addCallback(future, consumerCallback);
            }

        }

        @Override
        public void onFailure(Throwable t) {
            log.error("Could not process event", t);
            if (PlayCollect.this.executing.get()) {
                ListenableFuture<?> future = executor.submit(consumer);
                Futures.addCallback(future, consumerCallback);
            }
        }

    }

}
