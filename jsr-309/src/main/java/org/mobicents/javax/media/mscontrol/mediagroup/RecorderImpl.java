package org.mobicents.javax.media.mscontrol.mediagroup;

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
import java.util.concurrent.CopyOnWriteArrayList;
import javax.media.mscontrol.MediaErr;

import javax.media.mscontrol.join.JoinException;
import javax.media.mscontrol.MediaEvent;
import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.UnsupportedException;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.Recorder;
import javax.media.mscontrol.mediagroup.RecorderEvent;
import javax.media.mscontrol.mediagroup.SpeechDetectorConstants;
import javax.media.mscontrol.mediagroup.signals.SignalDetector;
import javax.media.mscontrol.resource.RTC;
import javax.media.mscontrol.resource.Resource;
import javax.media.mscontrol.resource.Trigger;

import org.mobicents.fsm.FSM;
import org.mobicents.fsm.State;
import org.mobicents.fsm.StateEventHandler;
import org.mobicents.fsm.TransitionHandler;
import org.mobicents.fsm.UnknownTransitionException;
import org.mobicents.javax.media.mscontrol.mediagroup.signals.Options;
import org.mobicents.jsr309.mgcp.PackageAU;

/**
 * 
 * @author amit bhayani
 * @author kulikov
 * @author Jose Antonio Santos Cadenas
 */
public class RecorderImpl implements Recorder, JainMgcpListener {

    public final static String STATE_IDLE = "IDLE";
    public final static String STATE_ACTIVATING = "ACTIVATING";
    public final static String STATE_ACTIVE = "ACTIVE";
    public final static String STATE_PAUSED = "PAUSED";
    public final static String STATE_STOPPING = "STOPPING";
    
    public final static String SIGNAL_START = "start";
    public final static String SIGNAL_START_PAUSED = "start_paused";
    public final static String SIGNAL_STOP = "stop";
    public final static String SIGNAL_PAUSE = "pause";
    public final static String SIGNAL_RESUME = "resume";
    public final static String SIGNAL_SUCCESS = "success";
    public final static String SIGNAL_FAILURE = "failure";
    public final static String SIGNAL_COMPLETE = "complete";
    
    protected MediaGroupImpl parent = null;
    private FSM fsm;
    private String params;
    protected CopyOnWriteArrayList<MediaEventListener<? extends MediaEvent<?>>> listeners = new CopyOnWriteArrayList<MediaEventListener<? extends MediaEvent<?>>>();

    private RecorderEventImpl recorderEvent;
    
    private boolean rtcTriggered = false;
    private ArrayList<Trigger> triggers = new ArrayList();
    
    public RecorderImpl(MediaGroupImpl mediaGroup) {
        this.parent = mediaGroup;

        initFSM();
    }

    private void initFSM() {
        fsm = new FSM(parent.getMediaSession().getDriver().getScheduler());

        fsm.createState(STATE_IDLE);
        fsm.createState(STATE_ACTIVATING);
        fsm.createState(STATE_ACTIVE);
        fsm.createState(STATE_PAUSED);
        fsm.createState(STATE_STOPPING).setOnEnter(new StopRequest());

        fsm.setStart(STATE_IDLE);
        fsm.setEnd(STATE_IDLE);

        //state IDLE
        fsm.createTransition(SIGNAL_START, STATE_IDLE, STATE_ACTIVATING).setHandler(new RecordRequest());
        fsm.createTransition(SIGNAL_START_PAUSED, STATE_IDLE, STATE_PAUSED);

        fsm.createTransition(SIGNAL_SUCCESS, STATE_ACTIVATING, STATE_ACTIVE);
        //server said follow to the hand. 
        fsm.createTransition(SIGNAL_FAILURE, STATE_ACTIVATING, STATE_IDLE).setHandler(new CompleteNotify());
        //user has asked to stop player 
        fsm.createTransition(SIGNAL_STOP, STATE_ACTIVATING, STATE_IDLE);
        
        //state ACTIVE
        fsm.createTransition(SIGNAL_STOP, STATE_ACTIVE, STATE_STOPPING);
        fsm.createTransition(SIGNAL_PAUSE, STATE_ACTIVE, STATE_PAUSED);
        fsm.createTransition(SIGNAL_COMPLETE, STATE_ACTIVE, STATE_IDLE).setHandler(new CompleteNotify());
        fsm.createTransition(SIGNAL_FAILURE, STATE_ACTIVE, STATE_IDLE);

        //state PAUSED
        fsm.createTransition(SIGNAL_STOP, STATE_PAUSED, STATE_IDLE);
        fsm.createTransition(SIGNAL_RESUME, STATE_PAUSED, STATE_ACTIVE);

        //state STOPPING
        fsm.createTransition(SIGNAL_SUCCESS, STATE_STOPPING, STATE_IDLE).setHandler(new StoppedNotify());
    }

