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

package org.restcomm.media.control.mgcp.pkg.au.pc;

import java.util.Map;

import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.pkg.AbstractMgcpSignal;
import org.restcomm.media.control.mgcp.pkg.SignalType;
import org.restcomm.media.control.mgcp.pkg.au.AudioPackage;
import org.restcomm.media.control.mgcp.pkg.au.SignalParameters;
import org.restcomm.media.core.spi.dtmf.DtmfDetector;
import org.restcomm.media.core.spi.dtmf.DtmfDetectorListener;
import org.restcomm.media.core.spi.dtmf.DtmfEvent;
import org.restcomm.media.core.spi.player.Player;
import org.restcomm.media.core.spi.player.PlayerEvent;
import org.restcomm.media.core.spi.player.PlayerListener;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * Plays a prompt and collects DTMF digits entered by a user.
 * 
 * <p>
 * If no digits are entered or an invalid digit pattern is entered, the user may be reprompted and given another chance to enter
 * a correct pattern of digits. The following digits are supported: 0-9, *, #, A, B, C, D.
 * </p>
 * 
 * <p>
 * By default PlayCollect does not play an initial prompt, makes only one attempt to collect digits, and therefore functions as
 * a simple Collect operation.<br>
 * Various special purpose keys, key sequences, and key sets can be defined for use during the PlayCollect operation.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayCollect extends AbstractMgcpSignal {

    static final String SYMBOL = "pc";

    // Finite State Machine
    private final PlayCollectFsm fsm;

    // Media Components
    private final DtmfDetector detector;
    final DtmfDetectorListener detectorListener;

    private final Player player;
    final PlayerListener playerListener;

    // Execution Context
    private final PlayCollectContext context;

    public PlayCollect(Player player, DtmfDetector detector, int requestId, NotifiedEntity notifiedEntity, Map<String, String> parameters, ListeningScheduledExecutorService executor) {
        super(AudioPackage.PACKAGE_NAME, SYMBOL, SignalType.TIME_OUT, requestId, notifiedEntity, parameters);

        // Media Components
        this.detector = detector;
        this.detectorListener = new DetectorListener();

        this.player = player;
        this.playerListener = new AudioPlayerListener();

        // Execution Context
        this.context = new PlayCollectContext(detector, detectorListener, parameters);

        // Build FSM
        this.fsm = PlayCollectFsmBuilder.INSTANCE.build(detector, detectorListener, player, playerListener, this, executor, context);
    }
    
    public PlayCollect(Player player, DtmfDetector detector, int requestId, Map<String, String> parameters, ListeningScheduledExecutorService executor) {
        this(player, detector, requestId, null, parameters, executor);
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
            case NO_DIGITS_REPROMPT:
            case FAILURE_ANNOUNCEMENT:
            case SUCCESS_ANNOUNCEMENT:
            case NON_INTERRUPTIBLE_PLAY:
            case SPEED:
            case VOLUME:
            case CLEAR_DIGIT_BUFFER:
            case MAXIMUM_NUM_DIGITS:
            case MINIMUM_NUM_DIGITS:
            case DIGIT_PATTERN:
            case FIRST_DIGIT_TIMER:
            case INTER_DIGIT_TIMER:
            case EXTRA_DIGIT_TIMER:
            case RESTART_KEY:
            case REINPUT_KEY:
            case RETURN_KEY:
            case POSITION_KEY:
            case STOP_KEY:
            case START_INPUT_KEY:
            case END_INPUT_KEY:
            case INCLUDE_END_INPUT_KEY:
            case NUMBER_OF_ATTEMPTS:
                return true;

            default:
                return false;
        }
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
            fsm.fire(PlayCollectEvent.CANCEL, this.context);
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
            context.setLastTone(tone);
            fsm.fire(PlayCollectEvent.DTMF_TONE, PlayCollect.this.context);
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
                    fsm.fire(PlayCollectEvent.NEXT_TRACK, context);
                    break;

                case PlayerEvent.FAILED:
                    // TODO handle player failure
                    break;

                default:
                    break;
            }
        }
    }

}
