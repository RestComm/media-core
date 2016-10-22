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

package org.mobicents.media.control.mgcp.pkg.au.pr;

import org.mobicents.media.control.mgcp.pkg.MgcpEventSubject;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerListener;
import org.mobicents.media.server.spi.recorder.Recorder;
import org.mobicents.media.server.spi.recorder.RecorderListener;
import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.StateMachineConfiguration;

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
                MgcpEventSubject.class, Recorder.class, RecorderListener.class, DtmfDetector.class, DtmfDetectorListener.class,
                Player.class, PlayerListener.class, PlayRecordContext.class);

        this.builder.defineFinishEvent(PlayRecordEvent.EVALUATE);

        this.builder.onEntry(PlayRecordState.ACTIVE).callMethod("enterActive");
        this.builder.defineParallelStatesOn(PlayRecordState.ACTIVE, PlayRecordState.RECORD, PlayRecordState.COLLECT, PlayRecordState.PROMPT);
        this.builder.transition().from(PlayRecordState.ACTIVE).to(PlayRecordState.TIMED_OUT).on(PlayRecordEvent.TIMEOUT);
        this.builder.transition().from(PlayRecordState.ACTIVE).to(PlayRecordState.CANCELED).on(PlayRecordEvent.CANCEL);
        this.builder.transition().from(PlayRecordState.ACTIVE).to(PlayRecordState.EVALUATING).on(PlayRecordEvent.EVALUATE);
        this.builder.onExit(PlayRecordState.ACTIVE).callMethod("exitActive");

        this.builder.defineSequentialStatesOn(PlayRecordState.PROMPT, PlayRecordState.PROMPTING, PlayRecordState.PROMPTED);
        this.builder.onEntry(PlayRecordState.PROMPTING).callMethod("enterPrompting");
        this.builder.internalTransition().within(PlayRecordState.PROMPTING).on(PlayRecordEvent.NEXT_TRACK).callMethod("onPrompting");
        this.builder.transition().from(PlayRecordState.PROMPTING).toFinal(PlayRecordState.PROMPTED).on(PlayRecordEvent.PROMPT_END);
        this.builder.onExit(PlayRecordState.PROMPTING).callMethod("exitPrompting");

        this.builder.onEntry(PlayRecordState.PROMPTED).callMethod("enterPrompted");

        this.builder.defineSequentialStatesOn(PlayRecordState.COLLECT, PlayRecordState.COLLECTING, PlayRecordState.COLLECTED);
        this.builder.onEntry(PlayRecordState.COLLECTING).callMethod("enterCollecting");
        this.builder.internalTransition().within(PlayRecordState.COLLECTING).on(PlayRecordEvent.DTMF_TONE).callMethod("onCollecting");
        this.builder.transition().from(PlayRecordState.COLLECTING).toFinal(PlayRecordState.COLLECTED).on(PlayRecordEvent.END_COLLECT);
        this.builder.onExit(PlayRecordState.COLLECTING).callMethod("exitCollecting");

        this.builder.onEntry(PlayRecordState.COLLECTED).callMethod("enterCollected");

        this.builder.defineSequentialStatesOn(PlayRecordState.RECORD, PlayRecordState.RECORDING, PlayRecordState.RECORDED);
        this.builder.onEntry(PlayRecordState.RECORDING).callMethod("enterRecording");
        this.builder.transition().from(PlayRecordState.RECORDING).toFinal(PlayRecordState.RECORDED).on(PlayRecordEvent.END_RECORD);
        this.builder.transition().from(PlayRecordState.COLLECTING).toFinal(PlayRecordState.RECORDED).on(PlayRecordEvent.END_RECORD);
        this.builder.transition().from(PlayRecordState.PROMPTING).toFinal(PlayRecordState.RECORDED).on(PlayRecordEvent.END_RECORD);
        this.builder.onExit(PlayRecordState.RECORDING).callMethod("exitRecording");

        this.builder.onEntry(PlayRecordState.RECORDED).callMethod("enterRecord");
        
        this.builder.onEntry(PlayRecordState.TIMED_OUT).callMethod("enterTimedOut");
        this.builder.transition().from(PlayRecordState.TIMED_OUT).to(PlayRecordState.ACTIVE).on(PlayRecordEvent.REPROMPT);
        this.builder.transition().from(PlayRecordState.TIMED_OUT).to(PlayRecordState.ACTIVE).on(PlayRecordEvent.NO_SPEECH);
        this.builder.transition().from(PlayRecordState.TIMED_OUT).to(PlayRecordState.PLAYING_SUCCESS).on(PlayRecordEvent.PLAY_SUCCESS);
        this.builder.transition().from(PlayRecordState.TIMED_OUT).to(PlayRecordState.SUCCEEDED).on(PlayRecordEvent.SUCCEED);
        this.builder.transition().from(PlayRecordState.TIMED_OUT).to(PlayRecordState.PLAYING_FAILURE).on(PlayRecordEvent.PLAY_FAILURE);
        this.builder.transition().from(PlayRecordState.TIMED_OUT).to(PlayRecordState.FAILED).on(PlayRecordEvent.FAIL);

        this.builder.onEntry(PlayRecordState.CANCELED).callMethod("enterCanceled");
        this.builder.transition().from(PlayRecordState.CANCELED).to(PlayRecordState.SUCCEEDED).on(PlayRecordEvent.SUCCEED);
        this.builder.transition().from(PlayRecordState.CANCELED).to(PlayRecordState.FAILED).on(PlayRecordEvent.FAIL);
        
        this.builder.onEntry(PlayRecordState.EVALUATING).callMethod("enterEvaluating");
        this.builder.transition().from(PlayRecordState.EVALUATING).to(PlayRecordState.ACTIVE).on(PlayRecordEvent.REPROMPT);
        this.builder.transition().from(PlayRecordState.EVALUATING).to(PlayRecordState.ACTIVE).on(PlayRecordEvent.NO_SPEECH);
        this.builder.transition().from(PlayRecordState.EVALUATING).to(PlayRecordState.PLAYING_SUCCESS).on(PlayRecordEvent.PLAY_SUCCESS);
        this.builder.transition().from(PlayRecordState.EVALUATING).to(PlayRecordState.SUCCEEDED).on(PlayRecordEvent.SUCCEED);
        this.builder.transition().from(PlayRecordState.EVALUATING).to(PlayRecordState.PLAYING_FAILURE).on(PlayRecordEvent.PLAY_FAILURE);
        this.builder.transition().from(PlayRecordState.EVALUATING).to(PlayRecordState.FAILED).on(PlayRecordEvent.FAIL);

        this.builder.onEntry(PlayRecordState.PLAYING_SUCCESS).callMethod("enterPlayingSuccess");
        this.builder.transition().from(PlayRecordState.PLAYING_SUCCESS).to(PlayRecordState.SUCCEEDED).on(PlayRecordEvent.SUCCEED);
        this.builder.transition().from(PlayRecordState.PLAYING_SUCCESS).to(PlayRecordState.SUCCEEDED).on(PlayRecordEvent.CANCEL);
        this.builder.onExit(PlayRecordState.PLAYING_SUCCESS).callMethod("exitPlayingSuccess");

        this.builder.onEntry(PlayRecordState.PLAYING_FAILURE).callMethod("enterPlayingFailure");
        this.builder.transition().from(PlayRecordState.PLAYING_FAILURE).to(PlayRecordState.SUCCEEDED).on(PlayRecordEvent.SUCCEED);
        this.builder.transition().from(PlayRecordState.PLAYING_FAILURE).to(PlayRecordState.SUCCEEDED).on(PlayRecordEvent.CANCEL);
        this.builder.onExit(PlayRecordState.PLAYING_FAILURE).callMethod("exitPlayingFailure");
    }

    public PlayRecordFsm build(MgcpEventSubject mgcpEventSubject, Recorder recorder, RecorderListener recorderListener,
            DtmfDetector detector, DtmfDetectorListener detectorListener, Player player, PlayerListener playerListener,
            PlayRecordContext context) {
        return this.builder.newStateMachine(PlayRecordState.ACTIVE,
                StateMachineConfiguration.getInstance().enableDebugMode(true), mgcpEventSubject, recorder, recorderListener,
                detector, detectorListener, player, playerListener, context);
    }

}
