package org.mobicents.javax.media.mscontrol.mediagroup.signals;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.NotificationRequestResponse;
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

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.UnsupportedException;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.signals.SignalDetector;
import javax.media.mscontrol.mediagroup.signals.SignalDetectorEvent;
import javax.media.mscontrol.resource.RTC;
import javax.media.mscontrol.resource.ResourceEvent;
import javax.media.mscontrol.resource.Trigger;

import org.mobicents.javax.media.mscontrol.spi.DriverImpl;
import org.mobicents.javax.media.mscontrol.MediaConfigImpl;
import org.mobicents.javax.media.mscontrol.mediagroup.MediaGroupImpl;
import org.mobicents.jsr309.mgcp.PackageAU;

/**
 * 
 * @author amit bhayani
 * @author vralev
 * @author kulikov
 */
public class SignalDetectorImpl implements SignalDetector, JainMgcpListener {

    private CopyOnWriteArrayList<MediaEventListener<SignalDetectorEvent>> listeners = new CopyOnWriteArrayList();
    
    private MediaGroupImpl parent = null;
    private MediaConfigImpl config;

    private String[] filters;
    
    private boolean rtcTriggered = false;
    
    private ArrayList<Trigger> triggers = new ArrayList();
    
    private FlushHandler flushHandler;
    private StopHandler stopHandler;
    
    private MgcpSender mgcpSender;
    
    public SignalDetectorImpl(MediaGroupImpl parent, MediaConfigImpl config) {
        this.parent = parent;
        this.config = config;
        
        flushHandler = new FlushHandler();        
        stopHandler = new StopHandler(); 
        
        mgcpSender=new MgcpSender();
    }

    public void flushBuffer() throws MsControlException {
        try {
    	    Thread.sleep(500);
    	} catch (Exception e) {
    	}
        this.cleanBuffer();
    }

    public void receiveSignals(int numSignals, Parameter[] labels, RTC[] rtc, Parameters options) throws MsControlException {
        if (rtc != null) {
            verifyRTC(rtc);
        }
        parent.info("Receive signal called");
        rtcTriggered = false;
        
        triggers.clear();
        
        String[] patterns = this.getPatterns(labels, options);
        
        if (patterns == null) {
            patterns = this.getPatterns(rtc, options);
        }
        
        filters = this.getFilters(options);
        
        Options params = new Options();
        params.setDigitsNumber(numSignals);
        params.setDigitPattern(patterns);

        if (options != null && options.containsKey(SignalDetectorImpl.PROMPT)) {
            params.setPrompt(((URI) options.get(SignalDetectorImpl.PROMPT)).toString());
        }
        
        if (options != null && options.containsKey(SignalDetectorImpl.INITIAL_TIMEOUT)) {
            params.setFirstDigitTimer((Integer) options.get(SignalDetectorImpl.INITIAL_TIMEOUT));
        }
        
        if (options != null && options.containsKey(SignalDetectorImpl.INTER_SIG_TIMEOUT)) {
            params.setInterDigitTimer((Integer) options.get(SignalDetectorImpl.INTER_SIG_TIMEOUT));
        }
        
        if (options != null && options.containsKey(SignalDetectorImpl.MAX_DURATION)) {
            params.setMaxDuration((Integer) options.get(SignalDetectorImpl.MAX_DURATION));
        }
        
        if (options != null && options.containsKey(Player.REPEAT_COUNT)) {
            params.setNumberOfAttempts((Integer) options.get(Player.REPEAT_COUNT));
        }

        params.setNonInterruptiblePlay(true);
        Boolean hasClearDigits=false;
        if (rtc != null) {
            for (int i = 0; i < rtc.length; i++) {
                if (rtc[i] == MediaGroup.SIGDET_STOPPLAY) {
                    params.setNonInterruptiblePlay(false);
                }
                
                if (rtc[i].getTrigger() == Player.PLAY_START && rtc[i].getAction() == SignalDetector.FLUSH_BUFFER) {
                	delay(500);
                	hasClearDigits=true;
                	params.setClearDigits(true);
                }
            }
        }       	
        
        this.requestPlayCollect(params.toString(),hasClearDigits);
    }

    private void verifyRTC(RTC[] rtc) throws UnsupportedException {
        for (RTC r: rtc ) {
            if (r.getTrigger() == Player.PLAY_START && r.getAction() == Player.STOP) {
                throw new UnsupportedException("Invalid RTC");
            }
        }
    }
    
