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

package org.restcomm.media.control.mgcp.pkg.au.pr;

import java.util.Map;

import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.pkg.AbstractMgcpSignal;
import org.restcomm.media.control.mgcp.pkg.SignalType;
import org.restcomm.media.control.mgcp.pkg.au.AudioPackage;
import org.restcomm.media.control.mgcp.pkg.au.SignalParameters;
import org.restcomm.media.spi.dtmf.DtmfDetector;
import org.restcomm.media.spi.dtmf.DtmfDetectorListener;
import org.restcomm.media.spi.dtmf.DtmfEvent;
import org.restcomm.media.spi.player.Player;
import org.restcomm.media.spi.player.PlayerEvent;
import org.restcomm.media.spi.player.PlayerListener;
import org.restcomm.media.spi.recorder.Recorder;
import org.restcomm.media.spi.recorder.RecorderEvent;
import org.restcomm.media.spi.recorder.RecorderListener;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayRecord extends AbstractMgcpSignal {

    static final String SYMBOL = "pr";

    // Finite State Machine
    private final PlayRecordFsm fsm;

    // Media Components
    private final Player player;
    final PlayerListener playerListener;

    private final DtmfDetector detector;
    final DtmfDetectorListener detectorListener;

    private final Recorder recorder;
    final RecorderListener recorderListener;

    // Execution Context
    private final PlayRecordContext context;

    public PlayRecord(Player player, DtmfDetector detector, Recorder recorder, int requestId, NotifiedEntity notifiedEntity, Map<String, String> parameters) {
        super(AudioPackage.PACKAGE_NAME, SYMBOL, SignalType.TIME_OUT, requestId, notifiedEntity, parameters);

        // Media Components
        this.player = player;
        this.playerListener = new AudioPlayerListener();

        this.detector = detector;
        this.detectorListener = new DetectorListener();

        this.recorder = recorder;
        this.recorderListener = new AudioRecorderListener();

        // Execution Context
        this.context = new PlayRecordContext(parameters);

        // Finite State Machine
        this.fsm = PlayRecordFsmBuilder.INSTANCE.build(this, recorder, recorderListener, detector, detectorListener, player,
                playerListener, context);
    }
    
    public PlayRecord(Player player, DtmfDetector detector, Recorder recorder, int requestId, Map<String, String> parameters) {
        this(player, detector, recorder, requestId, null, parameters);
    }

    @Override
    public void execute() {
        if (!this.fsm.isStarted()) {
            this.fsm.start(this.context);
        }
    }

    @Override
    public void cancel() {
        if (this.fsm.isStarted()) {
            this.fsm.fire(PlayRecordEvent.CANCEL, this.context);
        }

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
            case NO_SPEECH_REPROMTP:
            case FAILURE_ANNOUNCEMENT:
            case SUCCESS_ANNOUNCEMENT:
            case NON_INTERRUPTIBLE_PLAY:
            case SPEED:
            case VOLUME:
            case CLEAR_DIGIT_BUFFER:
            case PRE_SPEECH_TIMER:
            case POST_SPEECH_TIMER:
            case TOTAL_RECORDING_LENGTH_TIMER:
            case RESTART_KEY:
            case REINPUT_KEY:
            case RETURN_KEY:
            case POSITION_KEY:
            case STOP_KEY:
            case END_INPUT_KEY:
            case NUMBER_OF_ATTEMPTS:
                return true;

            default:
                return false;
        }
    }

    /**
     * Listens to DTMF events raised by the DTMF Detector.
     * 
     * @author Henrique Rosa (henrique.rosa@telestax.com)
     *
     */
    private final class DetectorListener implements DtmfDetectorListener {

        @Override
        public void process(DtmfEvent event) {
            final char tone = event.getTone().charAt(0);
            PlayRecord.this.context.setTone(tone);
            fsm.fire(PlayRecordEvent.DTMF_TONE, PlayRecord.this.context);
        }

    }

    /**
     * Listen to Play events raised by the Player.
     * 
     * @author Henrique Rosa (henrique.rosa@telestax.com)
     *
     */
    private final class AudioPlayerListener implements PlayerListener {

        @Override
        public void process(PlayerEvent event) {
            switch (event.getID()) {
                case PlayerEvent.STOP:
                    fsm.fire(PlayRecordEvent.NEXT_TRACK, PlayRecord.this.context);
                    break;

                case PlayerEvent.FAILED:
                    fsm.fire(PlayRecordEvent.FAIL, PlayRecord.this.context);
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * Listen to Record events raised by the Recorder.
     * 
     * @author Henrique Rosa (henrique.rosa@telestax.com)
     *
     */
    private final class AudioRecorderListener implements RecorderListener {

        @Override
        public void process(RecorderEvent event) {
            switch (event.getID()) {
                case RecorderEvent.SPEECH_DETECTED:
                    context.setSpeechDetected(true);
                    break;

                case RecorderEvent.STOP:
                    switch (event.getQualifier()) {
                        case RecorderEvent.NO_SPEECH:
                            fsm.fire(PlayRecordEvent.NO_SPEECH, PlayRecord.this.context);
                            break;

                        case RecorderEvent.MAX_DURATION_EXCEEDED:
                            fsm.fire(PlayRecordEvent.MAX_DURATION_EXCEEDED, PlayRecord.this.context);
                            break;

                        default:
                            fsm.fire(PlayRecordEvent.END_RECORD, PlayRecord.this.context);
                            break;
                    }
                    break;
                case RecorderEvent.FAILED:
                    fsm.fire(PlayRecordEvent.FAIL, PlayRecord.this.context);
                    break;

                default:
                    break;
            }
        }

    }

}
