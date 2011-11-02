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
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.DeleteConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionParm;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.RegularConnectionParm;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.mobicents.media.server.spi.Endpoint;

/**
 *
 * @author kulikov
 * @author amit bhayani
 */
public class DeleteConnectionAction implements Callable {

    private static Logger logger = Logger.getLogger(DeleteConnectionAction.class);
    
    private DeleteConnection req;
    private MgcpController controller;
    private MgcpUtils utils = new MgcpUtils();
    
    protected DeleteConnectionAction(MgcpController controller, DeleteConnection req) {
        this.controller = controller;
        this.req = req;
    }
    
    private JainMgcpResponseEvent endpointDeleteConnections(EndpointIdentifier endpointID) {
        //lookup endpoint activity first
        EndpointActivity endpointActivity = controller.activities.getEndpointActivity(endpointID);
        
        Endpoint endpoint = null;
        try {
            endpoint = controller.getServer().lookup(endpointID.getLocalEndpointName(), true);
        } catch (Exception e) {
            return new DeleteConnectionResponse(controller, ReturnCode.Endpoint_Unknown);
        }
        
        endpoint.deleteAllConnections();
        
        Collection<ConnectionActivity> activities = endpointActivity.connections.values();
        for (ConnectionActivity activity : activities) {
            activity.terminate();
        }
        
        endpointActivity.terminate();
        
        DeleteConnectionResponse response = new DeleteConnectionResponse(controller, ReturnCode.Transaction_Executed_Normally);
        return response;
    }
    
    private JainMgcpResponseEvent deleteConnection(EndpointIdentifier endpointID, String connectionID) {
        Endpoint endpoint = null;
        try {
            endpoint = controller.getServer().lookup(endpointID.getLocalEndpointName(), true);
        } catch (Exception e) {
            return new DeleteConnectionResponse(controller, ReturnCode.Endpoint_Unknown);
        }

        EndpointActivity endpointActivity = controller.activities.getEndpointActivity(endpointID); 
        
        ConnectionActivity activity = null;
        try {
            activity = endpointActivity.getConnectionActivity(connectionID);
        } catch (UnknownActivityException e) {
            return new DeleteConnectionResponse(controller, ReturnCode.Incorrect_Connection_ID);
        }
        
        ConnectionParm[] parms = new ConnectionParm[3];
        parms[0] = new RegularConnectionParm(RegularConnectionParm.OCTETS_RECEIVED, (int)activity.connection.getBytesReceived());
        parms[1] = new RegularConnectionParm(RegularConnectionParm.OCTETS_SENT, (int)activity.connection.getBytesTransmitted());
        parms[2] = new RegularConnectionParm(RegularConnectionParm.JITTER, (int)(activity.connection.getJitter() * 1000));
        
        endpoint.deleteConnection(activity.connection.getId());
        activity.terminate();
        
        DeleteConnectionResponse response = new DeleteConnectionResponse(controller, ReturnCode.Transaction_Executed_Normally);
        response.setConnectionParms(parms);
        
        endpointActivity.terminate();
        return response;
    }
    
    public JainMgcpResponseEvent call() throws Exception {
//        controller.dlcxReqCount++;
        logger.info(String.format("Request: tx=%d call=%s, endpoint=%s, connection=%s", 
                req.getTransactionHandle(), req.getCallIdentifier(), req.getEndpointIdentifier(), req.getConnectionIdentifier()));
        
        int txID = req.getTransactionHandle();
        CallIdentifier callID = req.getCallIdentifier();
        EndpointIdentifier endpointID = req.getEndpointIdentifier();
        ConnectionIdentifier connectionID = req.getConnectionIdentifier();
        
        JainMgcpResponseEvent response = null;
        if (endpointID != null && callID == null && connectionID == null) {
            response = this.endpointDeleteConnections(endpointID);
        } else if (endpointID != null && callID != null && connectionID == null) {
            //TODO : Delete all connection of Endpoint that belong to given callId
            response = this.endpointDeleteConnections(endpointID);
        } else if (endpointID != null && callID != null && connectionID != null) {
            response = this.deleteConnection(endpointID, connectionID.toString());
//                controller.dlcxCount++;
        } else {
        	//This is error condition        	
            response = new DeleteConnectionResponse(controller, ReturnCode.Protocol_Error);
        } 
        //Otherwise it wont be sent.
        response.setTransactionHandle(txID);
        logger.info("Response TX=" + txID + ", response=" + response.getReturnCode());
        return response;
    }
}
