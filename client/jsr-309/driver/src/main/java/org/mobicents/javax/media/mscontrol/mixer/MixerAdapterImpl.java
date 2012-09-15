package org.mobicents.javax.media.mscontrol.mixer;

import java.io.Serializable;
import java.util.Iterator;

import javax.media.mscontrol.MediaConfig;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.JoinableStream;
import javax.media.mscontrol.join.JoinableStream.StreamType;
import javax.media.mscontrol.mixer.MixerAdapter;
import javax.media.mscontrol.resource.Action;
import javax.media.mscontrol.resource.AllocationEventListener;

import org.mobicents.javax.media.mscontrol.container.ContainerImpl;
import org.mobicents.javax.media.mscontrol.ParameterImpl;

public class MixerAdapterImpl extends ContainerImpl implements MixerAdapter {
    public static MediaConfig ADAPTOR_CFG = new AdapterConfig().getConfig();    
    private final MediaMixerImpl mixer;
    private Boolean hasDtmfClamp=false;
    
    public MixerAdapterImpl(MediaMixerImpl mixer, Parameters params) throws MsControlException {
        super(mixer.getMediaSession(), params);        
        this.mixer = mixer;
    }
    
    public MixerAdapterImpl(MediaMixerImpl mixer, Parameters params,Boolean dtmfClamp) throws MsControlException {
        super(mixer.getMediaSession(), params);        
        this.mixer = mixer;
        this.hasDtmfClamp=dtmfClamp;
    }

    public boolean dtmfClamp()
    {
    	return this.hasDtmfClamp;
    }
    
    @Override
    public JoinableStream getJoinableStream(StreamType value) throws MsControlException {
        return mixer.getJoinableStream(value);
    }
    
    @Override
    public void join(Direction direction, Joinable other) throws MsControlException {
        mixer.join(direction, other);
    }
    
    @Override
    public void joinInitiate(Direction direction, Joinable other, Serializable context) throws MsControlException {
        mixer.joinInitiate(direction, other, context);
    }    
    
    @Override
    public void unjoin(Joinable other) throws MsControlException {
        mixer.unjoin(other);
    }   
    
    @Override
    public void unjoinInitiate(Joinable other, Serializable context) throws MsControlException {
        mixer.unjoinInitiate(other, context);
    }
    
    public void confirm() throws MsControlException {
        mixer.confirm();
    }

    public MediaConfig getConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    public <R> R getResource(Class<R> arg0) throws MsControlException {
        // TODO Auto-generated method stub
        return null;
    }

    public void triggerAction(Action arg0) {
        // TODO Auto-generated method stub
    }

    public Iterator<MediaObject> getMediaObjects() {
        // TODO Auto-generated method stub
        return null;
    }

    public <T extends MediaObject> Iterator<T> getMediaObjects(Class<T> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected ContainerImpl getOwner() {
        return this.mixer;
    }
    
    public void release() {
        this.mixer.adaptors.remove(this);
    }

    public void addListener(AllocationEventListener listener) {
        // TODO Auto-generated method stub
    }

    public void removeListener(AllocationEventListener listener) {
        // TODO Auto-generated method stub
    }
}
