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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.restcomm.javax.media.mscontrol.networkconnection;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import javax.media.mscontrol.MediaErr;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;

import org.restcomm.fsm.UnknownTransitionException;
import org.restcomm.javax.media.mscontrol.networkconnection.fsm.ConnectionTransition;

/**
 *
 * @author kulikov
 */
public class CreateConnectionResponseHandler implements JainMgcpListener {

    private NetworkConnectionImpl connection;
    
    public CreateConnectionResponseHandler(NetworkConnectionImpl connection) {
        this.connection = connection;
    }
    
    public void processMgcpCommandEvent(JainMgcpCommandEvent evt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void processMgcpResponseEvent(JainMgcpResponseEvent evt) {
        CreateConnectionResponse resp = (CreateConnectionResponse) evt;
        switch (resp.getReturnCode().getValue()) {
            case ReturnCode.TRANSACTION_BEING_EXECUTED :
                return;
            case ReturnCode.TRANSACTION_EXECUTED_NORMALLY :
                connection.connectionID = resp.getConnectionIdentifier();
                connection.setConcreteName(resp.getSpecificEndpointIdentifier()); 
                
                try {
                    connection.sdpPortManager.setLocalDescriptor(resp.getLocalConnectionDescriptor().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    connection.fsm.signalAsync(ConnectionTransition.FAILURE);
                    return;
                }
                
                try {
                    if (connection.sdpPortManager.remoteSdp != null) {
                        connection.fsm.signal(ConnectionTransition.OPENED);
                    } else {
                        connection.fsm.signal(ConnectionTransition.CREATED);
                    }
                } catch (UnknownTransitionException e) {
                }
                
                return;
            case ReturnCode.ENDPOINT_UNKNOWN :
                connection.error = MediaErr.NOT_FOUND;
                break;
            case ReturnCode.ENDPOINT_NOT_READY :
                connection.error = MediaErr.RESOURCE_UNAVAILABLE;
                break;
            case ReturnCode.ENDPOINT_IS_RESTARTING :
                connection.error = MediaErr.RESOURCE_UNAVAILABLE;
                break;
            case ReturnCode.ENDPOINT_INSUFFICIENT_RESOURCES :
                connection.error = MediaErr.RESOURCE_UNAVAILABLE;
                break;
            case ReturnCode.INCOMPATIBLE_PROTOCOL_VERSION :
                connection.error = MediaErr.BAD_SERVER;
                break;
            case ReturnCode.MISSING_REMOTECONNECTIONDESCRIPTOR :
            case 505 :
                connection.error = SdpPortManagerEvent.SDP_NOT_ACCEPTABLE;
                break;
            case ReturnCode.TRANSIENT_ERROR :
                connection.error = MediaErr.NOT_SUPPORTED;
                break;
            default:
                connection.error = MediaErr.UNKNOWN_ERROR;
        }
        
        // get error message
        connection.errorMsg = resp.getReturnCode().getComment();
        
        //signal to FSM
        try {
            connection.fsm.signal(ConnectionTransition.FAILURE);
        } catch (UnknownTransitionException e) {
        }
    }

}
