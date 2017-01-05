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

import java.util.Map;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.pkg.AbstractMgcpSignal;
import org.mobicents.media.control.mgcp.pkg.SignalType;

import com.google.common.base.Optional;

/**
 * Gracefully terminates a Play, PlayCollect, or PlayRecord signal.
 * 
 * <p>
 * For each of these signals, if the signal is terminated with the EndSignal signal the resulting OperationComplete event or
 * OperationFailed event will contain all the parameters it would normally, including any collected digits or the recording id
 * of the recording that was in progress when the EndSignal signal was received.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class EndSignal extends AbstractMgcpSignal {

    private static final Logger log = Logger.getLogger(EndSignal.class);

    private final MgcpEndpoint endpoint;

    public EndSignal(MgcpEndpoint endpoint, int requestId, Map<String, String> parameters) {
        super(AudioPackage.PACKAGE_NAME, "es", SignalType.BRIEF, requestId, parameters);
        this.endpoint = endpoint;
    }

    @Override
    protected boolean isParameterSupported(String name) {
        return false;
    }

    @Override
    public void execute() {
        if (this.executing.getAndSet(true)) {
            throw new IllegalStateException("Already executing.");
        }

        String signal = Optional.fromNullable(getParameter(SignalParameters.SIGNAL.symbol())).or("");
        if (signal.isEmpty()) {
            log.warn("The EndSignal has no target signal to stop. Execution was canceled.");
        } else {
            this.endpoint.cancelSignal(signal);
        }
    }

    @Override
    public void cancel() {
        // It's a brief signal so it cannot be stopped
    }

}
