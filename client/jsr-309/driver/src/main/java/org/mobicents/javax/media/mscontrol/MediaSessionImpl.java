package org.mobicents.javax.media.mscontrol;

import jain.protocol.ip.mgcp.message.parms.CallIdentifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.media.mscontrol.Configuration;
import javax.media.mscontrol.MediaConfig;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.vxml.VxmlDialog;

import org.mobicents.javax.media.mscontrol.mediagroup.MediaGroupImpl;
import org.mobicents.javax.media.mscontrol.mixer.MediaMixerImpl;
import org.mobicents.javax.media.mscontrol.networkconnection.NetworkConnectionImpl;

/**
 * 
 * @author amit bhayani
 * 
 */
public class MediaSessionImpl extends MediaObjectImpl implements MediaSession {

    public static final int SESSION_TIMEOUT = 30000;

    public static AtomicInteger lastCallId=new AtomicInteger(0);
    
    //call identifier associated with this session
    private CallIdentifier callID = null;
    
    //the list of active connections
    private List<NetworkConnection> connections = new ArrayList<NetworkConnection>();
    
    //list of available media groups
    private List<MediaGroup> groups = new ArrayList<MediaGroup>();
    
    //list of available media mixers
    private List<MediaMixer> mixers = new ArrayList<MediaMixer>();
    
    //attributes 
    private Map attributes = new HashMap();
    
    private final MsControlFactoryImpl factory;

    private static int reqID;
    
    public MediaSessionImpl(MsControlFactoryImpl factory) throws MsControlException {
        super(null, factory.getDriver(), null);
        this.factory = factory;

        //generate 
        callID = new CallIdentifier(genCallID());
    }

    public CallIdentifier getCallID() {
        return callID;
    }
    
    /**
     * Generates new unique call identifier.
     * 
     * @return hexidecimal integer as string
     */
    private String genCallID() {
        return Integer.toHexString(lastCallId.incrementAndGet());
    }
    
    public synchronized int getUniqueHandler() {
        return factory.getUniqueHandler();
    }
    
    public synchronized int getUniqueReqID() {
        return ++reqID;
    }
    
    public MediaGroup createMediaGroup(Configuration<MediaGroup> pattern) throws MsControlException {
        if (pattern == null) {
            throw new MsControlException("Configuration is NULL");
        }
        
        MediaConfigImpl config = (MediaConfigImpl) factory.getMediaConfig(pattern);
        if (config == null) {
            throw new MsControlException("Configuration is not supported: " + pattern);
        }
        MediaGroup group = new MediaGroupImpl(this, config);
        groups.add(group);
        return group;
    }

    public MediaGroup createMediaGroup(Configuration<MediaGroup> pattern, Parameters params) throws MsControlException {
        if (pattern == null) {
            throw new MsControlException("Configuration is NULL");
        }
        
        MediaConfigImpl config = (MediaConfigImpl) factory.getMediaConfig(pattern);
        if (config == null) {
            throw new MsControlException("Configuration is not supported: " + pattern);
        }
        config = (MediaConfigImpl) config.createCustomizedClone(params);
        MediaGroup group = new MediaGroupImpl(this, config);
        groups.add(group);
        return group;
    }

    public MediaGroup createMediaGroup(MediaConfig config, Parameters params) throws MsControlException {
        MediaGroup group = new MediaGroupImpl(this,(MediaConfigImpl) config.createCustomizedClone(params));
        groups.add(group);
        return group;
    }

    public MediaMixer createMediaMixer(Configuration<MediaMixer> pattern) throws MsControlException {
        if (pattern == null) {
            throw new MsControlException("Configuration is NULL");
        }
        
        MediaConfigImpl config = (MediaConfigImpl) factory.getMediaConfig(pattern);
        if (config == null) {
            throw new MsControlException("Configuration is not supported: " + pattern);
        }
        MediaMixerImpl mixer = new MediaMixerImpl(this, config);
        mixers.add(mixer);
        return mixer;
    }

