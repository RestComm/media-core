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

import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class CreateConnectionAction implements Callable {

    private static final ConnectionIdentifier ERROR_CONNID = new ConnectionIdentifier("0");
    private CreateConnection crcx;
    private MgcpController controller;
    private Activities activities;
    
    private MgcpUtils utils = new MgcpUtils();
    
    private Call call;
    private boolean isNewCall;
    
    private ConnectionActivity connActivity;
    private ConnectionActivity connActivity2;
    
    private static Logger logger = Logger.getLogger(CreateConnectionAction.class);

    protected CreateConnectionAction(MgcpController controller, CreateConnection req) {
        this.controller = controller;
        this.activities = controller.activities;
        this.crcx = req;
    }

    /**
     * Creates RTP connection.
     * 
     * @param crcx
     *            parameters of the connection to be created incapsulated
     *            withing MGCP CRCX message.
     * @return response with result of the parameters of the actualy created
     *         connection
     */
    private JainMgcpResponseEvent createRtpConnection(CreateConnection crcx) {
//        controller.crcxReqCount+=1;
        String callID = crcx.getCallIdentifier().toString();
        // reading connection mode
        ConnectionMode mode = utils.getMode(crcx.getMode());
        if (mode == null) {
            return reject(ReturnCode.Unsupported_Or_Invalid_Mode);
        }

        // reading endpoint identiier
        EndpointIdentifier endpointID = crcx.getEndpointIdentifier();
        String localName = endpointID.getLocalEndpointName();
        if (localName.contains("*")) {
            return reject(ReturnCode.Endpoint_Unknown);
        }

        // lookup endpoint
        Endpoint endpoint = null;
        try {
            endpoint = controller.getServer().lookup(localName, true);
        } catch (ResourceUnavailableException e) {
            return reject(ReturnCode.Endpoint_Unknown);
        }

        try {
            call = activities.getCall(callID);
        } catch (UnknownActivityException e) {
            call = activities.createCall(callID);
            this.isNewCall = true;
        }
        
        try {
            connActivity = activities.createConnectionActivity(callID, new EndpointIdentifier(endpoint.getLocalName(), endpointID.getDomainName()));
        } catch (UnknownActivityException e) {
        }
        
        Connection connection = null;
        try {
            connection = endpoint.createConnection();
            connection.setMode(mode);
        } catch (Exception e) {
            return reject(ReturnCode.Endpoint_Insufficient_Resources);
        }

        // try to assign the session desriptor if present
        ConnectionDescriptor remoteSdp = crcx.getRemoteConnectionDescriptor();
        if (remoteSdp != null) {
            try {
                connection.setRemoteDescriptor(remoteSdp.toString());
            } catch (Exception e) {
                //delete current connection
                endpoint.deleteConnection(connection.getId());
                return reject(ReturnCode.Missing_RemoteConnectionDescriptor);
            }
        }

        connActivity.attach(connection);
        
        ConnectionDescriptor localSDP = new ConnectionDescriptor(connection.getLocalDescriptor());
        ConnectionIdentifier connectionID = new ConnectionIdentifier(connActivity.getID());

        // Prepearing response
        CreateConnectionResponse response = new CreateConnectionResponse(crcx.getSource(),
                ReturnCode.Transaction_Executed_Normally, connectionID);
        response.setSpecificEndpointIdentifier(new EndpointIdentifier(endpoint.getLocalName(), crcx.getEndpointIdentifier().getDomainName()));
        response.setConnectionIdentifier(connectionID);
        response.setLocalConnectionDescriptor(localSDP);
        response.setTransactionHandle(crcx.getTransactionHandle());


//        controller.crcxCount++;
        return response;
    }

    private JainMgcpResponseEvent createLink(CreateConnection crcx) {
//        controller.crcxReqCount+=2;
        String callID = crcx.getCallIdentifier().toString();
        // reading connection mode
        ConnectionMode mode = utils.getMode(crcx.getMode());
        if (mode == null) {
            return reject(ReturnCode.Unsupported_Or_Invalid_Mode);
        }

        // reading endpoint identiier
        EndpointIdentifier endpointID = crcx.getEndpointIdentifier();
        String localName = endpointID.getLocalEndpointName();
        if (localName.contains("*")) {
            return reject(ReturnCode.Endpoint_Unknown);
        }

        EndpointIdentifier endpointID2 = crcx.getSecondEndpointIdentifier();
        String localName2 = endpointID2.getLocalEndpointName();
        if (localName2.contains("*")) {
            return reject(ReturnCode.Endpoint_Unknown);
        }
        
        try {
            call = controller.activities.getCall(callID);
        } catch (UnknownActivityException e) {
            call = controller.activities.createCall(callID);
            this.isNewCall = true;
        }
        
        // lookup endpoint
        Endpoint endpoint = null;
        try {
            endpoint = controller.getServer().lookup(localName, true);
        } catch (ResourceUnavailableException e) {
            return reject(ReturnCode.Endpoint_Unknown);
        }
        

        // lookup endpoint 2
        Endpoint endpoint2 = null;
        try {
            endpoint2 = controller.getServer().lookup(localName2, true);
        } catch (ResourceUnavailableException e) {
            return reject(ReturnCode.Endpoint_Unknown);
        }

        
        try {
            connActivity = activities.createConnectionActivity(callID, new EndpointIdentifier(endpoint.getLocalName(), endpointID.getDomainName()));
            connActivity2 = activities.createConnectionActivity(callID, new EndpointIdentifier(endpoint2.getLocalName(), endpointID.getDomainName()));
        } catch (UnknownActivityException e) {
        }
        
        
        Connection connection = null;
        try {
            connection = endpoint.createLocalConnection();
            connection.setMode(mode);
        } catch (Exception e) {
            return reject(ReturnCode.Endpoint_Insufficient_Resources);
        }

        // The mode of second connection is always send/recv as specified in
        // RFC3435
        Connection connection2 = null;
        try {
            connection2 = endpoint2.createLocalConnection();
            connection2.setMode(ConnectionMode.SEND_RECV);
        } catch (Exception e) {
            try {
                endpoint.deleteConnection(connection.getId());
            } finally {
                return reject(ReturnCode.Endpoint_Insufficient_Resources);
            }
        }

        try {
            connection.setOtherParty(connection2);
            connection2.setOtherParty(connection);
        } catch (IOException e) {
            try {
                endpoint.deleteConnection(connection.getId());
                endpoint2.deleteConnection(connection2.getId());
            } finally {
                return reject(ReturnCode.Endpoint_Insufficient_Resources);
            }
        }

        connActivity.attach(connection);
        connActivity2.attach(connection2);
        

        ConnectionIdentifier connectionID = new ConnectionIdentifier(connActivity.getID());
        ConnectionIdentifier connectionID2 = new ConnectionIdentifier(connActivity2.getID());

        // Sending response
        CreateConnectionResponse response = new CreateConnectionResponse(crcx.getSource(),
                ReturnCode.Transaction_Executed_Normally, connectionID);
        response.setSpecificEndpointIdentifier(new EndpointIdentifier(endpoint.getLocalName(), crcx.getEndpointIdentifier().getDomainName()));
        response.setSecondEndpointIdentifier(new EndpointIdentifier(endpoint2.getLocalName(), crcx.getEndpointIdentifier().getDomainName()));
        response.setSecondConnectionIdentifier(connectionID2);
        response.setTransactionHandle(crcx.getTransactionHandle());


//        controller.crcxCount+=2;
        return response;
    }

    private CreateConnectionResponse reject(ReturnCode reason) {
        //terminate activities for supposed connection1
        if (connActivity != null) {
            connActivity.terminate();
        }
        
        //terminate activities for supposed connection2
        if (connActivity2 != null) {
            connActivity2.terminate();
        }
        
        //terminate call if it was new
        if (call != null && isNewCall) {
            call.terminate();
        }
        
        return new CreateConnectionResponse(crcx.getSource(), reason, ERROR_CONNID);
    }
    
    public JainMgcpResponseEvent call() throws Exception {
        logger.info(String.format("Request: call=%s, Endpoint=%s", crcx.getCallIdentifier(), crcx.getEndpointIdentifier()));
        // CreateConnection may be used to create either an RTP connection or
        // a pair of local connections. If SecondEndpoint is not specified then
        // RTP connection will be created or two local connections other way.
        JainMgcpResponseEvent response = crcx.getSecondEndpointIdentifier() == null ? 
            createRtpConnection(crcx) : createLink(crcx);
        response.setTransactionHandle(crcx.getTransactionHandle());
        logger.info("Response TX = " + response.getTransactionHandle() + ", Response: " + response.getReturnCode());
        return response;
    }
}
