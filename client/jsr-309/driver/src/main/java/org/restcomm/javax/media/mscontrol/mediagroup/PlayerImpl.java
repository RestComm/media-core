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
package org.restcomm.javax.media.mscontrol.mediagroup;


import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.Notify;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;
import jain.protocol.ip.mgcp.message.parms.RequestedAction;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.media.mscontrol.MediaErr;
import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.Qualifier;
import javax.media.mscontrol.UnsupportedException;
import javax.media.mscontrol.Value;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.PlayerEvent;
import javax.media.mscontrol.resource.RTC;

import org.restcomm.fsm.FSM;
import org.restcomm.fsm.Logger;
import org.restcomm.fsm.State;
import org.restcomm.fsm.StateEventHandler;
import org.restcomm.fsm.TransitionHandler;
import org.restcomm.fsm.UnknownTransitionException;
import org.restcomm.javax.media.mscontrol.spi.DriverImpl;
import org.restcomm.jsr309.mgcp.PackageAU;

/**
 * b
 * @author amit bhayani
 * 
 */
public class PlayerImpl implements Player, JainMgcpListener, Logger {

    public final static String STATE_NULL = "NULL";
    public final static String STATE_IDLE = "IDLE";
    public final static String STATE_ACTIVE = "ACTIVE";
    public final static String STATE_ACTIVATING = "ACTIVATING";
    public final static String STATE_PAUSED = "PAUSED";
    public final static String STATE_INVALID = "INVALID";
    
    public final static String SIGNAL_CREATE = "CREATE";
    public final static String SIGNAL_PLAY = "PLAY";
    public final static String SIGNAL_STARTED = "STARTED";
    public final static String SIGNAL_FAILED = "FAILED";
    public final static String SIGNAL_START_PAUSED = "START_PAUSED";
    public final static String SIGNAL_STOP = "STOP";
    public final static String SIGNAL_PAUSE = "PAUSE";
    public final static String SIGNAL_RESUME = "RESUME";
    public final static String SIGNAL_PLAY_COMPLETED = "PLAY_COMPLETED";
    public final static String SIGNAL_RELEASE = "RELEASE";
    
    protected MediaGroupImpl parent = null;
    private FSM fsm;
    
    protected CopyOnWriteArrayList<MediaEventListener<PlayerEvent>> listeners = new CopyOnWriteArrayList<MediaEventListener<PlayerEvent>>();
    
    protected String uri;
    private String params;
    private String returnCode;
    private Qualifier qualifier;
    
    private ReentrantLock lock = new ReentrantLock();
    private Condition started = lock.newCondition();

    private long startTime;
    private long timeError = 0;    
    private long stopTime;
    private long reqDuration;
    
    private ConcurrentLinkedQueue<PlayTask> playList = new ConcurrentLinkedQueue();
    
    private MgcpSender mgcpSender;
    
    //-------------------------------------------------------------------------------------------/
    
    protected PlayerImpl(MediaGroupImpl parent) throws MsControlException {
        this.parent = parent;
        mgcpSender=new MgcpSender();
        this.initFSM();
    }