    public void record(URI streamID, RTC[] rtc, Parameters optargs) throws MsControlException {
        if (rtc != null) {
            verifyRTC(rtc);
        }
        
        if (parent.getJoinees(Direction.RECV).length == 0) {
        	throw new JoinException(this.parent.getURI()
                    + " Container is not joined to any other container");
        }

        String[] patterns = this.getPatterns(rtc, optargs);
        
        Options options = new Options();
        options.setRecordID(streamID.toString());
        
        //patterns
        if (patterns != null) {
            options.setDigitPattern(patterns);
        }
        
        //check for max duration
        if (optargs != null && optargs.containsKey(Recorder.MAX_DURATION)) {
            options.setRecordDuraion((Integer)optargs.get(Recorder.MAX_DURATION));
        }
        
        //check for append
        if (optargs != null && optargs.containsKey(Recorder.APPEND)) {
            options.setOverride(!(Boolean)optargs.get(Recorder.APPEND));
        }
        
        //post-speech time
        if (optargs != null && optargs.containsKey(Recorder.SILENCE_TERMINATION_ON)) {
            options.setSilenceTermination((Boolean)optargs.get(Recorder.SILENCE_TERMINATION_ON));
        }

        //final timeout
        if (optargs != null && optargs.containsKey(SpeechDetectorConstants.FINAL_TIMEOUT)) {
            options.setSilenceTermination(false);
            if (optargs.get(SpeechDetectorConstants.FINAL_TIMEOUT) != Resource.FOR_EVER) {
                options.setPostSpeechTimer((Integer)optargs.get(SpeechDetectorConstants.FINAL_TIMEOUT));
            }
        }
        
        Boolean hasPrompt=false;
        //initial prompt
        if (optargs != null && optargs.containsKey(Recorder.PROMPT)) {
        	hasPrompt=true;
            options.setPrompt(((URI)optargs.get(Recorder.PROMPT)).toString());
        }
        
        options.setNonInterruptiblePlay(true);
        if (rtc != null) {
            System.out.println("-------------------------------------");
            System.out.println("triggers=" + rtc.length);
            for (int i = 0; i < rtc.length; i++) {
                System.out.println("Trigger = " + rtc[i].getTrigger());
                if (rtc[i] == MediaGroup.SIGDET_STOPPLAY) {
                    options.setNonInterruptiblePlay(false);
                } else if (rtc[i].getTrigger() == SignalDetector.DETECTION_OF_ONE_SIGNAL) {                                                           
                    if(rtc[i].getAction()==Recorder.STOP && hasPrompt)
                    	//add one digit , first should not be counted
                    	options.setDigitsNumber(2);
                    else
                    	options.setDigitsNumber(1);
                    
                } else if (rtc[i].getTrigger() == Player.PLAY_START && rtc[i].getAction() == SignalDetector.FLUSH_BUFFER) {
                	options.setClearDigits(true);                    
                }
            }
        }
        
        
        params = options.toString();
        try {
            fsm.signal(SIGNAL_START);
        } catch (UnknownTransitionException e) {
            throw new MsControlException(e.getMessage());
        }
    }

