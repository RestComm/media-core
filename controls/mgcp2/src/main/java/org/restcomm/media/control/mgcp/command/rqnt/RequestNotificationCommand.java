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

package org.restcomm.media.control.mgcp.command.rqnt;

import com.google.common.util.concurrent.FutureCallback;
import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.command.AbstractMgcpCommand;
import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.pkg.MgcpPackageManager;
import org.restcomm.media.control.mgcp.pkg.MgcpSignalProvider;
import org.restcomm.media.control.mgcp.util.collections.Parameters;

/**
 * The NotificationRequest command is used to request the gateway to send notifications upon the occurrence of specified events
 * in an endpoint.
 *
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class RequestNotificationCommand extends AbstractMgcpCommand {

    private static final Logger log = Logger.getLogger(RequestNotificationCommand.class);

    private final RequestNotificationFsm fsm;
    private final RequestNotificationContext context;

    public RequestNotificationCommand(int transactionId, Parameters<MgcpParameterType> parameters, RequestNotificationFsm fsm, MgcpEndpointManager endpointManager, MgcpPackageManager packageManager, MgcpSignalProvider signalProvider) {
        super(transactionId, parameters, endpointManager);
        this.fsm = fsm;
        this.context = new RequestNotificationContext(transactionId, parameters, endpointManager, signalProvider, packageManager);
    }

    @Override
    public void execute(FutureCallback<MgcpCommandResult> callback) {
        if (this.fsm.isStarted()) {
            callback.onFailure(new IllegalStateException("RQNT tx=" + context.getTransactionId() + " is already executing."));
        } else {
            this.context.setCallback(callback);
            this.fsm.start(this.context);
        }
    }

}
