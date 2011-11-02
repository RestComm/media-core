/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.test.ivr;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpProvider;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.DeleteConnectionResponse;
import jain.protocol.ip.mgcp.message.ModifyConnection;
import jain.protocol.ip.mgcp.message.ModifyConnectionResponse;
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.Notify;
import jain.protocol.ip.mgcp.message.parms.ConflictingParameterException;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.ConnectionParm;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;
import jain.protocol.ip.mgcp.message.parms.RequestedAction;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.RegularConnectionParm;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import java.util.TooManyListenersException;

/**
 *
 * @author kulikov
 */
public class Connection implements JainMgcpListener {

    public final static String MEDIA_PATH = "";
    public final static String ENDPOINT_NAME = "/mobicents/media/IVR/$";
    public final static String MEDIA_SERVER_ADDRESS = "192.168.1.2";
    public final static int MGCP_PORT = 2427;
    public final static int STATE_NULL = 0;
    public final static int STATE_OPENING = 1;
    public final static int STATE_OPENED = 2;
    public final static int STATE_STARTING = 3;
    public final static int STATE_STARTED = 4;
    public final static int STATE_DELETING = 5;
    public final static int STATE_DELETED = 6;
    public final static int STATE_CREATING = 7;
    public final static int STATE_CREATED = 8;
    
    private static int TX_GEN;
    private int id;
    
    private Call call;
    private JainMgcpProvider provider;
    private int state;
    private int txID;
    private EndpointIdentifier endpointID;
    private ConnectionIdentifier connectionID;
    private String sdp;
    private int requestID;

    protected int bytesSent;
    protected int bytesReceived;
    protected double jitter;
    
    private String remoteSDP;
    
    public Connection(int id, Call call, JainMgcpProvider provider) throws TooManyListenersException {
        this.id = id;
        this.call = call;
        this.provider = provider;
        this.provider.addJainMgcpListener(this);
    }

    public int getID() {
        return id;
    }
    
    public int getState() {
        return state;
    }
    
    public void create() {
        this.state = STATE_CREATING;
        this.sendCRCX(null);
    }

    public void create(String sdp) {
        this.remoteSDP = sdp;
        this.state = STATE_CREATING;
        this.sendCRCX(sdp);
    }

    public void modify(String sdp) {
        this.remoteSDP = sdp;
        this.state = STATE_OPENING;
        this.sendModify(sdp);
    }

    public String getLocalSDP() {
        return sdp;
    }

    public void startMedia() {
        this.sendRQNT(MEDIA_PATH);
        state = STATE_STARTING;
    }

    public void delete() {
        txID = ++TX_GEN;
        DeleteConnection deleteConnection = new DeleteConnection(this, call.callID,  endpointID, connectionID);
        deleteConnection.setTransactionHandle(txID);
        this.state = STATE_DELETING;
        provider.sendMgcpEvents(new JainMgcpEvent[]{deleteConnection});
    }

    private void sendModify(String sdp) {
        ModifyConnection request = new ModifyConnection(this, call.callID, endpointID, connectionID);
        request.setRemoteConnectionDescriptor(new ConnectionDescriptor(sdp));

        txID = ++TX_GEN;
        request.setTransactionHandle(txID);
        
        provider.sendMgcpEvents(new JainMgcpEvent[]{request});
    }
    
    private void sendCRCX(String sdp) {
        EndpointIdentifier endpoint = new EndpointIdentifier(ENDPOINT_NAME, MEDIA_SERVER_ADDRESS + ":" + MGCP_PORT);

        CreateConnection createConnection = new CreateConnection(this, call.callID, endpoint, ConnectionMode.SendRecv);

        if (sdp != null) {
            try {
                createConnection.setRemoteConnectionDescriptor(new ConnectionDescriptor(sdp));
            } catch (ConflictingParameterException e) {
                // should never happen
            }
        }

        txID = ++TX_GEN;
        createConnection.setTransactionHandle(txID);
        provider.sendMgcpEvents(new JainMgcpEvent[]{createConnection});
    }

    @Override
    public void processMgcpCommandEvent(JainMgcpCommandEvent request) {
        Notify event = (Notify) request;
        if (event.getRequestIdentifier().toString().matches(Integer.toString(requestID))) {
            return;
        }

        if (event.getObservedEvents()[0].getEventIdentifier() == MgcpEvent.oc) {
            
        }
    //TODO notify call
    }

