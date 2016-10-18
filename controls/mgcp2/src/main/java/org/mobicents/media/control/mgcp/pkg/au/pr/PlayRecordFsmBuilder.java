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

        this.builder.onEntry(PlayRecordState.READY).callMethod("enterReady");
        this.builder.transition().from(PlayRecordState.READY).to(PlayRecordState.ACTIVE).on(PlayRecordEvent.START);
        this.builder.onExit(PlayRecordState.READY).callMethod("exitReady");

        this.builder.onEntry(PlayRecordState.ACTIVE).callMethod("enterActive");
        this.builder.transition().from(PlayRecordState.ACTIVE).to(PlayRecordState.READY).on(PlayRecordEvent.EVALUATE);
        this.builder.transition().from(PlayRecordState.ACTIVE).toFinal(PlayRecordState.TIMED_OUT).on(PlayRecordEvent.TIMEOUT);
        this.builder.onExit(PlayRecordState.ACTIVE).callMethod("exitActive");
        this.builder.defineParallelStatesOn(PlayRecordState.ACTIVE, PlayRecordState.COLLECT, PlayRecordState.PROMPT);

        this.builder.defineSequentialStatesOn(PlayRecordState.PROMPT, PlayRecordState._PROMPTING, PlayRecordState._PROMPTED);
        this.builder.onEntry(PlayRecordState._PROMPTING).callMethod("enterPrompting");
        this.builder.internalTransition().within(PlayRecordState._PROMPTING).on(PlayRecordEvent.NEXT_TRACK).callMethod("onPrompting");
        this.builder.transition().from(PlayRecordState._PROMPTING).toFinal(PlayRecordState._PROMPTED).on(PlayRecordEvent.PROMPT_END);
        this.builder.onExit(PlayRecordState._PROMPTING).callMethod("exitPrompting");

        this.builder.onEntry(PlayRecordState._PROMPTED).callMethod("enterPrompted");
        this.builder.onExit(PlayRecordState._PROMPTED).callMethod("exitPrompted");

        this.builder.defineSequentialStatesOn(PlayRecordState.COLLECT, PlayRecordState._COLLECTING, PlayRecordState._COLLECTED);
        this.builder.onEntry(PlayRecordState._COLLECTING).callMethod("enterCollecting");
        this.builder.internalTransition().within(PlayRecordState._COLLECTING).on(PlayRecordEvent.DTMF_TONE).callMethod("onCollecting");
        this.builder.transition().from(PlayRecordState._COLLECTING).toFinal(PlayRecordState._COLLECTED).on(PlayRecordEvent.END_COLLECT);
        this.builder.onExit(PlayRecordState._COLLECTING).callMethod("exitCollecting");

        this.builder.onEntry(PlayRecordState._COLLECTED).callMethod("enterCollected");
        this.builder.onExit(PlayRecordState._COLLECTED).callMethod("exitCollected");
        
        this.builder.onEntry(PlayRecordState.TIMED_OUT).callMethod("enterTimedOut");
    }

    public PlayRecordFsm build(MgcpEventSubject mgcpEventSubject, Recorder recorder, RecorderListener recorderListener,
            DtmfDetector detector, DtmfDetectorListener detectorListener, Player player, PlayerListener playerListener,
            PlayRecordContext context) {
        return this.builder.newStateMachine(PlayRecordState.READY, StateMachineConfiguration.getInstance().enableDebugMode(true), mgcpEventSubject, recorder, recorderListener, detector,
                detectorListener, player, playerListener, context);
    }

}
