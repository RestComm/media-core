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

package org.restcomm.media.core.control.mgcp.pkg.au.pc;

import org.restcomm.media.core.control.mgcp.pkg.MgcpEventSubject;
import org.restcomm.media.core.resource.dtmf.detector.DtmfEventSubject;
import org.restcomm.media.core.resource.dtmf.detector.DtmfEventObserver;
import org.restcomm.media.core.spi.player.Player;
import org.restcomm.media.core.spi.player.PlayerListener;
import org.squirrelframework.foundation.fsm.HistoryType;
import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.StateMachineConfiguration;
import org.squirrelframework.foundation.fsm.TransitionPriority;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayCollectFsmBuilder {

    public static final PlayCollectFsmBuilder INSTANCE = new PlayCollectFsmBuilder();

    private final StateMachineBuilder<PlayCollectFsm, PlayCollectState, PlayCollectEvent, PlayCollectContext> builder;

    private PlayCollectFsmBuilder() {
        // Finite State Machine
        this.builder = StateMachineBuilderFactory
                .<PlayCollectFsm, PlayCollectState, PlayCollectEvent, PlayCollectContext> create(PlayCollectFsmImpl.class,
                        PlayCollectState.class, PlayCollectEvent.class, PlayCollectContext.class, DtmfEventSubject.class,
                        DtmfEventObserver.class, Player.class, PlayerListener.class, MgcpEventSubject.class,
                        ListeningScheduledExecutorService.class, PlayCollectContext.class);

        this.builder.defineFinishEvent(PlayCollectEvent.EVALUATE);

        this.builder.onEntry(PlayCollectState.PLAY_COLLECT).callMethod("enterPlayCollect");
        this.builder.defineParallelStatesOn(PlayCollectState.PLAY_COLLECT, PlayCollectState.PLAY, PlayCollectState.COLLECT);
        this.builder.defineSequentialStatesOn(PlayCollectState.PLAY, HistoryType.NONE, PlayCollectState.LOADING_PLAYLIST, PlayCollectState.PROMPTING, PlayCollectState.REPROMPTING, PlayCollectState.NO_DIGITS_REPROMPTING, PlayCollectState.PROMPTED);
        this.builder.defineSequentialStatesOn(PlayCollectState.COLLECT, PlayCollectState.COLLECTING, PlayCollectState.COLLECTED);
        this.builder.transition().from(PlayCollectState.PLAY_COLLECT).to(PlayCollectState.EVALUATING).on(PlayCollectEvent.EVALUATE);
        this.builder.transition().from(PlayCollectState.PLAY_COLLECT).to(PlayCollectState.EVALUATING).on(PlayCollectEvent.TIMEOUT);
        this.builder.transition().from(PlayCollectState.PLAY_COLLECT).to(PlayCollectState.FAILING).on(PlayCollectEvent.RESTART);
        this.builder.transition().from(PlayCollectState.PLAY_COLLECT).to(PlayCollectState.FAILING).on(PlayCollectEvent.REINPUT);
        this.builder.transition(TransitionPriority.HIGHEST).from(PlayCollectState.PLAY_COLLECT).to(PlayCollectState.CANCELED).on(PlayCollectEvent.CANCEL);
        this.builder.onExit(PlayCollectState.PLAY_COLLECT).callMethod("exitPlayCollect");

        this.builder.onEntry(PlayCollectState.LOADING_PLAYLIST).callMethod("enterLoadingPlaylist");
        this.builder.transition().from(PlayCollectState.LOADING_PLAYLIST).to(PlayCollectState.PROMPTING).on(PlayCollectEvent.PROMPT);
        this.builder.transition().from(PlayCollectState.LOADING_PLAYLIST).to(PlayCollectState.REPROMPTING).on(PlayCollectEvent.REPROMPT);
        this.builder.transition().from(PlayCollectState.LOADING_PLAYLIST).to(PlayCollectState.NO_DIGITS_REPROMPTING).on(PlayCollectEvent.NO_DIGITS);
        this.builder.transition().from(PlayCollectState.LOADING_PLAYLIST).toFinal(PlayCollectState.PROMPTED).on(PlayCollectEvent.NO_PROMPT);
        this.builder.onExit(PlayCollectState.LOADING_PLAYLIST).callMethod("exitLoadingPlaylist");
        
        this.builder.onEntry(PlayCollectState.PROMPTING).callMethod("enterPrompting");
        this.builder.internalTransition().within(PlayCollectState.PROMPTING).on(PlayCollectEvent.NEXT_TRACK).callMethod("onPrompting");
        this.builder.transition().from(PlayCollectState.PROMPTING).toFinal(PlayCollectState.PROMPTED).on(PlayCollectEvent.END_PROMPT);
        this.builder.transition().from(PlayCollectState.PROMPTING).toFinal(PlayCollectState.PROMPTED).on(PlayCollectEvent.END_INPUT);
        this.builder.onExit(PlayCollectState.PROMPTING).callMethod("exitPrompting");
        
        this.builder.onEntry(PlayCollectState.REPROMPTING).callMethod("enterReprompting");
        this.builder.internalTransition().within(PlayCollectState.REPROMPTING).on(PlayCollectEvent.NEXT_TRACK).callMethod("onReprompting");
        this.builder.transition().from(PlayCollectState.REPROMPTING).toFinal(PlayCollectState.PROMPTED).on(PlayCollectEvent.END_PROMPT);
        this.builder.transition().from(PlayCollectState.REPROMPTING).toFinal(PlayCollectState.PROMPTED).on(PlayCollectEvent.END_INPUT);
        this.builder.onExit(PlayCollectState.REPROMPTING).callMethod("exitReprompting");
        
        this.builder.onEntry(PlayCollectState.NO_DIGITS_REPROMPTING).callMethod("enterNoDigitsReprompting");
        this.builder.internalTransition().within(PlayCollectState.NO_DIGITS_REPROMPTING).on(PlayCollectEvent.NEXT_TRACK).callMethod("onNoDigitsReprompting");
        this.builder.transition().from(PlayCollectState.NO_DIGITS_REPROMPTING).toFinal(PlayCollectState.PROMPTED).on(PlayCollectEvent.END_PROMPT);
        this.builder.transition().from(PlayCollectState.NO_DIGITS_REPROMPTING).toFinal(PlayCollectState.PROMPTED).on(PlayCollectEvent.END_INPUT);
        this.builder.onExit(PlayCollectState.NO_DIGITS_REPROMPTING).callMethod("exitNoDigitsReprompting");
        
        this.builder.onEntry(PlayCollectState.PROMPTED).callMethod("enterPrompted");

        this.builder.onEntry(PlayCollectState.COLLECTING).callMethod("enterCollecting");
        this.builder.internalTransition().within(PlayCollectState.COLLECTING).on(PlayCollectEvent.DTMF_TONE).callMethod("onCollecting");
        this.builder.transition().from(PlayCollectState.COLLECTING).toFinal(PlayCollectState.COLLECTED).on(PlayCollectEvent.END_INPUT);
        this.builder.onExit(PlayCollectState.COLLECTING).callMethod("exitCollecting");
        
        this.builder.onEntry(PlayCollectState.EVALUATING).callMethod("enterEvaluating");
        this.builder.transition().from(PlayCollectState.EVALUATING).to(PlayCollectState.SUCCEEDING).on(PlayCollectEvent.SUCCEED);
        this.builder.transition().from(PlayCollectState.EVALUATING).to(PlayCollectState.FAILING).on(PlayCollectEvent.NO_DIGITS);
        this.builder.transition().from(PlayCollectState.EVALUATING).to(PlayCollectState.FAILING).on(PlayCollectEvent.PATTERN_MISMATCH);
        this.builder.transition(TransitionPriority.HIGHEST).from(PlayCollectState.EVALUATING).to(PlayCollectState.CANCELED).on(PlayCollectEvent.CANCEL);
        this.builder.onExit(PlayCollectState.EVALUATING).callMethod("exitEvaluating");
        
        this.builder.onEntry(PlayCollectState.CANCELED).callMethod("enterCanceled");
        this.builder.transition().from(PlayCollectState.CANCELED).to(PlayCollectState.SUCCEEDED).on(PlayCollectEvent.SUCCEED);
        this.builder.transition().from(PlayCollectState.CANCELED).to(PlayCollectState.FAILED).on(PlayCollectEvent.FAIL);
        this.builder.onExit(PlayCollectState.CANCELED).callMethod("exitCanceled");
        
        this.builder.onEntry(PlayCollectState.FAILING).callMethod("enterFailing");
        this.builder.transition().from(PlayCollectState.FAILING).to(PlayCollectState.PLAY_COLLECT).on(PlayCollectEvent.REINPUT);
        this.builder.transition().from(PlayCollectState.FAILING).to(PlayCollectState.PLAY_COLLECT).on(PlayCollectEvent.RESTART);
        this.builder.transition().from(PlayCollectState.FAILING).to(PlayCollectState.PLAY_COLLECT).on(PlayCollectEvent.NO_DIGITS);
        this.builder.transition().from(PlayCollectState.FAILING).to(PlayCollectState.PLAYING_FAILURE).on(PlayCollectEvent.PROMPT);
        this.builder.transition().from(PlayCollectState.FAILING).toFinal(PlayCollectState.FAILED).on(PlayCollectEvent.NO_PROMPT);
        this.builder.transition(TransitionPriority.HIGHEST).from(PlayCollectState.FAILING).toFinal(PlayCollectState.FAILED).on(PlayCollectEvent.CANCEL);
        this.builder.onExit(PlayCollectState.FAILING).callMethod("exitFailing");

        this.builder.onEntry(PlayCollectState.PLAYING_FAILURE).callMethod("enterPlayingFailure");
        this.builder.internalTransition().within(PlayCollectState.PLAYING_FAILURE).on(PlayCollectEvent.NEXT_TRACK).callMethod("onPlayingFailure");
        this.builder.transition().from(PlayCollectState.PLAYING_FAILURE).toFinal(PlayCollectState.FAILED).on(PlayCollectEvent.END_PROMPT);
        this.builder.transition(TransitionPriority.HIGHEST).from(PlayCollectState.PLAYING_FAILURE).toFinal(PlayCollectState.FAILED).on(PlayCollectEvent.CANCEL);
        this.builder.onExit(PlayCollectState.PLAYING_FAILURE).callMethod("exitPlayingFailure");

        this.builder.onEntry(PlayCollectState.SUCCEEDING).callMethod("enterSucceeding");
        this.builder.transition().from(PlayCollectState.SUCCEEDING).to(PlayCollectState.PLAYING_SUCCESS).on(PlayCollectEvent.PROMPT);
        this.builder.transition().from(PlayCollectState.SUCCEEDING).toFinal(PlayCollectState.SUCCEEDED).on(PlayCollectEvent.NO_PROMPT);
        this.builder.transition(TransitionPriority.HIGHEST).from(PlayCollectState.SUCCEEDING).toFinal(PlayCollectState.SUCCEEDED).on(PlayCollectEvent.CANCEL);
        this.builder.onExit(PlayCollectState.SUCCEEDING).callMethod("exitSucceeding");
        
        this.builder.onEntry(PlayCollectState.PLAYING_SUCCESS).callMethod("enterPlayingSuccess");
        this.builder.internalTransition().within(PlayCollectState.PLAYING_SUCCESS).on(PlayCollectEvent.NEXT_TRACK).callMethod("onPlayingSuccess");
        this.builder.transition().from(PlayCollectState.PLAYING_SUCCESS).toFinal(PlayCollectState.SUCCEEDED).on(PlayCollectEvent.END_PROMPT);
        this.builder.transition(TransitionPriority.HIGHEST).from(PlayCollectState.PLAYING_SUCCESS).toFinal(PlayCollectState.SUCCEEDED).on(PlayCollectEvent.CANCEL);
        this.builder.onExit(PlayCollectState.PLAYING_SUCCESS).callMethod("exitPlayingSuccess");

        this.builder.onEntry(PlayCollectState.SUCCEEDED).callMethod("enterSucceeded");
        this.builder.onEntry(PlayCollectState.FAILED).callMethod("enterFailed");
    }

    public PlayCollectFsm build(DtmfEventSubject detector, DtmfEventObserver detectorObserver, Player player,
            PlayerListener playerListener, MgcpEventSubject eventSubject, ListeningScheduledExecutorService scheduler,
            PlayCollectContext context) {
        return builder.newStateMachine(PlayCollectState.PLAY_COLLECT,
                StateMachineConfiguration.getInstance().enableDebugMode(false), detector, detectorObserver, player,
                playerListener, eventSubject, scheduler, context);
    }

}
