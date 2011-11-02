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
import jain.protocol.ip.mgcp.message.Notify;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import javolution.util.FastMap;
import org.apache.log4j.Logger;
import org.mobicents.media.server.ctrl.mgcp.signal.Dispatcher;
import org.mobicents.media.server.ctrl.mgcp.signal.RequestExecutor;
import org.mobicents.media.server.ctrl.mgcp.signal.UnknownEventException;
import org.mobicents.media.server.ctrl.mgcp.signal.UnknownPackageException;
import org.mobicents.media.server.ctrl.mgcp.signal.UnknownSignalException;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;

/**
 * Represents media server endpoint.
 * 
 * @author kulikov
 */
public class EndpointActivity implements Dispatcher {
    
    private MgcpController controller;
    
    private Endpoint endpoint;
    private EndpointIdentifier endpointID;
    
    private RequestExecutor requestExecutor;
    
    private RequestIdentifier requestID;
    private NotifiedEntity callAgent;
    
    private Activities parent;
    protected FastMap<String, ConnectionActivity> connections = new FastMap<String, ConnectionActivity>();
    
    private static final Logger logger = Logger.getLogger(EndpointActivity.class);
    
    protected EndpointActivity(EndpointIdentifier endpointID, Activities parent) {
        this.endpointID = endpointID;
        this.parent = parent;
        parent.endpoints.put(endpointID.getLocalEndpointName(), this);
    }
         
    public void setController(MgcpController controller) throws Exception {
        this.controller = controller;
        this.requestExecutor = controller.getRequestExecutor();
        this.requestExecutor.setDispatcher(this);
    }
    
    public void attach(Endpoint endpoint) {
        this.endpoint = endpoint;
    }
    
    public void accept(NotifiedEntity callAgent, RequestIdentifier requestID, RequestedEvent[] events, EventName[] signals) 
            throws UnknownSignalException, UnknownEventException, UnknownPackageException, UnknownActivityException {
        requestExecutor.cancel();
        
        this.callAgent = callAgent;
        this.requestID = requestID;
        
        requestExecutor.accept(events, signals);
        requestExecutor.execute();
    }

    public void onEvent(EventName event) {
        Notify notify = new Notify(this, endpointID, requestID, new EventName[]{event});
        notify.setNotifiedEntity(callAgent);
        notify.setTransactionHandle(controller.nextID());
        notify.setRequestIdentifier(requestID);
        controller.getMgcpProvider().sendMgcpEvents(new JainMgcpEvent[] { notify });
    }

    public void completed() {
        requestExecutor.cancel();
        requestExecutor.recycle();
        this.requestExecutor = null;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Connection getConnection(String ID) throws UnknownActivityException {
        if (!connections.containsKey(ID)) {
            logger.warn("Could not lookup connection: " + ID);
            throw new UnknownActivityException("Connection: " + ID);
        }
        return connections.get(ID).connection;
    }

    public ConnectionActivity getConnectionActivity(String ID) throws UnknownActivityException {
        if (!connections.containsKey(ID)) {
            logger.warn("Could not lookup connection: " + ID);
            throw new UnknownActivityException("Connection: " + ID);
        }
        return connections.get(ID);
    }
    
    public void terminate() {
        if (requestExecutor != null) {
            requestExecutor.cancel();
            requestExecutor.recycle();
        }
        
        
//        ConnectionActivity[] list = new ConnectionActivity[connections.size()];
//        connections.values().toArray(list);
        
//        for (ConnectionActivity a : list) {
//            a.terminate();
//        }
        
//        parent.endpoints.remove(this.endpointID.getLocalEndpointName());
    }
}
