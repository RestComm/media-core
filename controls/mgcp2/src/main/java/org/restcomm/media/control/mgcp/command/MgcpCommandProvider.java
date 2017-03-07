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

package org.restcomm.media.control.mgcp.command;

import org.restcomm.media.control.mgcp.call.MgcpCallManager;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpRequestType;
import org.restcomm.media.control.mgcp.pkg.MgcpPackageManager;
import org.restcomm.media.control.mgcp.pkg.MgcpSignalProvider;
import org.restcomm.media.control.mgcp.util.collections.Parameters;

/**
 * Provides MGCP commands to be executed.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpCommandProvider {

    private final MgcpEndpointManager endpointManager;
    private final MgcpSignalProvider signalProvider;
    private final MgcpPackageManager packageManager;
    private final MgcpCallManager callManager;

    public MgcpCommandProvider(MgcpEndpointManager endpointManager, MgcpPackageManager packageManager, MgcpSignalProvider signalProvider, MgcpCallManager callManager) {
        super();
        this.endpointManager = endpointManager;
        this.packageManager = packageManager;
        this.signalProvider = signalProvider;
        this.callManager = callManager;
    }

    public MgcpCommand provide(MgcpRequestType type, int transactionId, Parameters<MgcpParameterType> parameters) {
        switch (type) {
            case CRCX:
                return new CreateConnectionCommand(transactionId, parameters, this.endpointManager);

            case MDCX:
                return new ModifyConnectionCommand(transactionId, parameters, this.endpointManager);

            case DLCX:
                return new DeleteConnectionCommand(transactionId, parameters, this.endpointManager);

            case RQNT:
                return new RequestNotificationCommand(transactionId, parameters, this.endpointManager, this.packageManager, this.signalProvider);

            case AUCX:
                return new AuditConnectionCommand(transactionId, parameters, this.endpointManager);

            case AUEP:
                return new AuditEndpointCommand(transactionId, parameters, this.endpointManager);

            default:
                throw new IllegalArgumentException("Unsupported command type " + type.name());
        }
    }

}