    private void initFSM() {
        fsm = new FSM(parent.getMediaSession().getDriver().getScheduler());
        fsm.setLogger(this);
        
        fsm.createState(STATE_NULL);
        fsm.createState(STATE_IDLE);
        fsm.createState(STATE_ACTIVATING);
        fsm.createState(STATE_ACTIVE).setOnEnter(new OnStart());
        fsm.createState(STATE_PAUSED);
        fsm.createState(STATE_INVALID);
        
        fsm.setStart(STATE_NULL);
        fsm.setEnd(STATE_INVALID);
        
        //state NULL
        fsm.createTransition(SIGNAL_CREATE, STATE_NULL, STATE_IDLE);
        fsm.createTransition(SIGNAL_RELEASE, STATE_NULL, STATE_INVALID);
        
        //state IDLE
        //sending request to server and waiting response
        fsm.createTransition(SIGNAL_PLAY, STATE_IDLE, STATE_ACTIVATING).setHandler(new PlayRequest());
        //player started in suspended mode, nothing is sent to server in this case
        fsm.createTransition(SIGNAL_START_PAUSED, STATE_IDLE, STATE_PAUSED);
        //player was stopped during activation, ask to stop it now
        //TODO: Handle this transition!
        //fsm.createTransition(SIGNAL_STARTED, STATE_IDLE, STATE_IDLE).setHandler(new StopRequest());
        //Player release, silently go to INVALID state
        fsm.createTransition(SIGNAL_RELEASE, STATE_IDLE, STATE_INVALID);

        //state ACTIVATNG
        //server said - ok
        fsm.createTransition(SIGNAL_STARTED, STATE_ACTIVATING, STATE_ACTIVE);
        //server said follow to the hand. 
        fsm.createTransition(SIGNAL_FAILED, STATE_ACTIVATING, STATE_IDLE);
        //user asks to stop player         
        fsm.createTransition(SIGNAL_STOP, STATE_ACTIVATING, STATE_IDLE);
        //playback failure
        fsm.createTransition("FAILURE", STATE_ACTIVE, STATE_IDLE).setHandler(new PlayFailure(this)); 
        //Player has been released. Do not forget to notify server
        fsm.createTransition(SIGNAL_RELEASE, STATE_ACTIVATING, STATE_INVALID).setHandler(new StopRequest(this));
        
        //state ACTIVE
        //play finishes normaly
        fsm.createTransition(SIGNAL_PLAY_COMPLETED, STATE_ACTIVE, STATE_IDLE).setHandler(new PlayCompleted(this));
        //user asks to stop player
        fsm.createTransition(SIGNAL_STOP, STATE_ACTIVE, STATE_IDLE).setHandler(new StopRequest(this));
        //Player has been released. Do not forget to notify server
        fsm.createTransition(SIGNAL_RELEASE, STATE_ACTIVE, STATE_INVALID).setHandler(new StopRequest(this));

        //state PAUSED
        //user ask to stop player but it was in puased mode so no need to notify server
        fsm.createTransition(SIGNAL_STOP, STATE_PAUSED, STATE_IDLE);
        //this transition means that some trigger activated. do not send anything more to server
        fsm.createTransition(SIGNAL_RESUME, STATE_PAUSED, STATE_ACTIVE);
        //Player has been released.
        fsm.createTransition(SIGNAL_RELEASE, STATE_INVALID, STATE_INVALID);
        
        try {
            fsm.signal(SIGNAL_CREATE);
        } catch (UnknownTransitionException e) {
        }
        
    }
    


    // Player methods
    @SuppressWarnings("static-access")
    public void play(URI[] uris, RTC[] rtc, Parameters params) throws MsControlException {        
        if (rtc != null) {
            verifyRTC(rtc);
        }
        
        this.checkURI(uris);
        PlayTask task = new PlayTask(uris, rtc, params);
        
        //user calls play during the current work?
        if (fsm.getState().getName().equals(STATE_IDLE)) {
            playList.offer(task);
        } else {
            //no specific behaviour?
            if (params == null) {
                throw new MsControlException("Busy now: state=" + fsm.getState());
            }
            
            Value behaviour = (Value) params.get(Player.BEHAVIOUR_IF_BUSY);
            playList.offer(task);

            if (Player.FAIL_IF_BUSY.equals(behaviour)) {
                //queue task and send stop
                throw new MsControlException("Busy now");
            }
            
            if (Player.STOP_IF_BUSY.equals(behaviour)) {
                //queue task and send stop
                try {
                    fsm.signal(SIGNAL_STOP);
                } catch (UnknownTransitionException e) {
                }
            }
        }

        new Thread(new Starter()).start();
        
        lock.lock();
        try {
            try {
                started.await();
            } catch (InterruptedException e) {
            }
        } finally {
            lock.unlock();
        }        
    }

