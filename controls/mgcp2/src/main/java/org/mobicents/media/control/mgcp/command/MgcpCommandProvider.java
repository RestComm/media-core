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

package org.mobicents.media.control.mgcp.command;

import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpRequestType;
import org.mobicents.media.control.mgcp.pkg.MgcpSignalProvider;
import org.mobicents.media.control.mgcp.util.Parameters;

/**
 * Provides MGCP commands to be executed.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpCommandProvider {

    private final MgcpEndpointManager endpointManager;
    private final MgcpSignalProvider signalProvider;

    public MgcpCommandProvider(MgcpEndpointManager endpointManager, MgcpSignalProvider signalProvider) {
        super();
        this.endpointManager = endpointManager;
        this.signalProvider = signalProvider;
    }

    public MgcpCommand provide(int transactionId, MgcpRequestType type, Parameters<MgcpParameterType> parameters) {
        switch (type) {
            case CRCX:
                return new CreateConnectionCommand(transactionId, this.endpointManager, parameters);

            case MDCX:
                return new ModifyConnectionCommand(transactionId, this.endpointManager, parameters);

            case DLCX:
                return new DeleteConnectionCommand(transactionId, this.endpointManager, parameters);

            case RQNT:
                return new RequestNotificationCommand(transactionId, this.endpointManager, parameters, this.signalProvider);

            case AUCX:
                return new AuditConnectionCommand(transactionId, this.endpointManager, parameters);

            case AUEP:
                return new AuditEndpointCommand(transactionId, this.endpointManager, parameters);

            default:
                throw new IllegalArgumentException("Unsupported command type " + type.name());
        }
    }

}
