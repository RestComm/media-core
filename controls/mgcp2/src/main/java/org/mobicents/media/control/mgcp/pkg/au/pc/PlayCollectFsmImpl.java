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

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.pkg.MgcpEventSubject;
import org.mobicents.media.control.mgcp.pkg.au.OperationComplete;
import org.mobicents.media.control.mgcp.pkg.au.OperationFailed;
import org.mobicents.media.control.mgcp.pkg.au.Playlist;
import org.mobicents.media.control.mgcp.pkg.au.ReturnCode;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerListener;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayCollectFsmImpl extends AbstractStateMachine<PlayCollectFsm, PlayCollectState, Object, PlayCollectContext>
        implements PlayCollectFsm {

    private static final Logger log = Logger.getLogger(PlayCollectFsmImpl.class);

    // Scheduler
    private final ListeningScheduledExecutorService executor;

    // Event Listener
    private final MgcpEventSubject mgcpEventSubject;

    // Media Components
    private final DtmfDetector detector;
    final DtmfDetectorListener detectorListener;

    private final Player player;
    final PlayerListener playerListener;

    // Execution Context
    private final PlayCollectContext context;

    public PlayCollectFsmImpl(DtmfDetector detector, DtmfDetectorListener detectorListener, Player player,
            PlayerListener playerListener, MgcpEventSubject mgcpEventSubject, ListeningScheduledExecutorService executor,
            PlayCollectContext context) {
        super();
        // Scheduler
        this.executor = executor;

        // Event Listener
        this.mgcpEventSubject = mgcpEventSubject;

        // Media Components
        this.detector = detector;
        this.detectorListener = detectorListener;

        this.player = player;
        this.playerListener = playerListener;

        // Execution Context
        this.context = context;
    }

    private void playAnnouncement(String url, long delay) {
        try {
            this.player.setInitialDelay(delay);
            this.player.setURL(url);
            this.player.activate();

            if (log.isInfoEnabled()) {
                log.info("Playing announcement " + url);
            }
        } catch (MalformedURLException e) {
            log.error("Could not play malformed segment " + url, e);
            context.setReturnCode(ReturnCode.BAD_AUDIO_ID.code());
            fire(PlayCollectEvent.FAIL, context);
            // TODO create transition from PROMPTING to FAILED
        } catch (ResourceUnavailableException e) {
            log.error("Could not play unavailable segment " + url, e);
            context.setReturnCode(ReturnCode.BAD_AUDIO_ID.code());
            fire(PlayCollectEvent.FAIL, context);
            // TODO create transition from PROMPTING to FAILED
        }
    }

    @Override
    public void onReady(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        this.context.newAttempt();
        if (this.context.getInitialPrompt().isEmpty()) {
            fire(PlayCollectEvent.COLLECT, this.context);
        } else {
            fire(PlayCollectEvent.PROMPT, this.context);
        }
    }

    @Override
    public void enterPrompting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        final Playlist prompt = context.getInitialPrompt();
        try {
            this.player.addListener(this.playerListener);
            playAnnouncement(prompt.next(), 0L);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(PlayCollectEvent.FAIL, context);
        }
    }

    @Override
    public void onPrompting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        final Playlist prompt = context.getInitialPrompt();
        final String next = prompt.next();

        if (next.isEmpty()) {
            // No more announcements to play
            fire(PlayCollectEvent.COLLECT, context);
        } else {
            playAnnouncement(next, 10 * 100);
        }
    }

    @Override
    public void exitPrompting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }

    @Override
    public void enterCollecting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        try {
            // Activate DTMF detector and bind listener
            this.detector.addListener(this.detectorListener);
            this.detector.activate();

            if (log.isInfoEnabled()) {
                log.info("Started collect phase. Attempt: " + context.getAttempt());
            }

            // Activate timer for first digit
            this.executor.schedule(new DetectorTimer(context), context.getFirstDigitTimer(), TimeUnit.MILLISECONDS);
        } catch (TooManyListenersException e) {
            log.error("Too many DTMF listeners", e);
        }
    }

    @Override
    public void exitCollecting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        // Deactivate DTMF detector and release listener
        this.detector.removeListener(this.detectorListener);
        this.detector.deactivate();

        if (log.isInfoEnabled()) {
            log.info("Stopped collect phase. Attempt: " + context.getAttempt());
        }
    }

    @Override
    public void onCollecting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        final char tone = ((DtmfToneEvent) event).tone();
        final char endInputKey = context.getEndInputKey();
        final char restartKey = context.getRestartKey();

        if (log.isInfoEnabled()) {
            log.info("Received tone " + tone);
        }

        if (restartKey == tone) {
            if (context.hasMoreAttempts()) {
                // Tell context that a key was received to cancel any timeout.
                context.collectDigit(tone);

                // Restart Collect operation
                fire(PlayCollectEvent.RESTART, context);
            } else {
                // Max number of attempts reached. Fail.
                context.setReturnCode(ReturnCode.MAX_ATTEMPTS_EXCEEDED.code());
                if (context.hasFailureAnnouncement()) {
                    fire(PlayCollectEvent.PLAY_FAILURE, context);
                } else {
                    fire(PlayCollectEvent.FAIL, context);
                }
            }
        } else if (endInputKey == tone) {
            if (context.hasDigitPattern()) {
                // Check if list of collected digits match the digit pattern
                if (context.getCollectedDigits().matches(context.getDigitPattern())) {
                    // Append endInputKey (if required)
                    if (context.getIncludeEndInputKey()) {
                        context.collectDigit(tone);
                    }

                    // Digit Collection succeeded
                    if (context.hasSuccessAnnouncement()) {
                        fire(PlayCollectEvent.PLAY_SUCCESS, context);
                    } else {
                        fire(PlayCollectEvent.SUCCEED, context);
                    }
                } else {
                    // Retry if more attempts are available. If not, fail.
                    if (context.hasMoreAttempts()) {
                        // Clear digits and play reprompt
                        if (context.countCollectedDigits() == 0) {
                            fire(PlayCollectEvent.NO_DIGITS_REPROMPT, context);
                        } else {
                            fire(PlayCollectEvent.REPROMPT, context);
                        }
                    } else {
                        // Fire failure event
                        context.setReturnCode(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code());
                        if (context.hasFailureAnnouncement()) {
                            fire(PlayCollectEvent.PLAY_FAILURE, context);
                        } else {
                            fire(PlayCollectEvent.FAIL, context);
                        }
                    }
                }
            } else {
                // Check if minimum number of digits was collected
                if (context.getMinimumDigits() <= context.countCollectedDigits()) {
                    // Minimum number of digits was collected
                    // Append endInputKey (if required)
                    if (context.getIncludeEndInputKey()) {
                        context.collectDigit(tone);
                    }

                    // Digit Collection succeeded
                    if (context.hasSuccessAnnouncement()) {
                        fire(PlayCollectEvent.PLAY_SUCCESS, context);
                    } else {
                        fire(PlayCollectEvent.SUCCEED, context);
                    }
                } else {
                    // Minimum number of digits was NOT collected
                    // Retry if more attempts are available. If not, fail.
                    if (context.hasMoreAttempts()) {
                        // Clear digits and play reprompt
                        if (context.countCollectedDigits() == 0) {
                            fire(PlayCollectEvent.NO_DIGITS_REPROMPT, context);
                        } else {
                            fire(PlayCollectEvent.REPROMPT, context);
                        }
                    } else {
                        // Fire failure event
                        context.setReturnCode(ReturnCode.MAX_ATTEMPTS_EXCEEDED.code());
                        if (context.hasFailureAnnouncement()) {
                            fire(PlayCollectEvent.PLAY_FAILURE, context);
                        } else {
                            fire(PlayCollectEvent.FAIL, context);
                        }
                    }
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
                // Digit Collection succeeded
                if (context.hasSuccessAnnouncement()) {
                    fire(PlayCollectEvent.PLAY_SUCCESS, context);
                } else {
                    fire(PlayCollectEvent.SUCCEED, context);
                }
            } else {
                // Start interdigit timer
                this.executor.schedule(new DetectorTimer(context), context.getInterDigitTimer(), TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void onTimingOut(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        if (log.isInfoEnabled()) {
            log.info("Timing out Collect operation.");
        }

        if (context.hasDigitPattern()) {
            // Check if list of collected digits match the digit pattern
            if (context.getCollectedDigits().matches(context.getDigitPattern())) {
                // Digit Collection succeeded
                if (context.hasSuccessAnnouncement()) {
                    fire(PlayCollectEvent.PLAY_SUCCESS, context);
                } else {
                    fire(PlayCollectEvent.SUCCEED, context);
                }
            } else {
                // Retry if more attempts are available. If not, fail.
                if (context.hasMoreAttempts()) {
                    // Clear digits and play reprompt
                    if (context.countCollectedDigits() == 0) {
                        fire(PlayCollectEvent.NO_DIGITS_REPROMPT, context);
                    } else {
                        fire(PlayCollectEvent.REPROMPT, context);
                    }
                } else {
                    // Fire failure event
                    context.setReturnCode(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code());
                    if (context.hasFailureAnnouncement()) {
                        fire(PlayCollectEvent.PLAY_FAILURE, context);
                    } else {
                        fire(PlayCollectEvent.FAIL, context);
                    }
                }
            }
        } else {
            // Check if minimum number of digits was collected
            if (context.getMinimumDigits() <= context.countCollectedDigits()) {
                // Minimum number of digits was collected
                // Digit Collection succeeded
                if (context.hasSuccessAnnouncement()) {
                    fire(PlayCollectEvent.PLAY_SUCCESS, context);
                } else {
                    fire(PlayCollectEvent.SUCCEED, context);
                }
            } else {
                // Minimum number of digits was NOT collected
                // Retry if more attempts are available. If not, fail.
                if (context.hasMoreAttempts()) {
                    // Clear digits and play reprompt
                    if (context.countCollectedDigits() == 0) {
                        fire(PlayCollectEvent.NO_DIGITS_REPROMPT, context);
                    } else {
                        fire(PlayCollectEvent.REPROMPT, context);
                    }
                } else {
                    // Fire failure event
                    context.setReturnCode(ReturnCode.MAX_ATTEMPTS_EXCEEDED.code());
                    if (context.hasFailureAnnouncement()) {
                        fire(PlayCollectEvent.PLAY_FAILURE, context);
                    } else {
                        fire(PlayCollectEvent.FAIL, context);
                    }
                }
            }
        }
    }

    @Override
    public void enterReprompting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        // Clear collected digits and rewind playlists
        context.newAttempt();

        final Playlist prompt = context.getReprompt();
        final String track = prompt.next();

        log.info("enterReprompting::atempt=" + context.getAttempt() + ", track=" + track);

        // Register player listener
        try {
            this.player.addListener(this.playerListener);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(PlayCollectEvent.FAIL, context);
        }

        // Play reprompt or move to collect state if no prompt is defined
        if (track.isEmpty()) {
            fire(PlayCollectEvent.COLLECT, context);
        } else {
            playAnnouncement(track, 0L);
        }
    }

    @Override
    public void onReprompting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        final Playlist prompt = context.getReprompt();
        final String track = prompt.next();

        log.info("onReprompting::atempt=" + context.getAttempt() + ", track=" + track);

        // Play reprompt or move to collect state if no prompt is defined
        if (track.isEmpty()) {
            fire(PlayCollectEvent.COLLECT, context);
        } else {
            playAnnouncement(track, 0L);
        }
    }

    @Override
    public void exitReprompting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {

        log.info("exitReprompting::atempt=" + context.getAttempt());

        // Deregister player listener
        this.player.removeListener(this.playerListener);

        // Deactivate player
        this.player.deactivate();
    }

    @Override
    public void enterNoDigitsReprompting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        // Clear collected digits and rewind playlists
        context.newAttempt();

        final Playlist prompt = context.getNoDigitsReprompt();
        final String track = prompt.next();

        log.info("enterNoDigitsReprompting::atempt=" + context.getAttempt() + ", track=" + track);

        // Register player listener
        try {
            this.player.addListener(this.playerListener);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(PlayCollectEvent.FAIL, context);
        }

        // Play reprompt or move to collect state if no prompt is defined
        if (track.isEmpty()) {
            fire(PlayCollectEvent.COLLECT, context);
        } else {
            playAnnouncement(track, 0L);
        }
    }

    @Override
    public void onNoDigitsReprompting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        final Playlist prompt = context.getNoDigitsReprompt();
        final String track = prompt.next();

        log.info("onNoDigitsReprompting::atempt=" + context.getAttempt() + ", track=" + track);

        // Play reprompt or move to collect state if no prompt is defined
        if (track.isEmpty()) {
            fire(PlayCollectEvent.COLLECT, context);
        } else {
            playAnnouncement(track, 0L);
        }
    }

    @Override
    public void exitNoDigitsReprompting(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {

        log.info("exitNoDigitsReprompting::atempt=" + context.getAttempt());

        // Deregister player listener
        this.player.removeListener(this.playerListener);

        // Deactivate player
        this.player.deactivate();
    }

    @Override
    public void enterCanceled(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        log.info("Canceled!!!");
        // TODO Auto-generated method stub
    }

    @Override
    public void enterPlayingSuccess(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        final Playlist prompt = context.getSuccessAnnouncement();
        final String track = prompt.next();

        log.info("enterPlayingSuccess::atempt=" + context.getAttempt() + ", track=" + track);

        // Register player listener
        try {
            this.player.addListener(this.playerListener);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(PlayCollectEvent.FAIL, context);
        }

        // Play reprompt or move to collect state if no prompt is defined
        if (track.isEmpty()) {
            fire(PlayCollectEvent.SUCCEED, context);
        } else {
            playAnnouncement(track, 0L);
        }

    }

    @Override
    public void onPlayingSuccess(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        final Playlist prompt = context.getSuccessAnnouncement();
        final String track = prompt.next();

        log.info("onPlayingSuccess::atempt=" + context.getAttempt() + ", track=" + track);

        // Play reprompt or move to collect state if no prompt is defined
        if (track.isEmpty()) {
            fire(PlayCollectEvent.SUCCEED, context);
        } else {
            playAnnouncement(track, 0L);
        }
    }

    @Override
    public void exitPlayingSuccess(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        log.info("exitPlayingSuccess::atempt=" + context.getAttempt());

        // Deregister player listener
        this.player.removeListener(this.playerListener);

        // Deactivate player
        this.player.deactivate();
    }

    @Override
    public void enterSucceeded(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        final String collectedDigits = context.getCollectedDigits();
        final int attempt = context.getAttempt();

        final OperationComplete operationComplete = new OperationComplete(PlayCollect.SYMBOL, ReturnCode.SUCCESS.code());
        operationComplete.setParameter("na", String.valueOf(attempt));
        operationComplete.setParameter("dc", collectedDigits);
        this.mgcpEventSubject.notify(this.mgcpEventSubject, operationComplete);
    }

    @Override
    public void enterPlayingFailure(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        final Playlist prompt = context.getFailureAnnouncement();
        final String track = prompt.next();

        log.info("enterPlayingFailure::atempt=" + context.getAttempt() + ", track=" + track);

        // Register player listener
        try {
            this.player.addListener(this.playerListener);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(PlayCollectEvent.FAIL, context);
        }

        // Play reprompt or move to collect state if no prompt is defined
        if (track.isEmpty()) {
            fire(PlayCollectEvent.FAIL, context);
        } else {
            playAnnouncement(track, 0L);
        }

    }

    @Override
    public void onPlayingFailure(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        final Playlist prompt = context.getFailureAnnouncement();
        final String track = prompt.next();

        log.info("onPlayingFailure::atempt=" + context.getAttempt() + ", track=" + track);

        // Play reprompt or move to collect state if no prompt is defined
        if (track.isEmpty()) {
            fire(PlayCollectEvent.FAIL, context);
        } else {
            playAnnouncement(track, 0L);
        }
    }

    @Override
    public void exitPlayingFailure(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        log.info("exitPlayingFailure::atempt=" + context.getAttempt());

        // Deregister player listener
        this.player.removeListener(this.playerListener);

        // Deactivate player
        this.player.deactivate();
    }

    @Override
    public void enterFailed(PlayCollectState from, PlayCollectState to, Object event, PlayCollectContext context) {
        final OperationFailed operationFailed = new OperationFailed(PlayCollect.SYMBOL, context.getReturnCode());
        this.mgcpEventSubject.notify(this.mgcpEventSubject, operationFailed);
    }

    /**
     * Timer that defines interval the system will wait for user's input. May interrupt Collect process.
     * 
     * @author Henrique Rosa (henrique.rosa@telestax.com)
     *
     */
    private final class DetectorTimer implements Runnable {

        private final long timestamp;
        private final PlayCollectContext context;

        public DetectorTimer(PlayCollectContext context) {
            this.timestamp = System.currentTimeMillis();
            this.context = context;
        }

        @Override
        public void run() {
            if (context.getLastCollectedDigitOn() <= this.timestamp) {
                fire(PlayCollectEvent.TIME_OUT, context);
            } else {
                if (log.isInfoEnabled()) {
                    log.info("Aborting timeout operation because a tone has been received in the meantime.");
                }
            }

        }

    }

}