    private String[] getPatterns(Parameter[] labels, Parameters options) {
        if (labels == null || options == null) {
            return null;
        }
        
        String[] patterns = new String[labels.length];
        for (int i = 0; i < labels.length; i++) {
            patterns[i] = (String) options.get(labels[i]);
        }
        
        return patterns;
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
                        list.add((String)options.get(SignalDetector.PATTERN[i]));
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
    
    private String[] getFilters(Parameters options) {
        if (options == null) {
            return null;
        }
        
        if (options.get(SignalDetector.FILTERED_PATTERNS) == null) {
            return null;
        }
        
        Parameter[] ptt = (Parameter[]) options.get(SignalDetector.FILTERED_PATTERNS);
        String[] filters = new String[ptt.length];
        for (int i = 0; i < ptt.length; i++) {
            filters[i] = (String) options.get(ptt[i]);
        }
        
        return filters;
    }
    
    public MediaGroup getContainer() {
        return this.parent;
    }

    public void stop() {    	
    	this.stopCollection();        
    }

    public void addListener(MediaEventListener<SignalDetectorEvent> listener) {
        this.listeners.add(listener);
    }

    public MediaSession getMediaSession() {
        return parent.getMediaSession();
    }

    public void removeListener(MediaEventListener<SignalDetectorEvent> listener) {
        listeners.remove(listener);
    }

    public void update(SignalDetectorEvent anEvent) {
        for (MediaEventListener<SignalDetectorEvent> m : listeners) {
            m.onEvent(anEvent);
        }
    }

    private void requestPlayCollect(String params,Boolean hasClearDigits) {
        //generate request identifier and transaction ID
        RequestIdentifier reqID = parent.nextRequestID();        
        int txID = parent.getMediaSession().getDriver().getNextTxID();
        
        //constructs request
        NotificationRequest req = new NotificationRequest(this, parent.getEndpoint().getIdentifier(), reqID);
        
        RequestedAction[] actions = new RequestedAction[] { RequestedAction.NotifyImmediately };

        ArrayList<EventName> signalList = new ArrayList();
        ArrayList<RequestedEvent> eventList = new ArrayList();

        //player signals     
        signalList.add(new EventName(PackageAU.Name, PackageAU.pc.withParm(params)));

        //player events
        eventList.add(new RequestedEvent(new EventName(PackageAU.Name, MgcpEvent.oc), actions));
        eventList.add(new RequestedEvent(new EventName(PackageAU.Name, MgcpEvent.of), actions));
        
        EventName[] signals = new EventName[signalList.size()];        
        signalList.toArray(signals);
        
        RequestedEvent[] events = new RequestedEvent[eventList.size()];
        eventList.toArray(events);
        
        DriverImpl driver=parent.getMediaSession().getDriver();
        
        //new EventName(auPackageName, MgcpEvent.oc, connId)        
        req.setRequestedEvents(events);
        req.setSignalRequests(signals);
        
        req.setTransactionHandle(txID);
        req.setNotifiedEntity(driver.getCallAgent());

        driver.attach(txID, this);
        driver.attach(reqID, this);
        
        if(this.parent.isStopping())
        	mgcpSender.init(driver, req);
        else
        	driver.send(req);        
    }

    private void cleanBuffer() {
        //generate request identifier and transaction ID
        RequestIdentifier reqID = parent.nextRequestID();        
        int txID = parent.getMediaSession().getDriver().getNextTxID();
        
        //constructs request
        NotificationRequest req = new NotificationRequest(this, parent.getEndpoint().getIdentifier(), reqID);
        
        //new EventName(auPackageName, MgcpEvent.oc, connId)        
        req.setSignalRequests(new EventName[]{new EventName(PackageAU.Name, PackageAU.es.withParm("cb=true"))});
        
        req.setTransactionHandle(txID);
        
        DriverImpl driver=parent.getMediaSession().getDriver();
        
        req.setNotifiedEntity(driver.getCallAgent());

        driver.attach(txID, flushHandler);
        
        driver.send(req);
    }

    private void stopCollection() {
        //generate request identifier and transaction ID
        RequestIdentifier reqID = parent.nextRequestID();        
        int txID = parent.getMediaSession().getDriver().getNextTxID();
        
        //constructs request
        NotificationRequest req = new NotificationRequest(this, parent.getEndpoint().getIdentifier(), reqID);
        
        //new EventName(auPackageName, MgcpEvent.oc, connId)        
        req.setSignalRequests(new EventName[]{new EventName(PackageAU.Name, PackageAU.es)});
        
        req.setTransactionHandle(txID);
        
        DriverImpl driver=parent.getMediaSession().getDriver();
        
        req.setNotifiedEntity(driver.getCallAgent());

        driver.attach(txID, stopHandler);
        this.parent.waitForStop();
        driver.send(req);
    }
    
    public void processMgcpCommandEvent(JainMgcpCommandEvent event) {
        switch (event.getObjectIdentifier()) {
            case Constants.CMD_NOTIFY :
                this.processNotify((Notify) event);
                break;
            default :
                return;
        }
    }

    public void processMgcpResponseEvent(JainMgcpResponseEvent event) {
        //NotifucationRequest response expected only
        NotificationRequestResponse evt = (NotificationRequestResponse) event;
        
        switch (event.getReturnCode().getValue()) {
            case ReturnCode.TRANSACTION_EXECUTED_NORMALLY :
                break;
                
        }
    }
    
    private void processNotify(Notify notify) {
        EventName[] events = notify.getObservedEvents();        
        for (EventName evt : events) {
            switch (evt.getEventIdentifier().intValue()) {
                case MgcpEvent.REPORT_ON_COMPLETION:
                    fireEvent(evt.getEventIdentifier().getParms());
                    break;
                case MgcpEvent.REPORT_FAILURE:
                    break;
            }
        }
    }
    
    private void fireEvent(String params) {
        Options options = new Options(params);
        options.processFilters(filters);
        
        SignalDetectorEventImpl report = new SignalDetectorEventImpl(this, SignalDetectorEvent.RECEIVE_SIGNALS_COMPLETED, true, options.getDigitsCollected());
        if (options.getPatternIndex() >= 0) {            
            report.setPatterIndex(options.getPatternIndex());
            if (rtcTriggered) {
                report.setQualifier(ResourceEvent.RTC_TRIGGERED);
                report.setRtcTrigger(triggers.get(options.getPatternIndex()));
            }
        } else if(options.getReturnCode()==326) {
        	if(options.getDigitsCollected()!=null && options.getDigitsCollected().length()>0)
        		report.setQualifier(SignalDetectorEvent.INTER_SIG_TIMEOUT_EXCEEDED);
        	else
        		report.setQualifier(SignalDetectorEvent.INITIAL_TIMEOUT_EXCEEDED);
        } else if(options.getReturnCode()==330) {
        	report.setQualifier(SignalDetectorEvent.DURATION_EXCEEDED);
        }
        else {        	
            report.setQualifier(SignalDetectorEvent.NUM_SIGNALS_DETECTED);
        }
        fireEvent(report);
    }
    
    protected void fireEvent(SignalDetectorEvent event) {
        delay(2);
        for (MediaEventListener l : listeners) {
            l.onEvent(event);
        }
    }

    public void patternMatches(int index, String s) {
        SignalDetectorEventImpl evt = new SignalDetectorEventImpl(this, SignalDetectorEvent.RECEIVE_SIGNALS_COMPLETED, true, s);
        evt.setPatterIndex(index);
        fireEvent(evt);
    }

    public void countMatches(String s) {
        SignalDetectorEventImpl evt = new SignalDetectorEventImpl(this, SignalDetectorEvent.RECEIVE_SIGNALS_COMPLETED, true, s);
        evt.setQualifier(SignalDetectorEvent.NUM_SIGNALS_DETECTED);
        fireEvent(evt);
    }
    
    private void delay(long amount) {
        try {
            Thread.sleep(amount);
        } catch (InterruptedException e) {
        }
    }
    
    @Override
    public String toString() {
        return String.format("Detector(%s)", this.getContainer());
    }
    
    public void stopCompleted()
    {
    	if(mgcpSender.waiting)
    		mgcpSender.run();
    }
    
    private class FlushHandler implements JainMgcpListener {

        public void processMgcpCommandEvent(JainMgcpCommandEvent event) {
        }

        public void processMgcpResponseEvent(JainMgcpResponseEvent event) {
            switch (event.getReturnCode().getValue()) {
                case ReturnCode.TRANSACTION_EXECUTED_NORMALLY:                    
                    SignalDetectorEventImpl oc = 
                	new SignalDetectorEventImpl(null, SignalDetectorEvent.FLUSH_BUFFER_COMPLETED, true);                    
                            fireEvent(oc);
                    System.out.println("DONE FLUSH BUFFER=======================>");
                    break;
                default :
                    break;
                    
            }
        }
        
    }
    
    private class StopHandler implements JainMgcpListener {

        public void processMgcpCommandEvent(JainMgcpCommandEvent event) {
        }

        public void processMgcpResponseEvent(JainMgcpResponseEvent event) {
            switch (event.getReturnCode().getValue()) {
                case ReturnCode.TRANSACTION_EXECUTED_NORMALLY:  
                	parent.releaseStop();
                    SignalDetectorEventImpl oc = 
                            new SignalDetectorEventImpl(null, SignalDetectorEvent.RECEIVE_SIGNALS_COMPLETED, true);                    
                    oc.setQualifier(ResourceEvent.STOPPED);
                    fireEvent(oc);
                    System.out.println("DONE STOP=======================>");
                    break;
                default :
                    break;
                    
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
}