    public MediaMixer createMediaMixer(Configuration<MediaMixer> pattern, Parameters params) throws MsControlException {
        if (pattern == null) {
            throw new MsControlException("Configuration is NULL");
        }
        
        MediaConfigImpl config = (MediaConfigImpl) factory.getMediaConfig(pattern);
        
        if (config == null) {
            throw new MsControlException("Configuration is not supported: " + pattern);
        }
        
        config = (MediaConfigImpl) config.createCustomizedClone(params);
        
        MediaMixerImpl mixer = new MediaMixerImpl(this, config);
        mixers.add(mixer);
        return mixer;
    }

    public MediaMixer createMediaMixer(MediaConfig config, Parameters params) throws MsControlException {
        MediaMixerImpl mixer = new MediaMixerImpl(this,(MediaConfigImpl) config.createCustomizedClone(params));
        mixers.add(mixer);
        return mixer;
    }

    public NetworkConnection createNetworkConnection(Configuration<NetworkConnection> pattern) throws MsControlException {
        if (pattern == null) {
            throw new MsControlException("Configuration is NULL");
        }
        
        //construsting network configuration from pattern
        MediaConfigImpl config = null;
        if (pattern == NetworkConnection.BASIC) {
            config = (MediaConfigImpl) NetworkConnectionImpl.BASE_CONFIG;
        } else {
            throw new MsControlException("Configuration is not supported: " + pattern);
        }
        
        //create connection object instance
        NetworkConnectionImpl connection = new NetworkConnectionImpl(this, config);
        connections.add(connection);
        
        return connection;
    }

    public NetworkConnection createNetworkConnection(Configuration<NetworkConnection> pattern, Parameters params) throws MsControlException {
        if (pattern == null) {
            throw new MsControlException("Configuration is NULL");
        }
        
        //construsting network configuration from pattern
        MediaConfigImpl config = null;
        if (pattern == NetworkConnection.BASIC) {
            config = (MediaConfigImpl) NetworkConnectionImpl.BASE_CONFIG;
        } else {
            throw new MsControlException("Configuration is not supported: " + pattern);
        }
        
        //add extended params
        config = (MediaConfigImpl) config.createCustomizedClone(params);
        
        //create connection object instance
        NetworkConnectionImpl connection = new NetworkConnectionImpl(this, config);
        connections.add(connection);
        
        return connection;        
    }

    public NetworkConnection createNetworkConnection(MediaConfig config, Parameters params)  throws MsControlException {
        return new NetworkConnectionImpl(this,(MediaConfigImpl) config.createCustomizedClone(params));
    }

    public VxmlDialog createVxmlDialog(Parameters paramParameters) throws MsControlException {
        throw new MsControlException("VxmlDialog is not yet supported");
//		VxmlDialogImpl vxmlDialogImpl = new VxmlDialogImpl(this, mgcpWrapper, paramParameters);
//		return vxmlDialogImpl;
    }

    public Object getAttribute(String paramString) {
        return attributes.get(paramString);
    }

    public Iterator<String> getAttributeNames() {
        return attributes.keySet().iterator();
    }

    public void removeAttribute(String paramString) {
        attributes.remove(paramString);

    }

    public void setAttribute(String paramString, Object paramObject) {
        attributes.put(paramString, paramObject);

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
        for (MediaMixer mixer : mixers) {
            mixer.release();
        }
        
        for (MediaGroup group : groups) {
            group.release();
        }
        
        for (NetworkConnection connection : connections) {
            connection.release();
        }
        
        factory.removeSession(this);
    }

    public void removeConnection(NetworkConnectionImpl connection) {
        connections.remove(connection);
    }
    
    public List<MediaGroup> getMedGrpList() {
        return groups;
    }

    public List<MediaMixer> getMedMxrList() {
        return mixers;
    }

    public MsControlFactoryImpl getMsControlFactoryImpl() {
        return factory;
    }
    
    @Override
    public String toString() {
        return this.getURI().toString();
    }
}