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

import org.mobicents.media.control.mgcp.pkg.MgcpPackage;

/**
 * This package defines events and signals for an ARF package for an Audio Server Media Gateway.
 * 
 * <p>
 * If an Advanced Audio Package signal is active on an endpoint and another signal of the same type is applied, the two signals
 * including parameters and parameter values will compared If the signals are identical, the signal in progress will be allowed
 * to continue and the new signal will be discarded.<br>
 * <b>Because of this behavior the Advanced Audio Package may not interoperate well with some other packages such as the Line
 * and Trunk packages.</b>
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 * @see <a href="https://tools.ietf.org/html/rfc2897">RFC2897</a>
 */
public class AudioPackage implements MgcpPackage {

    public static final String PACKAGE_NAME = "AU";

    @Override
    public String getPackageName() {
        return PACKAGE_NAME;
    }

    @Override
    public boolean isEventSupported(String event) {
        if (event == null || event.isEmpty()) {
            return false;
        }
        return (AdvancedAudioEventType.fromSymbol(event) != null);
    }

}
