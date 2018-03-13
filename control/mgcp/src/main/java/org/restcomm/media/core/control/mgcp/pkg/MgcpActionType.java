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

package org.restcomm.media.core.control.mgcp.pkg;

/**
 * List of actions that can be srpung when an event is fired.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public enum MgcpActionType {

    NOTIFY("N"), ACCUMULATE("A"), TREATE_ACCORDING_DIGIT_MAP("D"), SWAP("S"), IGNORE("I"), KEEP_ACTIVE("K"), EMBEDDED_RQNT("E");

    private final String symbol;

    private MgcpActionType(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return this.symbol;
    }

    public static MgcpActionType fromSymbol(String symbol) {
        if (symbol != null && !symbol.isEmpty()) {
            for (MgcpActionType action : values()) {
                if (action.symbol.equalsIgnoreCase(symbol)) {
                    return action;
                }
            }
        }
        return null;
    }
}
