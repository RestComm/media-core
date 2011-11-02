/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media.server.ctrl.mgcp;

import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.ModifyConnection;
import jain.protocol.ip.mgcp.message.ModifyConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.Endpoint;

/**
 * 
 * @author amit bhayani
 *
 */
public class ModifyConnectionAction implements Callable {

    private static Logger logger = Logger.getLogger(ModifyConnectionAction.class);
    private ModifyConnection mdcx;
    private MgcpController controller;
    private MgcpUtils utils = new MgcpUtils();

    protected ModifyConnectionAction(MgcpController controller, ModifyConnection req) {
        this.controller = controller;
        this.mdcx = req;
    }

    public JainMgcpResponseEvent call() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Request TX= " + mdcx.getTransactionHandle() + ", CallID = " + mdcx.getCallIdentifier() + ", Mode=" + mdcx.getMode() + ", Endpoint = " + mdcx.getEndpointIdentifier() + ", SDP present = " + (mdcx.getRemoteConnectionDescriptor() != null));
        }

        ModifyConnectionResponse response = null;

        EndpointIdentifier endpointID = mdcx.getEndpointIdentifier();
        String localEndpoint = endpointID.getLocalEndpointName();

        if (localEndpoint.contains("*") || localEndpoint.contains("$")) {
            return reject(ReturnCode.Protocol_Error);
        }

        CallIdentifier callID = mdcx.getCallIdentifier();
        Call call = null;
        try {        
            call = controller.activities.getCall(callID.toString());
        } catch (UnknownActivityException e) {
            return reject(ReturnCode.Unknown_Call_ID);
        }
        
        ConnectionIdentifier connectionID = mdcx.getConnectionIdentifier();
        ConnectionActivity connActivity = null;
        try {
            connActivity = call.getConnectionActivity(connectionID.toString());
        } catch (UnknownActivityException e) {
            return reject(ReturnCode.Incorrect_Connection_ID);
        }
        
        ConnectionMode mode = null;

        Endpoint endpoint = null;
        try {
            endpoint = controller.getServer().lookup(localEndpoint, true);
        } catch (Exception e) {
            return reject(ReturnCode.Endpoint_Unknown);
        }

        Connection connection = connActivity.getMediaConnection();

        if (mdcx.getMode() != null) {
            mode = utils.getMode(mdcx.getMode());
            connection.setMode(mode);
        }


        ConnectionDescriptor remoteConnectionDescriptor = mdcx.getRemoteConnectionDescriptor();
        if (remoteConnectionDescriptor != null) {
            connection.setRemoteDescriptor(remoteConnectionDescriptor.toString());
        }

        response = new ModifyConnectionResponse(this, ReturnCode.Transaction_Executed_Normally);
        response.setTransactionHandle(mdcx.getTransactionHandle());
	response.setLocalConnectionDescriptor(new ConnectionDescriptor(connection.getLocalDescriptor()));
        logger.info("Response TX = " + response.getTransactionHandle() + ", Response: " + response.getReturnCode());
        return response;
    }

    private ModifyConnectionResponse reject(ReturnCode code) {
        ModifyConnectionResponse response = new ModifyConnectionResponse(this, code);
        response.setTransactionHandle(mdcx.getTransactionHandle());
        return response;
    }
}
