package org.mobicents.javax.media.mscontrol.container;

import org.mobicents.javax.media.mscontrol.*;

import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import java.util.concurrent.locks.ReentrantLock;
import javax.media.mscontrol.MediaErr;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.JoinEvent;
import javax.media.mscontrol.join.JoinEventListener;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.JoinableContainer;
import javax.media.mscontrol.join.JoinableStream;
import javax.media.mscontrol.join.JoinableStream.StreamType;
import org.mobicents.fsm.UnknownTransitionException;


/**
 * 
 * @author amit bhayani
 * @author kulikov
 */
public abstract class ContainerImpl extends MediaObjectImpl implements JoinableContainer, LinkListener {

    protected MediaSessionImpl session = null;
    
    private CopyOnWriteArrayList<JoinEventListener> listeners = new CopyOnWriteArrayList<JoinEventListener>();
    
    protected MediaStreamImpl[] streams = new MediaStreamImpl[2];
    private ArrayList<Link> links = new ArrayList();
    private ArrayList<Link> incomingLinks = new ArrayList();
    protected MediaConfigImpl config;
    protected int maxJoinees = 1;

    //protected EndpointIdentifier endpointName;
    protected Endpoint endpoint;
    
    //use for implementing synchronous calls
    protected InvocationLock invocationLock = new InvocationLock();
    
    protected ReentrantLock lock = new ReentrantLock();
    protected Direction direction;
    
    public ContainerImpl(MediaSessionImpl session, Parameters parameters) throws MsControlException {
        super(session, session.getDriver(), parameters);
        this.session = session;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }
    
    public void setConcreteName(EndpointIdentifier endpoint) {
        this.endpoint.setConcreteName(endpoint);
    }
    
    public MediaSessionImpl getMediaSession() {
        return session;
    }

    public JoinableStream getJoinableStream(StreamType value) throws MsControlException {
        for (MediaStreamImpl s : streams) {
            if (s.getType().equals(value)) {
                return s;
            }
        }
        throw new MsControlException("Stream of type " + value + " is not supported");
    }

    public JoinableStream[] getJoinableStreams() throws MsControlException {
        return streams;
    }

    public Joinable[] getJoinees() throws MsControlException {
        int i = 0;
        Joinable[] joinees = new Joinable[links.size() + incomingLinks.size()];
        
        for (Link link : links) {
            joinees[i++] = link.getContainer(1);
        }
        
        for (Link link : incomingLinks) {
            joinees[i++] = link.getContainer(0);
        }
        
        return  joinees;
    }

    public Joinable[] getJoinees(Direction direction) throws MsControlException {
        int i = 0;
        Joinable[] joinees = new Joinable[links.size() + incomingLinks.size()];
        for (Link link : links) {
            if (link.direction.equals(direction)) {
                joinees[i++] = link.getContainer(1);
            }
        }
        
        Direction inverseDirection=inversion(direction);
        for (Link link : incomingLinks) {
            if (link.direction.equals(inverseDirection)) {
                joinees[i++] = link.getContainer(0);
            }
        }
        
        return  joinees;
    }

    public synchronized void addIncomingLink(Link toAdd)
    {
    	if(toAdd.getContainer(1) != this)
    		return;
    	
    	 for (Link l : links) {
             if (l.getContainer(1) == toAdd.getContainer(0)) {
                 return;
             }
         }
    	 
    	for (Link l : incomingLinks) {
            if (l.getContainer(0) == toAdd.getContainer(0)) {
                return;
            }
        }
    	
    	incomingLinks.add(toAdd);
    }
    
    public synchronized void removeIncomingLink(Link toRemove)
    {
    	incomingLinks.remove(toRemove);    	
    }
    
    public void join(Direction direction, Joinable other) throws MsControlException {
        joinInitiate(direction, other, null);
        invocationLock.lock(5000);
    }