    @SuppressWarnings("static-access")
    public void play(URI uri, RTC[] rtc, Parameters params) throws MsControlException {
        play(new URI[]{uri}, rtc, params);
    }


    // Resource Methods
    public MediaGroup getContainer() {
        return this.parent;
    }

    // MediaEventNotifier methods
    public void addListener(MediaEventListener<PlayerEvent> listener) {
        this.listeners.add(listener);
    }

    public void removeListener(MediaEventListener<PlayerEvent> listener) {
        this.listeners.remove(listener);
    }

    public MediaSession getMediaSession() {
        return this.parent.getMediaSession();
    }

    protected void update(PlayerEvent anEvent) {
    }

    private void checkURI(URI[] uris) throws MsControlException {
        if (uris == null) {
            throw new MsControlException("URI[] cannot be null");
        }

        for (URI uri : uris) {

            if (uri == null) {
                throw new MsControlException("URI cannot be null");
            }

            if (uri.getScheme().equalsIgnoreCase("data")) {
                continue;
            }
        }
    }

    public void stop(boolean stopAll) {
        try {
            fsm.signal(SIGNAL_STOP);
        } catch (UnknownTransitionException e) {
        }
    }
    
    private void verifyRTC(RTC[] rtc) throws UnsupportedException {
        for (RTC r: rtc ) {
            if (r.getTrigger() == Player.PLAY_START && r.getAction() == Player.STOP) {
                throw new UnsupportedException("Invalid RTC");
            }
        }
    }
    
    protected void fireEvent(PlayerEvent event) {
        new Thread(new EventSender(event)).start();
    }
    
    
    private String createParams(URI[] uris, Parameters params) {
        StringBuilder buff = new StringBuilder();
        
        //add URI list of the player
        if (uris.length == 1) {
            buff.append("an=").append(uris[0].toString());
        } else {
            buff.append("an=").append(uris[0].toString());
            for (int i = 1; i < uris.length; i++) {
                buff.append(";");
                buff.append(uris[i].toString());
            }
        }
        
        this.qualifier = PlayerEvent.END_OF_PLAY_LIST;
        
        //max duraion if specified
        if (params != null && params.containsKey(Player.MAX_DURATION)) {
            long du = ((Integer)params.get(Player.MAX_DURATION));
            buff.append(" ");
            buff.append("du=").append(du);
            
            if (du > 0) {
                this.qualifier = PlayerEvent.DURATION_EXCEEDED;
            }
            
            this.reqDuration = du;
        }

        //start offset if specified
        if (params != null && params.containsKey(Player.START_OFFSET)) {
            buff.append(" ");
            buff.append("of=").append(params.get(Player.START_OFFSET));
        }
        
        //iterations if specified
        if (params != null && params.containsKey(Player.REPEAT_COUNT)) {
            int it = 0;
            
            if (params.get(Player.REPEAT_COUNT) instanceof Integer) {
                try {
                    it = ((Integer)params.get(Player.REPEAT_COUNT));
                } catch (Exception e) {
                }
            }
            
            if (it < 0) it = 0;
            buff.append(" ");
            buff.append("it=").append(it);
        }

        //intervals if specified
        if (params != null && params.containsKey(Player.INTERVAL)) {
            buff.append(" ");
            buff.append("iv=").append(params.get(Player.INTERVAL));
        }
        
        return buff.toString();
    }
    
