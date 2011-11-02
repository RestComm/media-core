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

import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionParm;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.RegularConnectionParm;

import org.apache.log4j.Logger;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionListener;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionState;
import org.mobicents.protocols.mgcp.stack.ExtendedJainMgcpProvider;

/**
 * Used as a listener for an actual connection and sends events to the MGCP CA.
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class ConnectionActivity implements ConnectionListener {
    
    private static Logger logger = Logger.getLogger(ConnectionActivity.class);
    private static int GEN = 1;
    
    protected Connection connection;
    private String id;
    private Call call;
    
    private Activities parent;
    private String callID;
    private EndpointIdentifier endpointID;
    
    protected ConnectionActivity(Activities parent, String callID, EndpointIdentifier endpointID) {
        this.parent = parent;

        this.id = Integer.toHexString(GEN++);
        if (GEN == Integer.MAX_VALUE) {
            GEN = 1;
        }        
    
        this.callID = callID;
        this.endpointID = endpointID;
        
        parent.calls.get(callID).connections.put(id, this);
        parent.endpoints.get(endpointID.getLocalEndpointName()).connections.put(id, this);
//        logger.info(String.format("Created activity call=%s, endpoint=%s, connection=%s",
//                callID, endpointID.getLocalEndpointName(), id));
    }
    
    public void attach(Connection connection) {
        this.connection = connection;
        this.connection.addListener(this);
//        logger.info(String.format("Created relation between %s and %s", id, connection.getId()));
    }
    
    public String getID() {
        return id;
    }
    
    public void onStateChange(Connection connection, ConnectionState oldState) {
    }

    public void onModeChange(Connection connection, ConnectionMode oldMode) {
    }
    
    public void onError(Connection connection, Exception e){    	
    	
        ConnectionParm[] parms = new ConnectionParm[3];
        parms[0] = new RegularConnectionParm(RegularConnectionParm.OCTETS_RECEIVED, (int)this.connection.getBytesReceived());
        parms[1] = new RegularConnectionParm(RegularConnectionParm.OCTETS_SENT, (int)this.connection.getBytesTransmitted());
        parms[2] = new RegularConnectionParm(RegularConnectionParm.JITTER, (int)(this.connection.getJitter() * 1000));
        
//        this.close();    	
        
        //Send DLCX
        MgcpController controller = this.call.getController();
        NotifiedEntity notifiedEntity = controller.getNotifiedEntity();
        CallIdentifier callId = new CallIdentifier(this.call.getID());
        
        //DLCX doesn't take the NotifiedEntity, hence the Endpoint should point to client Ip:port 
        EndpointIdentifier endpointId = new EndpointIdentifier(connection.getEndpoint().getLocalName(), notifiedEntity.getDomainName()+":"+notifiedEntity.getPortNumber());
        ConnectionIdentifier connectionID = new ConnectionIdentifier(this.getID());
        
    	DeleteConnection dlcx = new DeleteConnection(controller, callId, endpointId, connectionID);
    	dlcx.setConnectionParms(parms);
    	
    	ExtendedJainMgcpProvider extendedMgcpProvider = controller.getExtendedMgcpProvider();
    	extendedMgcpProvider.sendAsyncMgcpEvents(new JainMgcpEvent[] { dlcx });
    	
    	logger.error("Error in Connection. Closed Connection and sent DLCX command to client", e);
    }
    
    public Connection getMediaConnection() {
        return connection;
    }
    
//    public void close() {
//        
//    	connection.getEndpoint().deleteConnection(connection.getId());
//        connection.removeListener(this);
//        this.connection = null;
//        logger.info("Deleted connection activity " + id);
//    }
    
//    protected Call getCall(){
//    	return this.call;
//    }

    public void terminate() {        
        parent.calls.get(callID).connections.remove(id);
        parent.endpoints.get(endpointID.getLocalEndpointName()).connections.remove(id);
        
/*        if (connection != null) {
            logger.info(String.format("Terminated activity call=%s, endpoint=%s, connection=%s, origin=%s",
                    callID, endpointID.getLocalEndpointName(), id, connection.getId()));
        } else {
            logger.info(String.format("Terminated activity call=%s, endpoint=%s, connection=%s, origin=%s",
                    callID, endpointID.getLocalEndpointName(), id, "null"));
        }
*/        
        if (this.connection != null) {
            connection.removeListener(this);
            connection = null;
        }
        
    }
}
