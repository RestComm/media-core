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

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.pkg.MgcpEventSubject;
import org.mobicents.media.control.mgcp.pkg.au.ReturnCode;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerListener;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayCollectFsmImpl extends AbstractStateMachine<PlayCollectFsm, PlayCollectState, PlayCollectEvent, PlayCollectContext> implements PlayCollectFsm {

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
    public void enterLoadingPlaylist(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered LOADING PLAYLIST state");
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

    }

    @Override
    public void onPrompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On PROMPTING state");
        }

    }

    @Override
    public void exitPrompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PROMPTING state");
        }

    }

    @Override
    public void enterReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered REPROMPTING state");
        }

    }

    @Override
    public void onReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On REPROMPTING state");
        }

    }

    @Override
    public void exitReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited REPROMPTING state");
        }

    }

    @Override
    public void enterNoDigitsReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered NO DIGITS REPROMPTING state");
        }

    }

    @Override
    public void onNoDigitsReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On NO DIGITS REPROMPTING state");
        }

    }

    @Override
    public void exitNoDigitsReprompting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited NO DIGITS REPROMPTING state");
        }

    }

    @Override
    public void enterCollecting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered COLLECTING state");
        }

    }

    @Override
    public void onCollecting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On COLLECTING state");
        }

    }

    @Override
    public void exitCollecting(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited COLLECTING state");
        }

    }

    @Override
    public void enterEvaluating(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered EVALUATING state");
        }

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

    }

    @Override
    public void onPlayingSuccess(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On PLAYING SUCCESS state");
        }

    }

    @Override
    public void exitPlayingSuccess(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PLAYING SUCCESS state");
        }

    }

    @Override
    public void enterSucceeded(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered SUCCEEDED state");
        }

    }

    @Override
    public void enterPlayingFailure(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered PLAYING FAILURE state");
        }

    }

    @Override
    public void onPlayingFailure(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On PLAYING FAILURE state");
        }

    }

    @Override
    public void exitPlayingFailure(PlayCollectState from, PlayCollectState to, PlayCollectEvent event,
            PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PLAYING FAILURE state");
        }

    }

    @Override
    public void enterFailed(PlayCollectState from, PlayCollectState to, PlayCollectEvent event, PlayCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered FAILED state");
        }

    }

}