    public synchronized void joinInitiate(Direction direction, Joinable other, Serializable context) throws MsControlException {
        Link link = null;
        Direction oldDirection=null;
        for (Link l : links) {
            if (l.getContainer(1) == other) {
            	oldDirection=l.direction;
                link = l;
                link.direction = direction;
                link.context = context;
                break;
            }
        }
        
        if (link == null) {
        	link = new Link(session.getDriver().getScheduler(), this, (ContainerImpl)other);
            
            link.direction = direction;
            link.context = context;
            link.setListener(this);
            
            links.add(link);
            ((ContainerImpl)other).addIncomingLink(link);
        }
        else if(direction==Direction.DUPLEX && oldDirection==Direction.DUPLEX)
        {
        	//join in send case
        	for (Link l : links) {
                if (l.getContainer(1) != other) {                    
                    l.direction = Direction.SEND;
                    try {            
                        l.signal("join");
                    } catch (UnknownTransitionException e) {
                        throw new MsControlException("Wrong state");
                    }
                }
            }
        }
        
        try {            
            link.signal("join");
        } catch (UnknownTransitionException e) {
            throw new MsControlException("Wrong state");
        }
    }

    private boolean hasLink(ContainerImpl other) {
        for (Link l : links) {
            if (l.getContainer(1) == other) {
                return true;
            }
        }
        return false;
    }
    
    protected Link getLink(Direction direction, ContainerImpl other) {
        Link link = null;
        
        for (Link l : links) {
            if (l.getContainer(1) == other) {
                link = l;
                break;
            }
        }
        
        if (link == null) {
            link = new Link(session.getDriver().getScheduler(), this, (ContainerImpl)other);
            
            link.direction = direction;
            link.setListener(this);
            
            links.add(link);
        }
        
        return link;
    }
    
    public void unjoin(Joinable other) throws MsControlException {
        debug(String.format("Synchronous unjoin start, other party = %s", ((MediaObjectImpl)other).getObjectID()));
        if (this.hasLink((ContainerImpl)other)) {
            unjoinInitiate(other, null);            
            invocationLock.lock(5000);
        } else {
            other.unjoin(this);
        }
        debug(String.format("Synchronous unjoin completed, other party = %s", ((MediaObjectImpl)other).getObjectID()));
    }

    public synchronized void unjoinInitiate(Joinable other, Serializable context) throws MsControlException {
        info(String.format("Initiation of unjoin procedure, other party = %s", ((MediaObjectImpl)other).getObjectID()));
        
        if (((ContainerImpl)other).hasLink(this)) {
            other.unjoinInitiate(this, context);
            return;
        }
        
    
        Link link = null;
        
        for (Link l : links) {
            if (l.getContainer(1) == other) {
                link = l;
                break;
            }
        }
                
        if (link == null) {
            return;
        }
        
        links.remove(link);
        ((ContainerImpl)other).removeIncomingLink(link);
        
        try {
            link.signal("release");
        } catch (UnknownTransitionException e) {
            throw new MsControlException("Illegal state");
        }
        
    }

    private Direction inversion(Direction direction) {
        switch (direction) {
            case SEND :
                return Direction.RECV;
            case RECV :
                return Direction.SEND;
            default :
                return Direction.DUPLEX;
        }
    }
    
    public void addListener(JoinEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(JoinEventListener listener) {
        listeners.remove(listener);
    }

    protected void fire(JoinEvent event) {
        new Thread(new EventHandler(event)).start();
    }

    protected ContainerImpl getOwner() {
        return this;
    }
    
    @Override
    public String toString() {
        return this.getURI().toString();
    }
    
    protected void unjoin() throws MsControlException {
        Link[] list = new Link[links.size()];
        links.toArray(list);
        for (Link link : list) {
            try {
                link.signal("release");
            } catch (UnknownTransitionException e) {
            }
        }
        
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
        }
    }
    
    public void joined(Link link) {
        JoinEventImpl evt = new JoinEventImpl(this, link.context, 
                        link.getContainer(1), JoinEvent.JOINED, true, 
                        MediaErr.NO_ERROR, null);
        fire(evt);
        invocationLock.release();
    }
    
    public void unjoined(Link link) {
        JoinEventImpl evt = new JoinEventImpl(this, link.context, 
                        link.getContainer(1), JoinEvent.UNJOINED, true, 
                        MediaErr.NO_ERROR, null);
        invocationLock.release();
        fire(evt);
    }
    
    private class EventHandler implements Runnable {
        
        private JoinEvent event;
        
        public EventHandler(JoinEvent event) {
            this.event = event;
        }
        
        public void run() {
            for (JoinEventListener s : listeners) {
                s.onEvent(event);
            }
        }
    }
}
