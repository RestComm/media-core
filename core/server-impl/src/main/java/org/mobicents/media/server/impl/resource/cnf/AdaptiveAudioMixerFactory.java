package org.mobicents.media.server.impl.resource.cnf;

import org.mobicents.media.Component;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.ResourceUnavailableException;

public class AdaptiveAudioMixerFactory extends AudioMixerFactory{
    public Component newInstance(Endpoint endpoint) throws ResourceUnavailableException {
        return new AdaptiveAudioMixer(name);
    }
}
