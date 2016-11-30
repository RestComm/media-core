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

package org.mobicents.media.control.mgcp.pkg.au.pc2;

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
public class PlayCollectFsmImpl extends
        AbstractStateMachine<PlayCollectFsm, PlayCollectState, PlayCollectEvent, PlayCollectContext> implements PlayCollectFsm {

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
        } catch (MalformedURLException e) {
            log.warn("Could not play malformed segment " + url);
            context.setReturnCode(ReturnCode.BAD_AUDIO_ID.code());
            fire(PlayCollectEvent.FAIL, context);
            // TODO create transition from PROMPTING to FAILED
        } catch (ResourceUnavailableException e) {
            log.warn("Could not play unavailable segment " + url);
            context.setReturnCode(ReturnCode.BAD_AUDIO_ID.code());
            fire(PlayCollectEvent.FAIL, context);
            // TODO create transition from PROMPTING to FAILED
        }
    }
    
    @Override
    public void enterPlayCollect(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered PLAY_COLLECT state");
        }
        
    }
    
    @Override
    public void exitPlayCollect(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PLAY_COLLECT state");
        }
        
    }

    @Override
    public void enterLoadingPlaylist(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered LOADING PLAYLIST state");
        }

        final Playlist prompt = context.getInitialPrompt();
        if (prompt.isEmpty()) {
            fire(PlayCollectEvent.NO_PROMPT, context);
        } else {
            fire(PlayCollectEvent.PROMPT, context);
        }
    }

    @Override
    public void exitLoadingPlaylist(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited LOADING PLAYLIST state");
        }
    }

    @Override
    public void enterPrompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
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
            fire(PlayCollectEvent.FAIL, context);
        }
    }

    @Override
    public void onPrompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On PROMPTING state");
        }

        final Playlist prompt = context.getInitialPrompt();
        final String track = prompt.next();

        if (track.isEmpty()) {
            // No more announcements to play
            fire(PlayCollectEvent.END_PROMPT, context);
        } else {
            playAnnouncement(track, 10 * 100);
        }
    }

    @Override
    public void exitPrompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PROMPTING state");
        }

        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }

    @Override
    public void enterReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered REPROMPTING state");
        }

        final Playlist prompt = context.getReprompt();
        final String track = prompt.next();
        try {
            this.player.addListener(this.playerListener);
            playAnnouncement(track, 0L);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(PlayCollectEvent.FAIL, context);
        }
    }

    @Override
    public void onReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On REPROMPTING state");
        }

        final Playlist prompt = context.getReprompt();
        final String track = prompt.next();

        if (track.isEmpty()) {
            // No more announcements to play
            fire(PlayCollectEvent.END_PROMPT, context);
        } else {
            playAnnouncement(track, 10 * 100);
        }
    }

    @Override
    public void exitReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited REPROMPTING state");
        }

        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }

    @Override
    public void enterNoDigitsReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered NO DIGITS REPROMPTING state");
        }

        final Playlist prompt = context.getNoDigitsReprompt();
        final String track = prompt.next();
        try {
            this.player.addListener(this.playerListener);
            playAnnouncement(track, 0L);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(PlayCollectEvent.FAIL, context);
        }
    }

    @Override
    public void onNoDigitsReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On NO DIGITS REPROMPTING state");
        }

        final Playlist prompt = context.getNoDigitsReprompt();
        final String track = prompt.next();

        if (track.isEmpty()) {
            // No more announcements to play
            fire(PlayCollectEvent.END_PROMPT, context);
        } else {
            playAnnouncement(track, 10 * 100);
        }
    }

    @Override
    public void exitNoDigitsReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited NO DIGITS REPROMPTING state");
        }

        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }

    @Override
    public void enterCollecting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered COLLECTING state");
        }

        try {
            // Activate DTMF detector and bind listener
            this.detector.addListener(this.detectorListener);
            this.detector.activate();

            // Activate timer for first digit
            this.executor.schedule(new DetectorTimer(context), context.getFirstDigitTimer(), TimeUnit.MILLISECONDS);
        } catch (TooManyListenersException e) {
            log.error("Too many DTMF listeners", e);
        }
    }

    @Override
    public void onCollecting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On COLLECTING state");
        }

        // Stop current prompt IF is interruptible
        if (!context.getNonInterruptibleAudio()) {
            // TODO check if child state PLAYING is currently active
            fire(PlayCollectEvent.END_PROMPT, context);
        }

        final char tone = context.getLastTone();

        if (context.getReinputKey() == tone) {
            // Force collection to cancel any scheduled timeout
            context.collectDigit(tone);

            fire(PlayCollectEvent.REINPUT, context);
        } else if (context.getRestartKey() == tone) {
            // Force collection to cancel any scheduled timeout
            context.collectDigit(tone);

            fire(PlayCollectEvent.RESTART, context);
        } else if (context.getEndInputKey() == tone) {
            fire(PlayCollectEvent.END_INPUT);
        } else {
            // Collect tone
            context.collectDigit(tone);
            
            // Stop digit collection if maximum number of digits was reached
            if(context.countCollectedDigits() == context.getMaximumDigits()) {
                if(log.isTraceEnabled()) {
                    log.trace("Maximum numbers of digits. Stopping collecting operation.");
                }
                fire(PlayCollectEvent.END_INPUT, context);
            }
        }

        // TODO Implement onCollecting
    }

    @Override
    public void exitCollecting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited COLLECTING state");
        }

        this.detector.removeListener(this.detectorListener);
        this.detector.deactivate();
    }

    @Override
    public void enterEvaluating(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered EVALUATING state");
        }

        // TODO implement EVALUATING state
    }

    @Override
    public void exitEvaluating(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited EVALUATING state");
        }
    }

    @Override
    public void enterCanceled(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered CANCELED state");
        }

        // TODO implement CANCELED state
    }

    @Override
    public void exitCanceled(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited CANCELED state");
        }
    }

    @Override
    public void enterPlayingSuccess(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
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
            fire(PlayCollectEvent.FAIL, context);
        }
    }

    @Override
    public void onPlayingSuccess(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On PLAYING SUCCESS state");
        }

        final Playlist prompt = context.getSuccessAnnouncement();
        final String track = prompt.next();

        if (track.isEmpty()) {
            // No more announcements to play
            fire(PlayCollectEvent.END_PROMPT, context);
        } else {
            playAnnouncement(track, 10 * 100);
        }
    }

    @Override
    public void exitPlayingSuccess(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PLAYING SUCCESS state");
        }

        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }

    @Override
    public void enterSucceeded(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered SUCCEEDED state");
        }

        final String collectedDigits = context.getCollectedDigits();
        final int attempt = context.getAttempt();

        final OperationComplete operationComplete = new OperationComplete(PlayCollect.SYMBOL, ReturnCode.SUCCESS.code());
        operationComplete.setParameter("na", String.valueOf(attempt));
        operationComplete.setParameter("dc", collectedDigits);
        this.mgcpEventSubject.notify(this.mgcpEventSubject, operationComplete);
    }

    @Override
    public void enterPlayingFailure(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
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
            fire(PlayCollectEvent.FAIL, context);
        }
    }

    @Override
    public void onPlayingFailure(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On PLAYING FAILURE state");
        }

        final Playlist prompt = context.getFailureAnnouncement();
        final String track = prompt.next();

        if (track.isEmpty()) {
            // No more announcements to play
            fire(PlayCollectEvent.END_PROMPT, context);
        } else {
            playAnnouncement(track, 10 * 100);
        }
    }

    @Override
    public void exitPlayingFailure(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PLAYING FAILURE state");
        }

        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }

    @Override
    public void enterFailed(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered FAILED state");
        }

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
                if (PlayCollectState.PLAY_COLLECT.equals(getCurrentState())) {
                    if (log.isDebugEnabled()) {
                        log.debug("Timing out collect operation!");
                    }
                    fire(PlayCollectEvent.TIMEOUT, context);
                }
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Aborting timeout operation because a tone has been received in the meantime.");
                }
            }

        }

    }

}