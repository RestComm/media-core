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

package org.mobicents.media.control.mgcp.pkg.au;

import org.mobicents.media.control.mgcp.pkg.AbstractMgcpSignal;
import org.mobicents.media.control.mgcp.pkg.SignalType;

/**
 * Plays a prompt and records user speech.
 * 
 * <p>
 * If the user does not speak, the user may be reprompted and given another chance to record.
 * </p>
 * 
 * <p>
 * By default PlayRecord does not play an initial prompt, makes only one attempt to record, and therefore functions as a simple
 * Record operation.
 * <p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayRecord extends AbstractMgcpSignal {

    public PlayRecord() {
        super(AudioPackage.PACKAGE_NAME, "pr", SignalType.TIME_OUT);
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

    @Override
    public void execute() {
        // TODO Auto-generated method stub

    }

    @Override
    public void cancel() {
        // TODO Auto-generated method stub

    }

}
