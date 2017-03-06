package org.restcomm.javax.media.mscontrol.mixer;

import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.media.mscontrol.Configuration;
import javax.media.mscontrol.MediaConfig;
import javax.media.mscontrol.MediaErr;
import javax.media.mscontrol.MediaEvent;
import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.mixer.MixerAdapter;
import javax.media.mscontrol.mixer.MixerEvent;
import javax.media.mscontrol.resource.Action;
import javax.media.mscontrol.resource.AllocationEvent;
import javax.media.mscontrol.resource.AllocationEventListener;

import org.restcomm.javax.media.mscontrol.MediaConfigImpl;
import org.restcomm.javax.media.mscontrol.MediaObjectImpl;
import org.restcomm.javax.media.mscontrol.MediaSessionImpl;
import org.restcomm.javax.media.mscontrol.ParametersImpl;
import org.restcomm.javax.media.mscontrol.container.ContainerImpl;
import org.restcomm.javax.media.mscontrol.container.Endpoint;

/**
 * 
 * @author amit bhayani
 * 
 */
public class MediaMixerImpl extends ContainerImpl implements MediaMixer {

    public final static MediaConfig AUDIO_CONFIG = new MixerAudioConfig().getConfig();
    
    protected List<MixerAdapter> adaptors = new ArrayList<MixerAdapter>();
    
    protected CopyOnWriteArrayList<MediaEventListener<? extends MediaEvent<?>>> listeners = new CopyOnWriteArrayList<MediaEventListener<? extends MediaEvent<?>>>();
    private CopyOnWriteArrayList<AllocationEventListener> allocationListeners = new CopyOnWriteArrayList();
    
    private int idx = 1;
    
    public MediaMixerImpl(MediaSessionImpl mediaSession, MediaConfigImpl config) throws MsControlException {
        super(mediaSession, config.getParameters());
        this.config = config;
        
        //determine endpoint local name
        String localName = (String)config.getValue(ENDPOINT_NAME);
        //domain name of the server is constructed using proprties from config
        String domainName = session.getDriver().getRemoteDomainName();
        
        //finally, the endpoint identifier is constructed
        endpoint = new Endpoint(new EndpointIdentifier(localName, domainName));
    }

    public MixerAdapter createMixerAdapter(Configuration<MixerAdapter> config) throws MsControlException {
        if (config == null) {
            throw new MsControlException("Configuration is NULL");
        }
                        
        Boolean hasDtmfClamp=false;
        if (config == MixerAdapter.DTMF_CLAMP)
        	hasDtmfClamp=true;	
        
        ParametersImpl params = new ParametersImpl();
        params.put(MEDIAOBJECT_ID, this.config.getParameters().get(MEDIAOBJECT_ID) + "/adaptor" + (idx++));
        params.put(MediaObjectImpl.ENDPOINT_NAME, this.config.getParameters().get(MediaObjectImpl.ENDPOINT_NAME));
        
        MixerAdapterImpl adaptor = new MixerAdapterImpl(this, params,hasDtmfClamp);
        adaptors.add(adaptor);
        return adaptor;

    }

    public MixerAdapter createMixerAdapter(Configuration<MixerAdapter> pattern, Parameters param)  throws MsControlException {
        if (config == null) {
            throw new MsControlException("Configuration can't be null");
        }
        
        MediaConfigImpl cfg = (MediaConfigImpl) getConfiguration(pattern).createCustomizedClone(param);

        MixerAdapterImpl adaptor = new MixerAdapterImpl(this,param);
        adaptors.add(adaptor);
        return adaptor;
    }

    public MixerAdapter createMixerAdapter(MediaConfig config, Parameters params)  throws MsControlException {        
        if (config == null) {
            throw new MsControlException("MediaConfig cannot be null");
        }
        
        MediaConfig cfg = ((MediaConfigImpl)config).createCustomizedClone(params);

        Boolean hasDtmfClamp=false;
        if (config == MixerAdapter.DTMF_CLAMP)
        	hasDtmfClamp=true;	
        
        MixerAdapterImpl adaptor = new MixerAdapterImpl(this,params,hasDtmfClamp);
        adaptors.add(adaptor);
        return adaptor;
    }

    public boolean hasDtmfClamp()
    {
    	for(int i=0;i<adaptors.size();i++)
    		if(((MixerAdapterImpl)adaptors.get(i)).dtmfClamp())
    			return true;
    	
    	return false;
    }
    
    public void confirm() throws MsControlException {
        AllocationEventImpl evt = new AllocationEventImpl(this, AllocationEvent.ALLOCATION_CONFIRMED, true, MediaErr.NO_ERROR, null);
        for (AllocationEventListener l : allocationListeners) {
            l.onEvent(evt);
        }
    }

    public MediaConfig getConfig() {
        return this.config;
    }

    public <R> R getResource(Class<R> resource) throws MsControlException {
        // TODO Auto-generated method stub
        return null;
    }

    public void triggerRTC(Action rtca) {
        // TODO Auto-generated method stub
    }

    public void release() {
        try {
            unjoin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addListener(MediaEventListener<MixerEvent> listener) {
        this.listeners.add(listener);
    }

    public void removeListener(MediaEventListener<MixerEvent> listener) {
        this.listeners.remove(listener);
    }

    public Iterator<MediaObject> getMediaObjects() {
        // TODO Auto-generated method stub
        return null;
    }

    public <T extends MediaObject> Iterator<T> getMediaObjects(Class<T> paramClass) {
        // TODO Auto-generated method stub
        return null;
    }

    public void addListener(AllocationEventListener listener) {
        this.allocationListeners.add(listener);
    }

    public void removeListener(AllocationEventListener listener) {
        this.allocationListeners.remove(listener);
    }

    public void triggerAction(Action arg0) {
        // TODO Auto-generated method stub
    }
    
    public MediaConfig getConfiguration(Configuration pattern) {
        if (pattern.equals(MixerAdapter.DTMF_CLAMP)) {
            return MixerAdapterImpl.ADAPTOR_CFG;
        }
        return null;
    }
}
