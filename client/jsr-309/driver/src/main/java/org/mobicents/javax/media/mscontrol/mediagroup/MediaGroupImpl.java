package org.mobicents.javax.media.mscontrol.mediagroup;

import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;

import java.net.URI;
import java.util.Iterator;

import javax.media.mscontrol.MediaConfig;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.Recorder;
import javax.media.mscontrol.mediagroup.signals.SignalDetector;
import javax.media.mscontrol.mediagroup.signals.SignalGenerator;
import javax.media.mscontrol.resource.Action;
import javax.media.mscontrol.resource.AllocationEventListener;

import org.mobicents.javax.media.mscontrol.container.ContainerImpl;
import org.mobicents.javax.media.mscontrol.MediaConfigImpl;
import org.mobicents.javax.media.mscontrol.MediaSessionImpl;
import org.mobicents.javax.media.mscontrol.container.Endpoint;
import org.mobicents.javax.media.mscontrol.mediagroup.signals.SignalDetectorImpl;

/**
 * 
 * @author amit bhayani
 * 
 */
public class MediaGroupImpl extends ContainerImpl implements MediaGroup {

    public final static MediaConfig PLAYER_CONFIG = new PlayerConfigImpl().getConfig();
    public final static MediaConfig SIGNAL_DETECTOR_CONFIG = new SignalDetectorConfig().getConfig();
    public final static MediaConfig PLAYER_SIGNAL_DETECTOR_CONFIG = new PlayerSignalDetectorConfig().getConfig();
    public final static MediaConfig RECORDER_CONFIG = new RecorderConfigImpl().getConfig();
    public final static MediaConfig PLAYER_RECORDER_SIGNAL_DETECTOR_CONFIG = new PlayerRecorderSignalDetectorConfig().getConfig();
    
    private URI uri = null;
    
    protected PlayerImpl player = null;
    protected RecorderImpl recorder = null;
    
    protected SignalDetectorImpl detector = null;
    protected SignalGenerator generator = null;
    
    public RequestIdentifier reqID = null;    
    
    private Boolean stopping=false;
    
    public MediaGroupImpl(MediaSessionImpl session, MediaConfigImpl config) throws MsControlException {
        super(session, config.getParameters());
        //determine endpoint local name
        String localName = (String)config.getValue(ENDPOINT_NAME);
        //domain name of the server is constructed using proprties from config
        String domainName = session.getDriver().getRemoteDomainName();
        
        //finally, the endpoint identifier is constructed
        endpoint = new Endpoint(new EndpointIdentifier(localName, domainName));
        
        player = new PlayerImpl(this);
        recorder = new RecorderImpl(this);
        detector = new SignalDetectorImpl(this, config);
        
    }
    
    public boolean isStopping()
    {
    	return this.stopping;
    }
    
    public void waitForStop()
    {
    	this.stopping=true;
    }
    
    public void releaseStop()
    {
    	this.stopping=false;
    	player.stopCompleted();
    	recorder.stopCompleted();
    	detector.stopCompleted();
    }

    // MediaGroup Methods
    public Player getPlayer() throws MsControlException {
        if (this.player != null) {
            return player;
        } else {
            throw new MsControlException(this.uri + " This MediaGroup contains no Player");
        }
    }

    public Recorder getRecorder() throws MsControlException {
        if (this.recorder != null) {
            return this.recorder;
        } else {
            throw new MsControlException(this.uri + " This MediaGroup contains no Recorder");
        }
    }

    public SignalDetector getSignalDetector() throws MsControlException {
        if (this.detector != null) {
            return this.detector;
        } else {
            throw new MsControlException(this.uri + " This MediaGroup contains no Signal Detector");
        }
    }

    public SignalGenerator getSignalGenerator() throws MsControlException {
        if (this.generator != null) {
            return this.generator;
        } else {
            throw new MsControlException(this.uri + " This MediaGroup contains no Signal Generator");
        }
    }

    public void stop() {
        this.player.stop(true);
        this.recorder.stop();
        this.detector.stop();
    }

    // ResourceContainer methods
    public void confirm() throws MsControlException {
    }

    public MediaConfig getConfig() {
        return config;
    }

    public <R> R getResource(Class<R> arg0) throws MsControlException {
        return null;
    }

    public void triggerRTC(Action rtca) {
    }

    public void release() {
        //unregister media group as listener
        info("Releasing...");
        try {
            unjoin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public Iterator<MediaObject> getMediaObjects() {
        // TODO Auto-generated method stub
        return null;
    }

    public <T extends MediaObject> Iterator<T> getMediaObjects(Class<T> paramClass) {
        // TODO Auto-generated method stub
        return null;
    }

    public void addListener(AllocationEventListener paramAllocationEventListener) {
        // TODO Auto-generated method stub
    }

    public void removeListener(AllocationEventListener paramAllocationEventListener) {
        // TODO Auto-generated method stub
    }

    public void triggerAction(Action action) {
        if (action == null) {
            return;
        } else if (action.equals(SignalDetector.FLUSH_BUFFER)) {
                throw new IllegalStateException(this.uri + " This MediaGroup contains no Signal Detector");
        // TODO : Handle RTC
        } else if (action.equals(SignalDetector.STOP)) {
            if (this.detector == null) {
                throw new IllegalStateException(this.uri + " This MediaGroup contains no Signal Detector");
            }
        // TODO : Handle RTC
        } else if (action.equals(SignalDetector.CANCEL)) {
            if (this.detector == null) {
                throw new IllegalStateException(this.uri + " This MediaGroup contains no Signal Detector");
            }
        // TODO : Handle RTC
        }

    }
    
    /**
     * Generates unique request identifier.
     * 
     * @return request identifier object.
     */
    public RequestIdentifier nextRequestID() {
        return new RequestIdentifier(Integer.toString(session.getUniqueReqID()));
    }
    
//    public Collection<ConnectionIdentifier> getConnectionIDs() {
//        ArrayList<ConnectionIdentifier> list = new ArrayList();
//        for (LocalConnection c : getConnections()) {
//            list.add(c.getID());
//        }
//        return list;
//    }
}
