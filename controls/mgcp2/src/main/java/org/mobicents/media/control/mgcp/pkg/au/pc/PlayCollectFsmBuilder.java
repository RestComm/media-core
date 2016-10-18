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

import org.mobicents.media.control.mgcp.pkg.MgcpEventSubject;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerListener;
import org.squirrelframework.foundation.fsm.HistoryType;
import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.StateMachineConfiguration;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayCollectFsmBuilder {
    
    public static final PlayCollectFsmBuilder INSTANCE = new PlayCollectFsmBuilder();

    private final StateMachineBuilder<PlayCollectFsm, PlayCollectState, Object, PlayCollectContext> builder;

    private PlayCollectFsmBuilder() {
        // Finite State Machine
        this.builder = StateMachineBuilderFactory.<PlayCollectFsm, PlayCollectState, Object, PlayCollectContext> create(
                PlayCollectFsmImpl2.class, PlayCollectState.class, Object.class, PlayCollectContext.class, DtmfDetector.class,
                DtmfDetectorListener.class, Player.class, PlayerListener.class, MgcpEventSubject.class,
                ListeningScheduledExecutorService.class, PlayCollectContext.class);

        // builder.onEntry(PlayCollectState.READY).callMethod("enterReady");
        // builder.transition().from(PlayCollectState.READY).to(PlayCollectState.PROCESSING).on(PlayCollectEvent.PROMPT);
        // builder.transition().from(PlayCollectState.READY).to(PlayCollectState.ACTIVE).on(PlayCollectEvent.REPROMPT);
        // builder.transition().from(PlayCollectState.READY).to(PlayCollectState.ACTIVE).on(PlayCollectEvent.NO_DIGITS_REPROMPT);
        // builder.onExit(PlayCollectState.READY).callMethod("exitReady");

        builder.defineFinishEvent(PlayCollectEvent.EVALUATE);
        builder.defineParallelStatesOn(PlayCollectState.PROCESSING, PlayCollectState.PROMPT, PlayCollectState.COLLECT);
        builder.transition().from(PlayCollectState.PROCESSING).to(PlayCollectState.EVALUATING).on(PlayCollectEvent.EVALUATE);
        builder.transition().from(PlayCollectState.PROCESSING).to(PlayCollectState.TIMED_OUT).on(PlayCollectEvent.TIMEOUT);

        builder.defineSequentialStatesOn(PlayCollectState.PROMPT, HistoryType.DEEP, PlayCollectState.PROMPTING, PlayCollectState.PROMPTED);
        builder.onEntry(PlayCollectState.PROMPTING).callMethod("enterPrompting");
        builder.internalTransition().within(PlayCollectState.PROMPTING).on(PlayCollectEvent.NEXT_TRACK).callMethod("onPrompting");
        builder.transition().from(PlayCollectState.PROMPTING).toFinal(PlayCollectState.PROMPTED).on(PlayCollectEvent.END_PROMPT);
        builder.onExit(PlayCollectState.PROMPTING).callMethod("exitPrompting");
        
        builder.defineSequentialStatesOn(PlayCollectState.COLLECT, HistoryType.DEEP, PlayCollectState.COLLECTING, PlayCollectState.COLLECTED);
        builder.onEntry(PlayCollectState.COLLECTING).callMethod("enterCollecting");
        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_0).callMethod("onCollecting");
        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_1).callMethod("onCollecting");
        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_2).callMethod("onCollecting");
        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_3).callMethod("onCollecting");
        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_4).callMethod("onCollecting");
        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_5).callMethod("onCollecting");
        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_6).callMethod("onCollecting");
        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_7).callMethod("onCollecting");
        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_8).callMethod("onCollecting");
        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_9).callMethod("onCollecting");
        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_A).callMethod("onCollecting");
        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_B).callMethod("onCollecting");
        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_C).callMethod("onCollecting");
        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_D).callMethod("onCollecting");
        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_HASH).callMethod("onCollecting");
        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_STAR).callMethod("onCollecting");
        builder.transition().from(PlayCollectState.COLLECTING).toFinal(PlayCollectState.COLLECTED).on(PlayCollectEvent.END_INPUT);
        builder.onExit(PlayCollectState.COLLECTING).callMethod("exitCollecting");
        
        builder.onEntry(PlayCollectState.EVALUATING).perform(EvaluateInputAction.INSTANCE);
        builder.transition().from(PlayCollectState.EVALUATING).toFinal(PlayCollectState.SUCCEEDED).on(PlayCollectEvent.SUCCEED);
        builder.transition().from(PlayCollectState.EVALUATING).toFinal(PlayCollectState.FAILED).on(PlayCollectEvent.FAIL);
        
        builder.onEntry(PlayCollectState.TIMED_OUT).perform(TimeoutAction.INSTANCE);
        builder.transition().from(PlayCollectState.TIMED_OUT).to(PlayCollectState.PLAYING_SUCCESS).on(PlayCollectEvent.PLAY_SUCCESS);
        builder.transition().from(PlayCollectState.TIMED_OUT).to(PlayCollectState.SUCCEEDED).on(PlayCollectEvent.SUCCEED);
        builder.transition().from(PlayCollectState.TIMED_OUT).to(PlayCollectState.PLAYING_FAILURE).on(PlayCollectEvent.PLAY_FAILURE);
        builder.transition().from(PlayCollectState.TIMED_OUT).to(PlayCollectState.FAILED).on(PlayCollectEvent.FAIL);