    private void verifyRTC(RTC[] rtc) throws UnsupportedException {
        for (RTC r: rtc ) {
            if (r.getTrigger() == Player.PLAY_START && r.getAction() == Player.STOP) {
                throw new UnsupportedException("Invalid RTC");
            }
        }
    }
    
    private String[] getPatterns(RTC[] rtc, Parameters options) {
        if (rtc == null || options == null) {
            return null;
        }
        
        ArrayList<String> list = new ArrayList();
        for (RTC r : rtc) {
            for (int i = 0; i < SignalDetector.PATTERN_MATCH.length; i++) {
                if (r.getTrigger() == SignalDetector.PATTERN_MATCH[i]) {
                    if (options.containsKey(SignalDetector.PATTERN[i])) {
                        String s = (String) options.get(SignalDetector.PATTERN[i]);
                        String pattern = "";

                        //now hack for TCK: test_2_1_9_7_MultipleReturnKeys
                        for (int k = 0; k < s.length() - 1; k++) {
                            pattern += (s.charAt(k) + "|");
                        }

                        pattern += s.charAt(s.length() - 1);
                        
                        list.add(pattern);
                        triggers.add(SignalDetector.PATTERN_MATCH[i]);
                    }
                }
            }
        }
        
        if (list.isEmpty()) {
            return null;
        }
        
        String[] patterns = new String[list.size()];
        list.toArray(patterns);
        
        this.rtcTriggered = true;
        return patterns;
    }
    
    public MediaGroup getContainer() {
        return this.parent;
    }

    public void stop() {
        try {
            fsm.signal(SIGNAL_STOP);
        } catch (UnknownTransitionException e) {
        }
    }

    public void addListener(MediaEventListener<RecorderEvent> listener) {
        this.listeners.add(listener);
    }

    public MediaSession getMediaSession() {
        return this.parent.getMediaSession();
    }

    public void removeListener(MediaEventListener<RecorderEvent> listener) {
        this.listeners.remove(listener);
    }

    protected void update(RecorderEvent anEvent) {
        for (MediaEventListener m : listeners) {
            m.onEvent(anEvent);
        }
    }

    private void requestRecording() {
        // generate request identifier and transaction ID
        RequestIdentifier reqID = parent.nextRequestID();
        int txID = parent.getMediaSession().getDriver().getNextTxID();

        RequestedAction[] actions = new RequestedAction[] { RequestedAction.NotifyImmediately };
        
        // constructs request
        NotificationRequest req = new NotificationRequest(this, parent.getEndpoint().getIdentifier(), reqID);

        ArrayList<EventName> signalList = new ArrayList<EventName>();
        ArrayList<RequestedEvent> eventList = new ArrayList();

        signalList.add(new EventName(PackageAU.Name, PackageAU.pr.withParm(params)));

        EventName[] signals = new EventName[signalList.size()];
        signalList.toArray(signals);

        //player events
        eventList.add(new RequestedEvent(new EventName(PackageAU.Name, MgcpEvent.oc), actions));
        eventList.add(new RequestedEvent(new EventName(PackageAU.Name, MgcpEvent.of), actions));
        
        RequestedEvent[] events = new RequestedEvent[eventList.size()];
        eventList.toArray(events);
        
        req.setRequestedEvents(events);
        req.setSignalRequests(signals);

        req.setTransactionHandle(txID);
        req.setNotifiedEntity(parent.getMediaSession().getDriver().getCallAgent());

        parent.getMediaSession().getDriver().attach(txID, this);
        parent.getMediaSession().getDriver().attach(reqID, this);

        parent.getMediaSession().getDriver().send(req);
    }