    private void requestAnnouncement() {
        //generate request identifier and transaction ID
        RequestIdentifier reqID = parent.nextRequestID();        
        int txID = parent.getMediaSession().getDriver().getNextTxID();
        
        //constructs request
        NotificationRequest req = new NotificationRequest(this, parent.getEndpoint().getIdentifier(), reqID);
        
        RequestedAction[] actions = new RequestedAction[] { RequestedAction.NotifyImmediately };

        ArrayList<EventName> signalList = new ArrayList();
        ArrayList<RequestedEvent> eventList = new ArrayList();

        //player signals     
        signalList.add(new EventName(PackageAU.Name, PackageAU.pa.withParm(params)));

        //player events
        eventList.add(new RequestedEvent(new EventName(PackageAU.Name, MgcpEvent.oc), actions));
        eventList.add(new RequestedEvent(new EventName(PackageAU.Name, MgcpEvent.of), actions));
        
        EventName[] signals = new EventName[signalList.size()];        
        signalList.toArray(signals);
        
        RequestedEvent[] events = new RequestedEvent[eventList.size()];
        eventList.toArray(events);
        
        //new EventName(auPackageName, MgcpEvent.oc, connId)        
        req.setRequestedEvents(events);
        req.setSignalRequests(signals);
        
        req.setTransactionHandle(txID);
        
        DriverImpl driver=parent.getMediaSession().getDriver();
        
        req.setNotifiedEntity(driver.getCallAgent());

        driver.attach(txID, this);
        driver.attach(reqID, this);
        
        if(this.parent.isStopping())
        	mgcpSender.init(driver, req);
        else
        	driver.send(req);        
    }

    private void requestStop() {
        //generate request identifier and transaction ID
        RequestIdentifier reqID = parent.nextRequestID();        
        int txID = parent.getMediaSession().getDriver().getNextTxID();
        
        //constructs request
        NotificationRequest req = new NotificationRequest(this, parent.getEndpoint().getIdentifier(), reqID);
        
        req.setTransactionHandle(txID);
        
        DriverImpl driver=parent.getMediaSession().getDriver();
        
        req.setNotifiedEntity(driver.getCallAgent());

        driver.attach(txID, this);
        driver.attach(reqID, this);
                  
        driver.send(req);
    }
    
    /**
     * Fires event.
     * 
     * @param evt the event to be fired
     */
    private void fireEvent(EventName eventName) {
        this.returnCode = eventName.getEventIdentifier().getParms();
        switch (eventName.getEventIdentifier().intValue()) {
            case MgcpEvent.REPORT_ON_COMPLETION :
                try {
                    fsm.signal(SIGNAL_PLAY_COMPLETED);
                } catch (UnknownTransitionException e) {
                    e.printStackTrace();
                }
                break;
            case MgcpEvent.REPORT_FAILURE :
                try {
                    fsm.signal("FAILURE");
                } catch (UnknownTransitionException e) {
                    e.printStackTrace();
                }
                break;            
        }
        
    }
    
    public void stopCompleted()
    {
    	if(mgcpSender.waiting)
    		mgcpSender.run();
    }
    
    private class PlayRequest implements TransitionHandler {

        public void process(State state) {
            requestAnnouncement();
        }
        
    }

    private class PlayCompleted implements TransitionHandler {
        private PlayerImpl player;
        
        protected PlayCompleted(PlayerImpl player) {
            this.player = player;
        }
        
        public void process(State state) {
            stopTime = System.currentTimeMillis();
            if (Math.abs(stopTime - startTime - reqDuration) > 1000) {
                qualifier = PlayerEvent.END_OF_PLAY_LIST;
            }
            
            PlayerEventImpl evt = new PlayerEventImpl(player, PlayerEvent.PLAY_COMPLETED, true, qualifier, null);
            evt.setOffset((int)(stopTime - startTime));
            fireEvent(evt);
            
            if (!playList.isEmpty()) {
                new Thread(new Starter()).start();
            }
        }       
    }

    private class PlayFailure implements TransitionHandler {
        private PlayerImpl player;
        
        protected PlayFailure(PlayerImpl player) {
            this.player = player;
        }
        