//      builder.transition().from(PlayCollectState.TIMED_OUT).to(PlayCollectState.REPROMPTING).on(PlayCollectEvent.REPROMPT);
//      builder.transition().from(PlayCollectState.TIMED_OUT).to(PlayCollectState.NO_DIGITS_REPROMPTING).on(PlayCollectEvent.NO_DIGITS_REPROMPT);
//      builder.transition(TransitionPriority.HIGHEST).from(PlayCollectState.TIMED_OUT).to(PlayCollectState.CANCELED).on(PlayCollectEvent.CANCEL).perform(CancelAction.INSTANCE);
//      builder.onExit(PlayCollectState.TIMED_OUT).callMethod("exitTimedOut");
        
        builder.onEntry(PlayCollectState.SUCCEEDED).callMethod("enterSucceeded");
        builder.defineState(PlayCollectState.SUCCEEDED).setFinal(true);

        builder.onEntry(PlayCollectState.FAILED).callMethod("enterFailed");
        builder.defineState(PlayCollectState.FAILED).setFinal(true);
        
//        builder.onEntry(PlayCollectState.READY).callMethod("enterReady");
//        builder.transition().from(PlayCollectState.READY).to(PlayCollectState.COLLECTING).on(PlayCollectEvent.COLLECT);
//        builder.transition().from(PlayCollectState.READY).to(PlayCollectState.PROMPTING).on(PlayCollectEvent.PROMPT);
//        builder.onExit(PlayCollectState.READY).callMethod("exitReady");
//
//        builder.onEntry(PlayCollectState.PROMPTING).callMethod("enterPrompting");
//        builder.internalTransition().within(PlayCollectState.PROMPTING).on(PlayCollectEvent.NEXT_TRACK).callMethod("onPrompting");
//        builder.transition().from(PlayCollectState.PROMPTING).to(PlayCollectState.COLLECTING).on(PlayCollectEvent.COLLECT);
//        builder.transition(TransitionPriority.HIGHEST).from(PlayCollectState.PROMPTING).to(PlayCollectState.CANCELED).on(PlayCollectEvent.CANCEL).perform(CancelAction.INSTANCE);
//        builder.onExit(PlayCollectState.PROMPTING).callMethod("exitPrompting");
//
//        builder.onEntry(PlayCollectState.COLLECTING).callMethod("enterCollecting");
//        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_0).callMethod("onCollecting");
//        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_1).callMethod("onCollecting");
//        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_2).callMethod("onCollecting");
//        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_3).callMethod("onCollecting");
//        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_4).callMethod("onCollecting");
//        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_5).callMethod("onCollecting");
//        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_6).callMethod("onCollecting");
//        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_7).callMethod("onCollecting");
//        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_8).callMethod("onCollecting");
//        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_9).callMethod("onCollecting");
//        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_A).callMethod("onCollecting");
//        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_B).callMethod("onCollecting");
//        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_C).callMethod("onCollecting");
//        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_D).callMethod("onCollecting");
//        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_HASH).callMethod("onCollecting");
//        builder.internalTransition().within(PlayCollectState.COLLECTING).on(DtmfToneEvent.DTMF_STAR).callMethod("onCollecting");
//        builder.transition().from(PlayCollectState.COLLECTING).to(PlayCollectState.PLAYING_SUCCESS).on(PlayCollectEvent.PLAY_SUCCESS);
//        builder.transition().from(PlayCollectState.COLLECTING).to(PlayCollectState.SUCCEEDED).on(PlayCollectEvent.SUCCEED);
//        builder.transition().from(PlayCollectState.COLLECTING).to(PlayCollectState.PLAYING_FAILURE).on(PlayCollectEvent.PLAY_FAILURE);
//        builder.transition().from(PlayCollectState.COLLECTING).to(PlayCollectState.FAILED).on(PlayCollectEvent.FAIL);
//        builder.transition().from(PlayCollectState.COLLECTING).to(PlayCollectState.TIMED_OUT).on(PlayCollectEvent.TIMEOUT).perform(TimeoutAction.INSTANCE);
//        builder.transition().from(PlayCollectState.COLLECTING).to(PlayCollectState.READY).on(PlayCollectEvent.RESTART);
//        builder.transition().from(PlayCollectState.COLLECTING).to(PlayCollectState.REPROMPTING).on(PlayCollectEvent.REPROMPT);
//        builder.transition().from(PlayCollectState.COLLECTING).to(PlayCollectState.NO_DIGITS_REPROMPTING).on(PlayCollectEvent.NO_DIGITS_REPROMPT);
//        builder.transition(TransitionPriority.HIGHEST).from(PlayCollectState.COLLECTING).to(PlayCollectState.CANCELED).on(PlayCollectEvent.CANCEL).perform(CancelAction.INSTANCE);
//        builder.onExit(PlayCollectState.COLLECTING).callMethod("exitCollecting");
//
//        builder.onEntry(PlayCollectState.TIMED_OUT).callMethod("enterTimedOut");
//        builder.transition().from(PlayCollectState.TIMED_OUT).to(PlayCollectState.PLAYING_SUCCESS).on(PlayCollectEvent.PLAY_SUCCESS);
//        builder.transition().from(PlayCollectState.TIMED_OUT).to(PlayCollectState.SUCCEEDED).on(PlayCollectEvent.SUCCEED);
//        builder.transition().from(PlayCollectState.TIMED_OUT).to(PlayCollectState.PLAYING_FAILURE).on(PlayCollectEvent.PLAY_FAILURE);
//        builder.transition().from(PlayCollectState.TIMED_OUT).to(PlayCollectState.FAILED).on(PlayCollectEvent.FAIL);
//        builder.transition().from(PlayCollectState.TIMED_OUT).to(PlayCollectState.REPROMPTING).on(PlayCollectEvent.REPROMPT);
//        builder.transition().from(PlayCollectState.TIMED_OUT).to(PlayCollectState.NO_DIGITS_REPROMPTING).on(PlayCollectEvent.NO_DIGITS_REPROMPT);
//        builder.transition(TransitionPriority.HIGHEST).from(PlayCollectState.TIMED_OUT).to(PlayCollectState.CANCELED).on(PlayCollectEvent.CANCEL).perform(CancelAction.INSTANCE);
//        builder.onExit(PlayCollectState.TIMED_OUT).callMethod("exitTimedOut");
//
//        builder.onEntry(PlayCollectState.REPROMPTING).callMethod("enterReprompting");
//        builder.transition().from(PlayCollectState.REPROMPTING).to(PlayCollectState.COLLECTING).on(PlayCollectEvent.COLLECT);
//        builder.internalTransition().within(PlayCollectState.REPROMPTING).on(PlayCollectEvent.NEXT_TRACK).callMethod("onReprompting");
//        builder.transition(TransitionPriority.HIGHEST).from(PlayCollectState.REPROMPTING).to(PlayCollectState.CANCELED).on(PlayCollectEvent.CANCEL).perform(CancelAction.INSTANCE);
//        builder.onExit(PlayCollectState.REPROMPTING).callMethod("exitReprompting");
//
//        builder.onEntry(PlayCollectState.NO_DIGITS_REPROMPTING).callMethod("enterNoDigitsReprompting");
//        builder.transition().from(PlayCollectState.NO_DIGITS_REPROMPTING).to(PlayCollectState.COLLECTING).on(PlayCollectEvent.COLLECT);
//        builder.internalTransition().within(PlayCollectState.NO_DIGITS_REPROMPTING).on(PlayCollectEvent.NEXT_TRACK).callMethod("onNoDigitsReprompting");
//        builder.transition(TransitionPriority.HIGHEST).from(PlayCollectState.NO_DIGITS_REPROMPTING).to(PlayCollectState.CANCELED).on(PlayCollectEvent.CANCEL).perform(CancelAction.INSTANCE);
//        builder.onExit(PlayCollectState.NO_DIGITS_REPROMPTING).callMethod("exitNoDigitsReprompting");
//
//        builder.onEntry(PlayCollectState.CANCELED).callMethod("enterCanceled");
//        builder.transition().from(PlayCollectState.CANCELED).to(PlayCollectState.SUCCEEDED).on(PlayCollectEvent.SUCCEED);
//        builder.transition().from(PlayCollectState.CANCELED).to(PlayCollectState.FAILED).on(PlayCollectEvent.FAIL);
//        builder.onExit(PlayCollectState.CANCELED).callMethod("exitCanceled");
//
//        builder.onEntry(PlayCollectState.PLAYING_SUCCESS).callMethod("enterPlayingSuccess");
//        builder.internalTransition().within(PlayCollectState.PLAYING_SUCCESS).on(PlayCollectEvent.NEXT_TRACK).callMethod("onPlayingSuccess");
//        builder.transition().from(PlayCollectState.PLAYING_SUCCESS).to(PlayCollectState.SUCCEEDED).on(PlayCollectEvent.SUCCEED);
//        builder.transition(TransitionPriority.HIGHEST).from(PlayCollectState.PLAYING_SUCCESS).to(PlayCollectState.SUCCEEDED).on(PlayCollectEvent.CANCEL);
//        builder.onExit(PlayCollectState.PLAYING_SUCCESS).callMethod("exitPlayingSuccess");
//
//        builder.onEntry(PlayCollectState.PLAYING_FAILURE).callMethod("enterPlayingFailure");
//        builder.internalTransition().within(PlayCollectState.PLAYING_FAILURE).on(PlayCollectEvent.NEXT_TRACK).callMethod("onPlayingFailure");
//        builder.transition().from(PlayCollectState.PLAYING_FAILURE).to(PlayCollectState.FAILED).on(PlayCollectEvent.FAIL);
//        builder.transition(TransitionPriority.HIGHEST).from(PlayCollectState.PLAYING_FAILURE).to(PlayCollectState.FAILED).on(PlayCollectEvent.CANCEL);
//        builder.onExit(PlayCollectState.PLAYING_FAILURE).callMethod("exitPlayingFailure");
//
//        builder.onEntry(PlayCollectState.SUCCEEDED).callMethod("enterSucceeded");
//        builder.defineState(PlayCollectState.SUCCEEDED).setFinal(true);
//
//        builder.onEntry(PlayCollectState.FAILED).callMethod("enterFailed");
//        builder.defineState(PlayCollectState.FAILED).setFinal(true);
    }
    
    public PlayCollectFsm build(DtmfDetector detector, DtmfDetectorListener detectorListener, Player player, PlayerListener playerListener, MgcpEventSubject eventSubject, ListeningScheduledExecutorService scheduler, PlayCollectContext context) {
        return builder.newStateMachine(PlayCollectState.PROCESSING, StateMachineConfiguration.getInstance().enableDebugMode(false), detector, detectorListener, player, playerListener, eventSubject, scheduler, context);
    }

}