    private void stopRecording() {
        // generate request identifier and transaction ID
        RequestIdentifier reqID = parent.nextRequestID();
        int txID = parent.getMediaSession().getDriver().getNextTxID();

        // constructs request
        NotificationRequest req = new NotificationRequest(this, parent.getEndpoint().getIdentifier(), reqID);

        req.setTransactionHandle(txID);
        req.setNotifiedEntity(parent.getMediaSession().getDriver().getCallAgent());

        parent.getMediaSession().getDriver().attach(txID, this);
        parent.getMediaSession().getDriver().attach(reqID, this);

        parent.getMediaSession().getDriver().send(req);
    }


    private void signal(String signal) {
        try {
            fsm.signal(signal);
        } catch (UnknownTransitionException e) {
        }
    }
    
    private void fireEvent(RecorderEventImpl evt) {
        new Thread(new EventSender(evt)).start();
    }
    
    /**
     * Fires event.
     * 
     * @param evt the event to be fired
     */
    private void fireEvent(EventName eventName) {        
        switch (eventName.getEventIdentifier().intValue()) {
            case MgcpEvent.REPORT_ON_COMPLETION :
                //parse options
                Options options = new Options(eventName.getEventIdentifier().getParms());
                switch (options.getReturnCode()) {
                    case 100 :                    	
                    	if (options.getPatternIndex() >= 0) {
                            recorderEvent = new RecorderEventImpl(this, RecorderEvent.RECORD_COMPLETED, true, RecorderEvent.RTC_TRIGGERED, triggers.get(options.getPatternIndex()), 0);
                        } else if (options.getDigitsCollected() != null) {
                            recorderEvent = new RecorderEventImpl(this, RecorderEvent.RECORD_COMPLETED, true, RecorderEvent.RTC_TRIGGERED, SignalDetector.DETECTION_OF_ONE_SIGNAL, 0);
                        } else {
                            recorderEvent = new RecorderEventImpl(this, RecorderEvent.RECORD_COMPLETED, true, RecorderEvent.NO_QUALIFIER, null, 0);
                        } 
                        signal(SIGNAL_COMPLETE);
                        break;
                    case 328 :
                        recorderEvent = new RecorderEventImpl(this, RecorderEvent.RECORD_COMPLETED, true, RecorderEvent.DURATION_EXCEEDED, null, 0);
                        signal(SIGNAL_COMPLETE);
                        break;
                    case 327 :
                        recorderEvent = new RecorderEventImpl(this, RecorderEvent.RECORD_COMPLETED, true, RecorderEvent.SILENCE, null, 0);
                        signal(SIGNAL_COMPLETE);
                        break;
                }
                break;
            case MgcpEvent.REPORT_FAILURE :
                recorderEvent = new RecorderEventImpl(this, RecorderEvent.RECORD_COMPLETED, true, MediaErr.NOT_FOUND, "Not found");
                signal(SIGNAL_FAILURE);
                break;
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
                    fsm.signal(SIGNAL_SUCCESS);
                } catch (UnknownTransitionException e) {
                }
                break;
            default :
                try {
                    fsm.signal(SIGNAL_FAILURE);
                } catch (UnknownTransitionException e) {
                }
        }
    }
    
    private void delay(long amount) {
        try {
            Thread.sleep(amount);
        } catch (InterruptedException e) {
        }
    }
    
    private class RecordRequest implements TransitionHandler {
        public void process(State state) {
            requestRecording();
        }
    }
    
    private class StopRequest implements StateEventHandler {
        
        public void onEvent(State state) {
            stopRecording();
        }
        
    }
    
    private class CompleteNotify implements TransitionHandler {
        
        public void process(State state) {
            fireEvent(recorderEvent);
        }
        
    }
    
    private class StoppedNotify implements TransitionHandler {
        public void process(State state) {
            fireEvent(new RecorderEventImpl(null, RecorderEvent.RECORD_COMPLETED, true, RecorderEvent.NO_QUALIFIER, null, 0));
        }
    }
    
    private class EventSender implements Runnable {
        protected RecorderEvent event;
        
        public EventSender(RecorderEvent event) {
            this.event = event;
        }
        
        public void run() {
            for (MediaEventListener l : listeners) {
                l.onEvent(event);
            }
        }
    }
    
}
