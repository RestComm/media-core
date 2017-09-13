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

package org.restcomm.media.control.mgcp.pkg.au.asr;

import org.restcomm.media.asr.AsrEngine;
import org.restcomm.media.control.mgcp.pkg.MgcpEventSubject;
import org.restcomm.media.spi.dtmf.DtmfDetector;
import org.restcomm.media.spi.player.Player;
import org.squirrelframework.foundation.fsm.HistoryType;
import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.TransitionPriority;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * @author anikiforov
 */
public class AsrFsmBuilder {

    public static final AsrFsmBuilder INSTANCE = new AsrFsmBuilder();

    private final StateMachineBuilder<AsrFsm, AsrState, AsrEvent, AsrContext> builder;

    private AsrFsmBuilder() {
        // Finite State Machine
        this.builder = StateMachineBuilderFactory.<AsrFsm, AsrState, AsrEvent, AsrContext> create(AsrFsmImpl.class, AsrState.class, AsrEvent.class, AsrContext.class, DtmfDetector.class, Player.class, AsrEngine.class, MgcpEventSubject.class, ListeningScheduledExecutorService.class, AsrContext.class);

        this.builder.defineFinishEvent(AsrEvent.EVALUATE);
        this.builder.defineParallelStatesOn(AsrState.PLAY_COLLECT, AsrState.PLAY, AsrState.COLLECT);
        this.builder.defineSequentialStatesOn(AsrState.PLAY, HistoryType.NONE, AsrState.LOADING_PLAYLIST, AsrState.PROMPTING, AsrState.PROMPTED);
        this.builder.defineSequentialStatesOn(AsrState.COLLECT, AsrState.COLLECTING, AsrState.WAITING_FOR_RESPONSE, AsrState.COLLECTED);

        this.builder.onEntry(AsrState.PLAY_COLLECT).callMethod("enterPlayCollect");

        this.builder.transition().from(AsrState.PLAY_COLLECT).to(AsrState.EVALUATING).on(AsrEvent.EVALUATE);
        this.builder.transition().from(AsrState.PLAY_COLLECT).to(AsrState.EVALUATING).on(AsrEvent.TIMEOUT);
        this.builder.transition(TransitionPriority.HIGHEST).from(AsrState.PLAY_COLLECT).to(AsrState.CANCELED).on(AsrEvent.CANCEL);
        this.builder.transition(TransitionPriority.HIGHEST).from(AsrState.PLAY_COLLECT).toFinal(AsrState.FAILED).on(AsrEvent.DRIVER_ERROR);
        this.builder.onExit(AsrState.PLAY_COLLECT).callMethod("exitPlayCollect");

        this.builder.onEntry(AsrState.LOADING_PLAYLIST).callMethod("enterLoadingPlaylist");
        this.builder.transition().from(AsrState.LOADING_PLAYLIST).to(AsrState.PROMPTING).on(AsrEvent.PROMPT);
        this.builder.transition().from(AsrState.LOADING_PLAYLIST).toFinal(AsrState.PROMPTED).on(AsrEvent.NO_PROMPT);
        this.builder.onExit(AsrState.LOADING_PLAYLIST).callMethod("exitLoadingPlaylist");

        this.builder.onEntry(AsrState.PROMPTING).callMethod("enterPrompting");
        this.builder.internalTransition().within(AsrState.PROMPTING).on(AsrEvent.NEXT_TRACK).callMethod("onPrompting");
        this.builder.transition().from(AsrState.PROMPTING).toFinal(AsrState.PROMPTED).on(AsrEvent.END_PROMPT);
        this.builder.transition().from(AsrState.PROMPTING).toFinal(AsrState.PROMPTED).on(AsrEvent.END_INPUT);
        this.builder.transition().from(AsrState.PROMPTING).toFinal(AsrState.PROMPTED).on(AsrEvent.END_INPUT_WITHOUT_WAITING_FOR_RESPONSE);
        this.builder.onExit(AsrState.PROMPTING).callMethod("exitPrompting");

        this.builder.onEntry(AsrState.PROMPTED).callMethod("enterPrompted");

        this.builder.onEntry(AsrState.COLLECTING).callMethod("enterCollecting");
        this.builder.internalTransition().within(AsrState.COLLECTING).on(AsrEvent.DTMF_TONE).callMethod("onCollecting");
        this.builder.internalTransition().within(AsrState.COLLECTING).on(AsrEvent.RECOGNIZED_TEXT).callMethod("onTextRecognized");
        this.builder.localTransition().from(AsrState.COLLECTING).to(AsrState.WAITING_FOR_RESPONSE).on(AsrEvent.END_INPUT);
        this.builder.localTransition().from(AsrState.COLLECTING).to(AsrState.COLLECTED).on(AsrEvent.END_INPUT_WITHOUT_WAITING_FOR_RESPONSE);
        this.builder.onExit(AsrState.COLLECTING).callMethod("exitCollecting");

        this.builder.onEntry(AsrState.WAITING_FOR_RESPONSE).callMethod("enterWaitingForResponse");
        this.builder.internalTransition().within(AsrState.WAITING_FOR_RESPONSE).on(AsrEvent.RECOGNIZED_TEXT).callMethod("onTextRecognized");
        this.builder.localTransition().from(AsrState.WAITING_FOR_RESPONSE).toFinal(AsrState.COLLECTED).on(AsrEvent.WAITING_FOR_RESPONSE_TIMEOUT);
        this.builder.onExit(AsrState.WAITING_FOR_RESPONSE).callMethod("exitWaitingForResponse");

        this.builder.onEntry(AsrState.EVALUATING).callMethod("enterEvaluating");
        this.builder.transition().from(AsrState.EVALUATING).to(AsrState.SUCCEEDING).on(AsrEvent.SUCCEED);
        this.builder.transition().from(AsrState.EVALUATING).to(AsrState.FAILING).on(AsrEvent.NO_RECOGNIZED_TEXT);
        this.builder.transition().from(AsrState.EVALUATING).to(AsrState.FAILING).on(AsrEvent.PATTERN_MISMATCH);
        this.builder.transition(TransitionPriority.HIGHEST).from(AsrState.EVALUATING).to(AsrState.CANCELED).on(AsrEvent.CANCEL);
        this.builder.onExit(AsrState.EVALUATING).callMethod("exitEvaluating");

        this.builder.onEntry(AsrState.CANCELED).callMethod("enterCanceled");
        this.builder.transition().from(AsrState.CANCELED).to(AsrState.SUCCEEDED).on(AsrEvent.SUCCEED);
        this.builder.transition().from(AsrState.CANCELED).to(AsrState.FAILED).on(AsrEvent.FAIL);
        this.builder.onExit(AsrState.CANCELED).callMethod("exitCanceled");

        this.builder.onEntry(AsrState.FAILING).callMethod("enterFailing");
        this.builder.transition().from(AsrState.FAILING).to(AsrState.PLAYING_FAILURE).on(AsrEvent.PROMPT);
        this.builder.transition().from(AsrState.FAILING).toFinal(AsrState.FAILED).on(AsrEvent.NO_PROMPT);
        this.builder.transition(TransitionPriority.HIGHEST).from(AsrState.FAILING).toFinal(AsrState.FAILED).on(AsrEvent.CANCEL);
        this.builder.onExit(AsrState.FAILING).callMethod("exitFailing");

        this.builder.onEntry(AsrState.PLAYING_FAILURE).callMethod("enterPlayingFailure");
        this.builder.internalTransition().within(AsrState.PLAYING_FAILURE).on(AsrEvent.NEXT_TRACK).callMethod("onPlayingFailure");
        this.builder.transition().from(AsrState.PLAYING_FAILURE).toFinal(AsrState.FAILED).on(AsrEvent.END_PROMPT);
        this.builder.transition(TransitionPriority.HIGHEST).from(AsrState.PLAYING_FAILURE).toFinal(AsrState.FAILED).on(AsrEvent.CANCEL);
        this.builder.onExit(AsrState.PLAYING_FAILURE).callMethod("exitPlayingFailure");

        this.builder.onEntry(AsrState.SUCCEEDING).callMethod("enterSucceeding");
        this.builder.transition().from(AsrState.SUCCEEDING).to(AsrState.PLAYING_SUCCESS).on(AsrEvent.PROMPT);
        this.builder.transition().from(AsrState.SUCCEEDING).toFinal(AsrState.SUCCEEDED).on(AsrEvent.NO_PROMPT);
        this.builder.transition(TransitionPriority.HIGHEST).from(AsrState.SUCCEEDING).toFinal(AsrState.SUCCEEDED).on(AsrEvent.CANCEL);
        this.builder.onExit(AsrState.SUCCEEDING).callMethod("exitSucceeding");

        this.builder.onEntry(AsrState.PLAYING_SUCCESS).callMethod("enterPlayingSuccess");
        this.builder.internalTransition().within(AsrState.PLAYING_SUCCESS).on(AsrEvent.NEXT_TRACK).callMethod("onPlayingSuccess");
        this.builder.transition().from(AsrState.PLAYING_SUCCESS).toFinal(AsrState.SUCCEEDED).on(AsrEvent.END_PROMPT);
        this.builder.transition(TransitionPriority.HIGHEST).from(AsrState.PLAYING_SUCCESS).toFinal(AsrState.SUCCEEDED).on(AsrEvent.CANCEL);
        this.builder.onExit(AsrState.PLAYING_SUCCESS).callMethod("exitPlayingSuccess");

        this.builder.onEntry(AsrState.SUCCEEDED).callMethod("enterSucceeded");
        this.builder.onEntry(AsrState.FAILED).callMethod("enterFailed");
    }

    public AsrFsm build(DtmfDetector detector, Player player, AsrEngine asrEngine, MgcpEventSubject eventSubject, ListeningScheduledExecutorService scheduler, AsrContext context) {
        return builder.newStateMachine(AsrState.PLAY_COLLECT, detector, player, asrEngine, eventSubject, scheduler, context);
    }

}