        public void process(State state) {
            MediaErr error = null;
            
            if (returnCode.equals("rc=301")) {
                error = MediaErr.BAD_ARG;
            } else if (returnCode.equals("rc=312")) {
                error = MediaErr.NOT_FOUND;
            }
            
            stopTime = System.currentTimeMillis();
            PlayerEventImpl evt = new PlayerEventImpl(player, PlayerEvent.PLAY_COMPLETED, false, PlayerEvent.NO_QUALIFIER, null, error, "");
            evt.setOffset((int)(stopTime - startTime));
            fireEvent(evt);
            
            if (!playList.isEmpty()) {
                new Thread(new Starter()).start();
            }
        }
        
    }
    
    private class StopRequest implements TransitionHandler {
        private PlayerImpl player;
        
        protected StopRequest(PlayerImpl player) {
            this.player = player;
        }

        public void process(State state) {
            requestStop();
            PlayerEventImpl evt = new PlayerEventImpl(player, PlayerEvent.PLAY_COMPLETED, true, PlayerEvent.STOPPED, null);
            evt.setOffset((int)(stopTime - startTime));
            fireEvent(evt);
        }        
    }    
    
    private class OnStart implements StateEventHandler {

        public void onEvent(State state) {
            startTime = System.currentTimeMillis() + timeError;
            //notify that player started
            lock.lock();
            try {
                started.signal();
            } finally {
                lock.unlock();
            }
        }
        
    }

    public void processMgcpCommandEvent(JainMgcpCommandEvent event) {
        switch (event.getObjectIdentifier()) {
            case Constants.CMD_NOTIFY :
                Notify notify = (Notify) event;
                
                EventName[] events = notify.getObservedEvents();
                for (EventName evt: events) {
                    fireEvent(evt);
                }
                
                break;
            default :
                return;
        }
    }

    public void processMgcpResponseEvent(JainMgcpResponseEvent event) {
        switch (event.getReturnCode().getValue()) {
            case ReturnCode.TRANSACTION_BEING_EXECUTED :
                break;
            case ReturnCode.TRANSACTION_EXECUTED_NORMALLY :
                try {
                    fsm.signal(SIGNAL_STARTED);
                } catch (UnknownTransitionException e) {
                }
                break;
            default :
                try {
                    fsm.signal(SIGNAL_FAILED);
                } catch (UnknownTransitionException e) {                	
                }
        }
    }
    
    private class PlayTask {
        private URI[] uris;
        private RTC[] rtc;
        private Parameters params;
        
        protected PlayTask(URI[] uris, RTC[] rtc, Parameters params) {
            this.uris = uris;
            this.rtc = rtc;
            this.params = params;
        }
    }
    
    private class Starter implements Runnable {

        public void run() {
            PlayTask task = playList.poll();
            params = createParams(task.uris, task.params);
            
            try {
            	fsm.signal(SIGNAL_PLAY);
            } catch (UnknownTransitionException e) {
            }
        }
        
    }

    private class EventSender implements Runnable {
        
        private PlayerEvent evt;
        
        public EventSender(PlayerEvent evt) {
            this.evt = evt;
        }
        
        public void run() {
        	//needed to disable Busy Now Errors.
        	try{ 
                Thread.sleep(2);
             } catch( InterruptedException e ) {                 
             }

             
            for (MediaEventListener l : listeners) {
                l.onEvent(evt);
            }
        }
    }
    
    private class MgcpSender {
    	
    	private DriverImpl driver;
    	private NotificationRequest req;
    	private Boolean waiting=false;
    	
    	public MgcpSender()
    	{
    	}
    	
    	public void init(DriverImpl driver,NotificationRequest req)
    	{
    		this.driver=driver;
    		this.req=req;
    		waiting=true;
    	}
    	
        public void run() {
        	driver.send(req);
        	waiting=false;
        }
    }
    
    public void info(String s) {
        parent.info(s);
    }

    public void debug(String s) {
        parent.debug(s);
    }

    public void warn(String s) {
        parent.warn(s);
    }    
}
