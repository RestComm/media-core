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

import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.media.mscontrol.MediaConfig;
import javax.media.mscontrol.MediaErr;
import javax.media.mscontrol.MediaEvent;
import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.resource.Action;
import javax.media.mscontrol.resource.AllocationEventListener;

import org.restcomm.fsm.FSM;
import org.restcomm.fsm.Logger;
import org.restcomm.fsm.UnknownTransitionException;
import org.restcomm.javax.media.mscontrol.MediaConfigImpl;
import org.restcomm.javax.media.mscontrol.MediaSessionImpl;
import org.restcomm.javax.media.mscontrol.container.ContainerImpl;
import org.restcomm.javax.media.mscontrol.container.Endpoint;
import org.restcomm.javax.media.mscontrol.networkconnection.fsm.ConnectionState;
import org.restcomm.javax.media.mscontrol.networkconnection.fsm.ConnectionTransition;

/**
 * 
 * @author amit bhayani
 * 
 */
public final class NetworkConnectionImpl extends ContainerImpl implements NetworkConnection, Logger {

    //Configuration pattern related to NetworkConnection.BASE
    public final static MediaConfig BASE_CONFIG = new BaseConfig().getConfig();
    
    protected SdpPortManagerImpl sdpPortManager = null;

//    protected EndpointIdentifier endpointName;
    protected ConnectionIdentifier connectionID;
    
    private CopyOnWriteArrayList<MediaEventListener<? extends MediaEvent<?>>> listeners = 
            new CopyOnWriteArrayList<MediaEventListener<? extends MediaEvent<?>>>();
    
    //FSM processor
    protected FSM fsm;
    
    //last error occured
    protected MediaErr error = MediaErr.NO_ERROR;
    protected String errorMsg;
    
    public NetworkConnectionImpl(MediaSessionImpl session, MediaConfigImpl config) throws MsControlException {
        super(session, config.getParameters());
        this.config = config;
        
        //determine endpoint local name
        String localName = (String)config.getValue(ENDPOINT_NAME);
        //domain name of the server is constructed using proprties from config
        String domainName = session.getDriver().getRemoteDomainName();
        
        //finally, the endpoint identifier is constructed
//        endpointName = new EndpointIdentifier(localName, domainName);
        endpoint = new Endpoint(new EndpointIdentifier(localName, domainName));
        maxJoinees = 1;
        sdpPortManager = new SdpPortManagerImpl(this);
        
        this.init();
    }

