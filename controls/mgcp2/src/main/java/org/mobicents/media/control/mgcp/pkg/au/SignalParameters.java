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

/**
 * The PlayAnnouncement, PlayRecord, and PlayCollect events may each be qualified by a string of parameters, most of which are
 * optional. Where appropriate, parameters default to reasonable values.
 * <p>
 * The only event with a required parameter is PlayAnnouncement. If a Play-Announcement event is not provided with a parameter
 * specifying some form of playable audio an error is returned to the application.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public enum SignalParameters {

    ANNOUNCEMENT("an"),
    INITIAL_PROMPT("ip"),
    REPROMPT("rp"),
    NO_DIGITS_REPROMPT("nd"),
    NO_SPEECH_REPROMTP("ns"),
    FAILURE_ANNOUNCEMENT("fa"),
    SUCCESS_ANNOUNCEMENT("sa"), 
    NON_INTERRUPTIBLE_PLAY("ni"),
    ITERATIONS("it"),
    INTERVAL("iv"),
    DURATION("du"),
    SPEED("sp"),
    VOLUME("vl"),
    CLEAR_DIGIT_BUFFER("cb"),
    MAXIMUM_NUM_DIGITS("mx"),
    MINIMUM_NUM_DIGITS("mn"),
    DIGIT_PATTERN("dp"),
    FIRST_DIGIT_TIMER("fdt"),
    INTER_DIGIT_TIMER("idt"),
    EXTRA_DIGIT_TIMER("edt"),
    PRE_SPEECH_TIMER("prt"),
    POST_SPEECH_TIMER("pst"),
    TOTAL_RECORDING_LENGTH_TIMER("rlt"),
    RESTART_KEY("rsk"),
    REINPUT_KEY("rik"),
    RETURN_KEY("rtk"),
    POSITION_KEY("psk"),
    STOP_KEY("stk"),
    START_INPUT_KEY("sik"),
    END_INPUT_KEY("eik"),
    INCLUDE_END_INPUT_KEY("iek"),
    NUMBER_OF_ATTEMPTS("na"), 
    RECORD_ID("ri");

    private final String symbol;

    private SignalParameters(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    public static final SignalParameters fromSymbol(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return null;
        }

        for (SignalParameters parameter : values()) {
            if (parameter.symbol.equalsIgnoreCase(symbol)) {
                return parameter;
            }
        }
        return null;
    }

}
