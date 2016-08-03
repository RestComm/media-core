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

package org.mobicents.media.control.mgcp.pkg.base;

import org.mobicents.media.control.mgcp.pkg.GenericMgcpEvent;

/**
 * The operation complete event is generated when the gateway was asked to apply one or several signals of type TO on the
 * endpoint or connection, and one or more of those signals completed without being stopped by the detection of a requested
 * event such as off-hook transition or dialed digit.
 * <p>
 * The completion report should carry as a parameter the name of the signal that came to the end of its live time, as in: <br>
 * <br>
 * O: G/oc(G/rt)<br>
 * <br>
 * In this case, the observed event occurred because the "rt" signal in the "G" package timed out.
 * </p>
 * <p>
 * If the reported signal was applied on a connection, the parameter supplied will include the name of the connection as well,
 * as in: <br>
 * <br>
 * O: G/oc(G/rt@0A3F58)<br>
 * <br>
 * When the operation complete event is requested, it cannot be parameterized with any event parameters.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class OperationComplete extends GenericMgcpEvent {

    public OperationComplete(String pkg, String signal) {
        super(pkg, "oc", signal);
    }

}
