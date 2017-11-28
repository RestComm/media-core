/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.control.mgcp.pkg.au.es;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.au.AudioPackage;
import org.restcomm.media.control.mgcp.pkg.au.SignalParameters;
import org.restcomm.media.control.mgcp.signal.AbstractSignal;
import org.restcomm.media.control.mgcp.signal.BriefSignal;

import java.util.Map;

/**
 * Gracefully terminates a Play, PlayCollect, or PlayRecord signal.
 * <p>
 * <p>
 * For each of these signals, if the signal is terminated with the EndSignal signal the resulting OperationComplete event or
 * OperationFailed event will contain all the parameters it would normally, including any collected digits or the recording id
 * of the recording that was in progress when the EndSignal signal was received.
 * </p>
 *
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class EndSignal extends AbstractSignal<Void> implements BriefSignal {

    private static final Logger log = Logger.getLogger(EndSignal.class);

    private final MgcpEndpoint endpoint;

    public EndSignal(MgcpEndpoint endpoint, String requestId, Map<String, String> parameters) {
        super(requestId, AudioPackage.PACKAGE_NAME, "es", parameters);
        this.endpoint = endpoint;
    }

    @Override
    public boolean isParameterSupported(String name) {
        // Check if parameter is valid
        SignalParameters parameter = SignalParameters.fromSymbol(name);
        if (parameter == null) {
            return false;
        }

        // Check if parameter is supported
        switch (parameter) {
            case SIGNAL:
                return true;

            default:
                return false;
        }
    }

    @Override
    public void execute(final FutureCallback<Void> callback) {
        final String signal = Optional.fromNullable(getParameter(SignalParameters.SIGNAL.symbol())).or("");

        if (signal.isEmpty()) {
            // No indication of which signal to target. Abort operation.
            Throwable t = new IllegalArgumentException("The EndSignal (X:" + getRequestId() + " has no target signal to stop.");
            callback.onFailure(t);
        } else {
            // Ask endpoint to retrieve status of quarantined signal
            this.endpoint.endSignal(this.getRequestId(), signal, new FutureCallback<MgcpEvent>() {

                @Override
                public void onSuccess(MgcpEvent result) {
                    if (log.isDebugEnabled()) {
                        log.debug("Ended signal " + signal + "(X:" + getRequestId() + ") with result " + result.toString());
                    }

                    // Signal was found and has a result.
                    // Raise event on endpoint
                    endpoint.onEvent(EndSignal.this, result);

                    // Notify callback that operation ended successfully
                    callback.onSuccess(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    if (log.isDebugEnabled()) {
                        log.debug("Could not end signal " + signal + "(X:" + getRequestId() + ")");
                    }

                    // Notify callback that operation failed
                    callback.onFailure(t);
                }
            });
        }
    }
}
