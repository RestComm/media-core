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
package org.mobicents.media.server.ctrl.mgcp.evt;

import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.RequestedAction;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;

import org.apache.log4j.Logger;
import org.mobicents.media.Component;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.ctrl.mgcp.Request;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.NotificationListener;
import org.mobicents.media.server.spi.events.NotifyEvent;

/**
 * @author amit bhayani
 * @author kulikov
 */
public abstract class EventDetector implements NotificationListener {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MgcpPackage mgcpPackage;
    private String eventName;
    private int eventID;
    private Endpoint endpoint;
    private Connection connection;
    private RequestedAction[] actions;
    protected String params;
    protected Component component;
    private ConnectionIdentifier connectionIdentifier = null;
    private Request request;
    private Logger logger = Logger.getLogger(EventDetector.class);

    protected Class _interface = null;
    protected MediaType mediaType = null;
    
    public EventDetector(MgcpPackage mgcpPackage, String eventName, int eventID, String params,
            RequestedAction[] actions) {
        this.mgcpPackage = mgcpPackage;
        this.eventName = eventName;
        this.eventID = eventID;
        this.params = params;
        this.actions = actions;
    }

    public void setConnectionIdentifier(ConnectionIdentifier connectionIdentifier) {
        this.connectionIdentifier = connectionIdentifier;
    }

    public int getEventID() {
        return eventID;
    }

    public void setEventID(int eventID) {
        this.eventID = eventID;
    }

    public RequestedAction[] getActions() {
        return actions;
    }

    public Connection getConnection() {
        return connection;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setPackage(MgcpPackage mgcpPackage) {
        this.mgcpPackage = mgcpPackage;
    }

    //TODO Probably its better to keep reference to original EventName rather than re-constructing?
    public EventName getEventName() {
        if (this.connectionIdentifier != null) {
            return new EventName(PackageName.factory(this.mgcpPackage.getName()), MgcpEvent.factory(eventName), connectionIdentifier);
        }
        return new EventName(PackageName.factory(this.mgcpPackage.getName()), MgcpEvent.factory(eventName));
    }
    
    public RequestedEvent getRequestedEvent() {
    	RequestedEvent requestedEvent = null;
    	
    	MgcpEvent mgcpEvent = MgcpEvent.factory(eventName);
    	
    	if(this.params != null){
    		mgcpEvent = mgcpEvent.withParm(this.params);
    	}
    	
    	EventName eventNameTemp = null;
    	
    	if (this.connectionIdentifier != null) {
    		eventNameTemp = new EventName(PackageName.factory(this.mgcpPackage.getName()), mgcpEvent, connectionIdentifier);
        } else { 
        	eventNameTemp = new EventName(PackageName.factory(this.mgcpPackage.getName()), mgcpEvent);
        }
        
        if(this.actions != null && this.actions.length > 0){
        	requestedEvent = new RequestedEvent(eventNameTemp, this.actions);
        } else{
        	requestedEvent = new RequestedEvent(eventNameTemp, new RequestedAction[]{RequestedAction.NotifyImmediately});
        }
        
        return requestedEvent;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public Request getRequest() {
        return request;
    }

    public boolean verify(Connection connection) {
        this.connection = connection;
        return this.doVerify(connection);
    }

    public boolean verify(Endpoint endpoint) {
        this.endpoint = endpoint;
        return this.doVerify(endpoint);
    }

    protected boolean doVerify(Connection connection) {
        //Do we detect on anything else than sink ?
    	Component componentWhichProvideInterface = (Component) connection.getComponent(mediaType,_interface);
        if(componentWhichProvideInterface!=null)
        {
        	this.component = (Component) componentWhichProvideInterface.getInterface(_interface);
        	return true;
        }else
        {
        	return false;
        }
       
    }

    protected boolean doVerify(Endpoint endpoint) {
    	//Do we detect on anything else than sink ?
        MediaSink sink = endpoint.getSink(mediaType);
        if(sink!=null)
        {
        	//FIXME: should we extends comp
        	component = (Component) sink.getInterface(_interface);
        	
            return component != null;
        }else
        {
        	//lets check source
        	 MediaSource source = endpoint.getSource(mediaType);
             if(source!=null)
             {

             	component = (Component) source.getInterface(_interface);
             	
                 return component != null;
             }
        	return false;
        }
        
    }

    public void start() {
        component.addListener(this);        
        //FIXME: add start if not started?
    }

    public void stop() {
        component.removeListener(this);
    	//FIXME: add stop if started?
    }

    public void update(NotifyEvent event) {

        if (event.getEventID() == this.eventID) {
            for (RequestedAction action : actions) {
                performAction(event, action);
            }
        }
    }

    public abstract void performAction(NotifyEvent event, RequestedAction action);

	public void setDetectorInterface(Class detectorInterface) {
		this._interface = detectorInterface;
		
	}

	public void setMediaType(MediaType mediaType) {
		this.mediaType = mediaType;
		
	}
}
