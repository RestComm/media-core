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

package org.restcomm.javax.media.mscontrol.networkconnection;

import org.restcomm.fsm.State;
import org.restcomm.fsm.StateEventHandler;

import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;

/**
 *
 * @author kulikov
 */
public class DeleteConnectionRequest implements StateEventHandler {
    private NetworkConnectionImpl connection;
    
    protected DeleteConnectionRequest(NetworkConnectionImpl connection) {
        this.connection = connection;
    }
    
    public void onEvent(State state) {
        //if connection has not concrete name then it was not created
        if (!connection.getEndpoint().hasConcreteName()) {
            return;
        }
        //prepear callID and endpointID parameters for request
        CallIdentifier callId = connection.getMediaSession().getCallID();
        EndpointIdentifier endpointID = connection.getEndpoint().getIdentifier();
        ConnectionIdentifier connectionID = connection.getConnectionID();
        //ask for new unique transaction handler
        int txHandle = connection.getMediaSession().getUniqueHandler();
        
        //ask for new unique transaction handler
        DeleteConnection req = new DeleteConnection(this, callId, endpointID, connectionID);
        req.setTransactionHandle(txHandle);

        connection.getMediaSession().getDriver().attach(txHandle, new DeleteConnectionResponseHandler(connection));
        //send request
        connection.getMediaSession().getDriver().send(req);
    }

}
