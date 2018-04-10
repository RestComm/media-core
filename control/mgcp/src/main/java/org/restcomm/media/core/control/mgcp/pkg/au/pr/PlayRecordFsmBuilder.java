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

package org.restcomm.media.core.control.mgcp.pkg.au.pr;

import org.restcomm.media.core.control.mgcp.pkg.MgcpEventSubject;
import org.restcomm.media.core.resource.dtmf.DtmfSinkFacade;
import org.restcomm.media.core.resource.dtmf.DtmfEventObserver;
import org.restcomm.media.core.spi.player.Player;
import org.restcomm.media.core.spi.player.PlayerListener;
import org.restcomm.media.core.spi.recorder.Recorder;
import org.restcomm.media.core.spi.recorder.RecorderListener;
import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.StateMachineConfiguration;
import org.squirrelframework.foundation.fsm.TransitionPriority;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayRecordFsmBuilder {

    static final PlayRecordFsmBuilder INSTANCE = new PlayRecordFsmBuilder();

    private final StateMachineBuilder<PlayRecordFsm, PlayRecordState, PlayRecordEvent, PlayRecordContext> builder;

    private PlayRecordFsmBuilder() {
        this.builder = StateMachineBuilderFactory.<PlayRecordFsm, PlayRecordState, PlayRecordEvent, PlayRecordContext> create(
                PlayRecordFsmImpl.class, PlayRecordState.class, PlayRecordEvent.class, PlayRecordContext.class,
                MgcpEventSubject.class, Recorder.class, RecorderListener.class, DtmfSinkFacade.class, DtmfEventObserver.class,
                Player.class, PlayerListener.class, PlayRecordContext.class);

        this.builder.defineFinishEvent(PlayRecordEvent.RECORD_SUCCESS);
        
        this.builder.onEntry(PlayRecordState.LOADING_PLAYLIST).perform(LoadPlaylistAction.INSTANCE);
        this.builder.transition().from(PlayRecordState.LOADING_PLAYLIST).to(PlayRecordState.PROMPTING).on(PlayRecordEvent.PROMPT);
        this.builder.transition().from(PlayRecordState.LOADING_PLAYLIST).to(PlayRecordState.REPROMPTING).on(PlayRecordEvent.REPROMPT);
        this.builder.transition().from(PlayRecordState.LOADING_PLAYLIST).to(PlayRecordState.NO_SPEECH_PROMPTING).on(PlayRecordEvent.NO_SPEECH);
        this.builder.transition().from(PlayRecordState.LOADING_PLAYLIST).to(PlayRecordState.COLLECT_RECORD).on(PlayRecordEvent.NO_PROMPT);
        this.builder.transition(TransitionPriority.HIGHEST).from(PlayRecordState.LOADING_PLAYLIST).to(PlayRecordState.CANCELED).on(PlayRecordEvent.CANCEL);
        this.builder.onExit(PlayRecordState.LOADING_PLAYLIST).callMethod("exitLoadingPlaylist");

        this.builder.onEntry(PlayRecordState.PROMPTING).callMethod("enterPrompting");
        this.builder.internalTransition().within(PlayRecordState.PROMPTING).on(PlayRecordEvent.NEXT_TRACK).callMethod("onPrompting");
        this.builder.transition().from(PlayRecordState.PROMPTING).to(PlayRecordState.COLLECT_RECORD).on(PlayRecordEvent.PROMPT_END);
        this.builder.transition(TransitionPriority.HIGHEST).from(PlayRecordState.PROMPTING).to(PlayRecordState.CANCELED).on(PlayRecordEvent.CANCEL);
        this.builder.onExit(PlayRecordState.PROMPTING).callMethod("exitPrompting");

        this.builder.onEntry(PlayRecordState.REPROMPTING).callMethod("enterReprompting");
        this.builder.internalTransition().within(PlayRecordState.REPROMPTING).on(PlayRecordEvent.NEXT_TRACK).callMethod("onReprompting");
        this.builder.transition().from(PlayRecordState.REPROMPTING).to(PlayRecordState.COLLECT_RECORD).on(PlayRecordEvent.PROMPT_END);
        this.builder.transition(TransitionPriority.HIGHEST).from(PlayRecordState.REPROMPTING).to(PlayRecordState.CANCELED).on(PlayRecordEvent.CANCEL);
        this.builder.onExit(PlayRecordState.REPROMPTING).callMethod("exitReprompting");

        this.builder.onEntry(PlayRecordState.NO_SPEECH_PROMPTING).callMethod("enterNoSpeechReprompting");
        this.builder.internalTransition().within(PlayRecordState.NO_SPEECH_PROMPTING).on(PlayRecordEvent.NEXT_TRACK).callMethod("onNoSpeechReprompting");
        this.builder.transition().from(PlayRecordState.NO_SPEECH_PROMPTING).to(PlayRecordState.COLLECT_RECORD).on(PlayRecordEvent.PROMPT_END);
        this.builder.transition(TransitionPriority.HIGHEST).from(PlayRecordState.NO_SPEECH_PROMPTING).to(PlayRecordState.CANCELED).on(PlayRecordEvent.CANCEL);
        this.builder.onExit(PlayRecordState.NO_SPEECH_PROMPTING).callMethod("exitNoSpeechReprompting");

        this.builder.defineParallelStatesOn(PlayRecordState.COLLECT_RECORD, PlayRecordState.RECORD, PlayRecordState.COLLECT);
        this.builder.transition().from(PlayRecordState.COLLECT_RECORD).to(PlayRecordState.SUCCEEDING).on(PlayRecordEvent.RECORD_SUCCESS);
        this.builder.transition().from(PlayRecordState.COLLECT_RECORD).to(PlayRecordState.FAILING).on(PlayRecordEvent.REINPUT);
        this.builder.transition().from(PlayRecordState.COLLECT_RECORD).to(PlayRecordState.FAILING).on(PlayRecordEvent.RESTART);
        this.builder.transition().from(PlayRecordState.COLLECT_RECORD).to(PlayRecordState.FAILING).on(PlayRecordEvent.MAX_DURATION_EXCEEDED);
        this.builder.transition().from(PlayRecordState.COLLECT_RECORD).to(PlayRecordState.FAILING).on(PlayRecordEvent.NO_SPEECH);
        this.builder.transition(TransitionPriority.HIGHEST).from(PlayRecordState.COLLECT_RECORD).to(PlayRecordState.CANCELED).on(PlayRecordEvent.CANCEL);

        this.builder.defineSequentialStatesOn(PlayRecordState.COLLECT, PlayRecordState.COLLECTING, PlayRecordState.COLLECTED);
        this.builder.onEntry(PlayRecordState.COLLECTING).callMethod("enterCollecting");
        this.builder.internalTransition().within(PlayRecordState.COLLECTING).on(PlayRecordEvent.DTMF_TONE).callMethod("onCollecting");
        this.builder.transition().from(PlayRecordState.COLLECTING).toFinal(PlayRecordState.COLLECTED).on(PlayRecordEvent.END_COLLECT);
        this.builder.onExit(PlayRecordState.COLLECTING).callMethod("exitCollecting");
        this.builder.onEntry(PlayRecordState.COLLECTED).callMethod("enterCollected");

        this.builder.defineSequentialStatesOn(PlayRecordState.RECORD, PlayRecordState.RECORDING, PlayRecordState.RECORDED);
        this.builder.onEntry(PlayRecordState.RECORDING).callMethod("enterRecording");
        this.builder.transition().from(PlayRecordState.RECORDING).toFinal(PlayRecordState.RECORDED).on(PlayRecordEvent.END_RECORD);
        this.builder.transition().from(PlayRecordState.COLLECTING).toFinal(PlayRecordState.COLLECTED).on(PlayRecordEvent.END_RECORD);
        this.builder.onExit(PlayRecordState.RECORDING).callMethod("exitRecording");

        this.builder.onEntry(PlayRecordState.RECORDED).callMethod("enterRecorded");
        
        this.builder.onEntry(PlayRecordState.CANCELED).callMethod("enterCanceled");
        this.builder.transition().from(PlayRecordState.CANCELED).toFinal(PlayRecordState.SUCCEEDED).on(PlayRecordEvent.SUCCEED);
        this.builder.transition().from(PlayRecordState.CANCELED).toFinal(PlayRecordState.FAILED).on(PlayRecordEvent.FAIL);
        this.builder.onExit(PlayRecordState.CANCELED).callMethod("exitCanceled");
        
        this.builder.onEntry(PlayRecordState.SUCCEEDING).callMethod("enterSucceeding");
        this.builder.transition().from(PlayRecordState.SUCCEEDING).to(PlayRecordState.PLAYING_SUCCESS).on(PlayRecordEvent.PROMPT);
        this.builder.transition().from(PlayRecordState.SUCCEEDING).toFinal(PlayRecordState.SUCCEEDED).on(PlayRecordEvent.NO_PROMPT);
        this.builder.transition(TransitionPriority.HIGHEST).from(PlayRecordState.SUCCEEDING).toFinal(PlayRecordState.SUCCEEDED).on(PlayRecordEvent.CANCEL);
        this.builder.onExit(PlayRecordState.SUCCEEDING).callMethod("exitSucceeding");
        
        this.builder.onEntry(PlayRecordState.SUCCEEDED).callMethod("enterSucceeded");

        this.builder.onEntry(PlayRecordState.PLAYING_SUCCESS).callMethod("enterPlayingSuccess");
        this.builder.internalTransition().within(PlayRecordState.PLAYING_SUCCESS).on(PlayRecordEvent.NEXT_TRACK).callMethod("onPlayingSuccess");
        this.builder.transition().from(PlayRecordState.PLAYING_SUCCESS).toFinal(PlayRecordState.SUCCEEDED).on(PlayRecordEvent.PROMPT_END);
        this.builder.transition(TransitionPriority.HIGHEST).from(PlayRecordState.PLAYING_SUCCESS).toFinal(PlayRecordState.SUCCEEDED).on(PlayRecordEvent.CANCEL);
        this.builder.onExit(PlayRecordState.PLAYING_SUCCESS).callMethod("exitPlayingSuccess");

        this.builder.onEntry(PlayRecordState.FAILING).callMethod("enterFailing");
        this.builder.transition().from(PlayRecordState.FAILING).to(PlayRecordState.PLAYING_FAILURE).on(PlayRecordEvent.PROMPT);
        this.builder.transition().from(PlayRecordState.FAILING).toFinal(PlayRecordState.FAILED).on(PlayRecordEvent.NO_PROMPT);
        this.builder.transition().from(PlayRecordState.FAILING).to(PlayRecordState.LOADING_PLAYLIST).on(PlayRecordEvent.REINPUT);
        this.builder.transition().from(PlayRecordState.FAILING).to(PlayRecordState.LOADING_PLAYLIST).on(PlayRecordEvent.RESTART);
        this.builder.transition().from(PlayRecordState.FAILING).to(PlayRecordState.LOADING_PLAYLIST).on(PlayRecordEvent.NO_SPEECH);
        this.builder.transition().from(PlayRecordState.FAILING).to(PlayRecordState.LOADING_PLAYLIST).on(PlayRecordEvent.MAX_DURATION_EXCEEDED);
        this.builder.transition(TransitionPriority.HIGHEST).from(PlayRecordState.FAILING).toFinal(PlayRecordState.FAILED).on(PlayRecordEvent.CANCEL);
        this.builder.onExit(PlayRecordState.FAILING).callMethod("exitFailing");

        this.builder.onEntry(PlayRecordState.PLAYING_FAILURE).callMethod("enterPlayingFailure");
        this.builder.internalTransition().within(PlayRecordState.PLAYING_FAILURE).on(PlayRecordEvent.NEXT_TRACK).callMethod("onPlayingFailure");
        this.builder.transition().from(PlayRecordState.PLAYING_FAILURE).toFinal(PlayRecordState.FAILED).on(PlayRecordEvent.PROMPT_END);
        this.builder.transition(TransitionPriority.HIGHEST).from(PlayRecordState.PLAYING_FAILURE).toFinal(PlayRecordState.FAILED).on(PlayRecordEvent.CANCEL);
        this.builder.onExit(PlayRecordState.PLAYING_FAILURE).callMethod("exitPlayingFailure");

        this.builder.onEntry(PlayRecordState.FAILED).callMethod("enterFailed");
    }

    public PlayRecordFsm build(MgcpEventSubject mgcpEventSubject, Recorder recorder, RecorderListener recorderListener,
            DtmfSinkFacade detector, DtmfEventObserver detectorObserver, Player player, PlayerListener playerListener,
            PlayRecordContext context) {
        return this.builder.newStateMachine(PlayRecordState.LOADING_PLAYLIST,
                StateMachineConfiguration.getInstance().enableDebugMode(false), mgcpEventSubject, recorder, recorderListener,
                detector, detectorObserver, player, playerListener, context);
    }

}