    @Override
    public void processMgcpResponseEvent(JainMgcpResponseEvent response) {
        if (response.getTransactionHandle() != txID) {
            //this response does not relate to this connection
            return;
        }
        if (response.getReturnCode() == ReturnCode.Transaction_Being_Executed) {
           //provisional response
            return;
        }
        switch (state) {
            case STATE_CREATING :
                if (response.getReturnCode() != ReturnCode.Transaction_Executed_Normally) {
                    //some error is occured
                    state = STATE_DELETED;
                    call.onFailed(this);
                    return;
                }
                
                
                CreateConnectionResponse event = (CreateConnectionResponse) response;
                this.endpointID = event.getSpecificEndpointIdentifier();
                this.connectionID = event.getConnectionIdentifier();
                this.sdp = event.getLocalConnectionDescriptor().toString();
                
                if (remoteSDP != null) {
                    state = STATE_OPENED;
                    System.out.println("CallID= " + call.callID + " Endpoint=" + endpointID);
                    call.onConnectionConnected(this);
                } else {
                    state = STATE_CREATED;
                    call.onConnectionCreated(this);
                }
                break;
            case STATE_OPENING:
                if (response.getReturnCode() != ReturnCode.Transaction_Executed_Normally) {
                    //some error is occured
                    state = STATE_DELETING;
                    call.onFailed(this);
                    return;
                }
                
                ModifyConnectionResponse evt1 = (ModifyConnectionResponse) response;
                sdp = evt1.getLocalConnectionDescriptor().toString();
                
                state = STATE_OPENED;
                    System.out.println("CallID= " + call.callID + " Endpoint=" + endpointID);
                call.onConnectionConnected(this);
                break;
            case STATE_STARTING:
                if (response.getReturnCode() == ReturnCode.Transaction_Being_Executed) {
                    //provisional response
                    return;
                }
                if (response.getReturnCode() != ReturnCode.Transaction_Executed_Normally) {
                    //some error is occured
                    state = STATE_NULL;
                //TODO notify call
                }
                state = STATE_STARTED;
            case STATE_DELETING:
                if (response.getReturnCode() == ReturnCode.Transaction_Being_Executed) {
                    //provisional response
                    return;
                }
                if (response.getReturnCode() != ReturnCode.Transaction_Executed_Normally) {
                    //some error is occured
                    state = STATE_NULL;
                    return;
                //TODO notify call
                }
                DeleteConnectionResponse evt = (DeleteConnectionResponse) response;
                ConnectionParm[] stats = evt.getConnectionParms();
                System.out.println("Stats=" + stats);
                if (stats != null) {
                    for (int i = 0; i < stats.length; i++) {
                        if (stats[i].getConnectionParmType() == RegularConnectionParm.OCTETS_RECEIVED) {
                            bytesReceived = stats[i].getConnectionParmValue();
                        } else if (stats[i].getConnectionParmType() == RegularConnectionParm.OCTETS_SENT) {
                            bytesSent = stats[i].getConnectionParmValue();
                        } else if (stats[i].getConnectionParmType() == RegularConnectionParm.JITTER) {
                            jitter = (((double)stats[i].getConnectionParmValue())/1000);
                        }
                    }
                }
                state = STATE_DELETED;
                call.onConnectionDisconnected(this);

        }
    }

    private void sendRQNT(String mediaPath) {
        RequestIdentifier reqID = new RequestIdentifier(Integer.toString(++requestID));
        NotificationRequest notificationRequest = new NotificationRequest(this, endpointID, reqID);

        EventName[] signalRequests = {new EventName(PackageName.Announcement, MgcpEvent.ann.withParm(mediaPath), this.connectionID)};
        notificationRequest.setSignalRequests(signalRequests);

        RequestedAction[] actions = new RequestedAction[]{RequestedAction.NotifyImmediately};

        RequestedEvent[] requestedEvents = {
            new RequestedEvent(new EventName(PackageName.Announcement, MgcpEvent.oc, connectionID), actions),
            new RequestedEvent(new EventName(PackageName.Announcement, MgcpEvent.of, connectionID), actions),
            new RequestedEvent(new EventName(PackageName.Dtmf, MgcpEvent.dtmf0, connectionID), actions),
            new RequestedEvent(new EventName(PackageName.Dtmf, MgcpEvent.dtmf1, connectionID), actions),
            new RequestedEvent(new EventName(PackageName.Dtmf, MgcpEvent.dtmf2, connectionID), actions),
            new RequestedEvent(new EventName(PackageName.Dtmf, MgcpEvent.dtmf3, connectionID), actions),
            new RequestedEvent(new EventName(PackageName.Dtmf, MgcpEvent.dtmf4, connectionID), actions),
            new RequestedEvent(new EventName(PackageName.Dtmf, MgcpEvent.dtmf5, connectionID), actions),
            new RequestedEvent(new EventName(PackageName.Dtmf, MgcpEvent.dtmf6, connectionID), actions),
            new RequestedEvent(new EventName(PackageName.Dtmf, MgcpEvent.dtmf7, connectionID), actions),
            new RequestedEvent(new EventName(PackageName.Dtmf, MgcpEvent.dtmf8, connectionID), actions),
            new RequestedEvent(new EventName(PackageName.Dtmf, MgcpEvent.dtmf9, connectionID), actions),
            new RequestedEvent(new EventName(PackageName.Dtmf, MgcpEvent.dtmfA, connectionID), actions),
            new RequestedEvent(new EventName(PackageName.Dtmf, MgcpEvent.dtmfB, connectionID), actions),
            new RequestedEvent(new EventName(PackageName.Dtmf, MgcpEvent.dtmfC, connectionID), actions),
            new RequestedEvent(new EventName(PackageName.Dtmf, MgcpEvent.dtmfD, connectionID), actions),
            new RequestedEvent(new EventName(PackageName.Dtmf, MgcpEvent.dtmfStar, connectionID), actions),
            new RequestedEvent(new EventName(PackageName.Dtmf, MgcpEvent.dtmfHash, connectionID), actions)
        };

        notificationRequest.setRequestedEvents(requestedEvents);

        txID = ++TX_GEN;        
        notificationRequest.setTransactionHandle(txID);

        NotifiedEntity notifiedEntity = new NotifiedEntity(Tester.ADDRESS, Tester.ADDRESS, Tester.port);
        notificationRequest.setNotifiedEntity(notifiedEntity);


        provider.sendMgcpEvents(new JainMgcpEvent[]{notificationRequest});

    }
    
    
}