    protected void init() {        
        fsm = new FSM(session.getDriver().getScheduler());
        fsm.setLogger(this);
        
        fsm.createState(ConnectionState.NULL);
        fsm.createState(ConnectionState.OPENING);
        fsm.createState(ConnectionState.OPEN);
        fsm.createState(ConnectionState.CANCELED);
        fsm.createState(ConnectionState.HALF_OPEN);
        fsm.createState(ConnectionState.MODIFYING);
        fsm.createState(ConnectionState.CLOSING).setOnEnter(
                new DeleteConnectionRequest(this));
        fsm.createState(ConnectionState.FAILED).setOnEnter(
                new SdpPortManagerEventImpl(sdpPortManager, SdpPortManagerEvent.NETWORK_STREAM_FAILURE));
        fsm.createState(ConnectionState.INVALID);
        
        fsm.setStart(ConnectionState.NULL);
        fsm.setEnd(ConnectionState.INVALID);
        
        //******************************************************************/
        //               STATE NULL                                         /
        //******************************************************************/
        //transition from NULL to OPENING
        fsm.createTransition(ConnectionTransition.OPEN, 
                ConnectionState.NULL, ConnectionState.OPENING).setHandler(
                new CreateConnectionRequest(this));
        fsm.createTransition(ConnectionTransition.CLOSE, 
                ConnectionState.NULL, ConnectionState.INVALID);
        
        //******************************************************************/
        //               STATE OPENING                                      /
        //******************************************************************/
        //transition from OPENING to OPEN
        fsm.createTransition(ConnectionTransition.OPENED, 
                ConnectionState.OPENING, ConnectionState.OPEN).setHandler(
                new SdpPortManagerEventImpl(sdpPortManager, SdpPortManagerEvent.ANSWER_GENERATED));
        
        //transition from OPENING to HALF_OPEN
        fsm.createTransition(ConnectionTransition.CREATED, 
                ConnectionState.OPENING, ConnectionState.HALF_OPEN).setHandler(
                new SdpPortManagerEventImpl(sdpPortManager, SdpPortManagerEvent.OFFER_GENERATED));
        
        //transition from OPENING to CANCELED        
        fsm.createTransition(ConnectionTransition.CLOSE, 
                ConnectionState.OPENING, ConnectionState.CANCELED);
        
        //transition from OPENING to FAILED        
        fsm.createTransition(ConnectionTransition.FAILURE, 
                ConnectionState.OPENING, ConnectionState.FAILED);
        
        //transition from OPENING to FAILED        
        fsm.createTimeoutTransition(
                ConnectionState.OPENING, ConnectionState.FAILED, 5000).setHandler(
                new TimeoutError(this));
        
        
        
        //******************************************************************/
        //               STATE OPENED                                       /
        //******************************************************************/
        //transition to CLOSING
        fsm.createTransition(ConnectionTransition.CLOSE, 
                ConnectionState.OPEN, ConnectionState.CLOSING);

        //******************************************************************/
        //               STATE HALF_OPEN                                    /
        //******************************************************************/
        //transition to MODIFYING
        fsm.createTransition(ConnectionTransition.MODIFY, 
                ConnectionState.HALF_OPEN, ConnectionState.MODIFYING).setHandler(
                new ModifyConnectionRequest(this));
        
        //transition to CLOSING
        fsm.createTransition(ConnectionTransition.CLOSE, 
                ConnectionState.HALF_OPEN, ConnectionState.CLOSING);
        
        
        
        //******************************************************************/
        //               STATE MODIFYING                                    /
        //******************************************************************/
        //transition to OPEN
        fsm.createTransition(
                ConnectionTransition.OPENED,
                ConnectionState.MODIFYING, ConnectionState.OPEN).setHandler(
                new SdpPortManagerEventImpl(sdpPortManager, SdpPortManagerEvent.ANSWER_PROCESSED));
        //transition to FAILED        
        fsm.createTransition(
                ConnectionTransition.FAILURE, 
                ConnectionState.MODIFYING, ConnectionState.FAILED);
        //transition to CLOSING
        fsm.createTransition(ConnectionTransition.CLOSE, 
                ConnectionState.MODIFYING, ConnectionState.CLOSING);

        fsm.createTimeoutTransition(
                ConnectionState.MODIFYING, ConnectionState.FAILED, 5000).setHandler(
                new TimeoutError(this));
        
        //******************************************************************/
        //               STATE CANCELED                                     /
        //******************************************************************/
        //transition to CLOSING upon OPENED signal
        fsm.createTransition(ConnectionTransition.OPENED, 
                ConnectionState.CANCELED, ConnectionState.CLOSING);
        
        //transition to CLOSING upon CREATED signal
        fsm.createTransition(ConnectionTransition.CREATED, 
                ConnectionState.CANCELED, ConnectionState.CLOSING);
        
        fsm.createTransition(ConnectionTransition.CLOSE, 
                ConnectionState.CANCELED, ConnectionState.CANCELED);
        
        //transition to CLOSING without signals
        fsm.createTimeoutTransition(
                ConnectionState.CANCELED, ConnectionState.INVALID, 5000);
        
        //******************************************************************/
        //               STATE CLOSING                                     /
        //******************************************************************/
        fsm.createTransition(ConnectionTransition.CLOSE, 
                ConnectionState.CLOSING, ConnectionState.CLOSING);
        
        //signal from server
        fsm.createTransition(ConnectionTransition.CLOSED, 
                ConnectionState.CLOSING, ConnectionState.INVALID);
        //failure signal from server (TODO: try to resent signal again)
        fsm.createTransition(ConnectionTransition.FAILURE, 
                ConnectionState.CLOSING, ConnectionState.INVALID);
        //no signals from server
        fsm.createTimeoutTransition(
                ConnectionState.CLOSING, ConnectionState.INVALID, 5000);
        
        //******************************************************************/
        //               STATE FAILED                                     /
        //******************************************************************/
        fsm.createTransition(ConnectionTransition.CLOSE, 
                ConnectionState.FAILED, ConnectionState.CLOSING);
        
      //******************************************************************/
        //               STATE INVALID                                     /
        //******************************************************************/
        fsm.createTransition(ConnectionTransition.CLOSE, 
                ConnectionState.INVALID, ConnectionState.INVALID);
    }
    
    protected ConnectionIdentifier getConnectionID() {
        return connectionID;
    }
    
    protected void setConnectionID(ConnectionIdentifier connectionID) {
        this.connectionID = connectionID;
    }
    
    
    public SdpPortManager getSdpPortManager() throws MsControlException {
        return sdpPortManager;
    }

    public void confirm() throws MsControlException {
        throw new MsControlException("Operation not yet supported");
    }

    public MediaConfig getConfig() {
        return this.config;
    }

    public <R> R getResource(Class<R> paramClass) throws MsControlException {
        // TODO Auto-generated method stub
        return null;
    }

    public void triggerRTC(Action paramAction) {
        // TODO Auto-generated method stub
    }


    public Iterator<MediaObject> getMediaObjects() {
        // TODO Auto-generated method stub
        return null;
    }

    public <T extends MediaObject> Iterator<T> getMediaObjects(Class<T> paramClass) {
        // TODO Auto-generated method stub
        return null;
    }


    public void release() {
        info("Releasing....");
        //send signal to all local connections
        try {
            this.unjoin();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        //send signal to network connection
        try {
            fsm.signal(ConnectionTransition.CLOSE);
        } catch (UnknownTransitionException e) {
        	e.printStackTrace();
        }
    }


    public void addListener(AllocationEventListener listener) {
    }

    public void removeListener(AllocationEventListener listener) {
        // TODO Auto-generated method stub
    }

    public void triggerAction(Action action) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
