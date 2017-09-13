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
        
package org.restcomm.media.control.mgcp.pkg.au;

/**
 * Each event has an associated set of possible return parameters which are listed in the following tables.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public enum ReturnParameters {

    VOICE_INTERRUPT("vi"),
    INTERRUPTING_KEY_SEQUENCE("ik"),
    AMOUNT_PLAYED("ap"),
    NUMBER_OF_ATTEMPTS("na"),
    DIGITS_COLLECTED("dc"),
    RECORDING_ID("ri"),
    RETURN_CODE("rc"),
    ASR_RESULT("asrr");

    private final String symbol;

    private ReturnParameters(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

}
