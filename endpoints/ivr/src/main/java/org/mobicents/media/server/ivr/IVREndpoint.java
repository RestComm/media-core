/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.media.server.ivr;

import org.mobicents.media.Component;
import org.mobicents.media.MediaSink;
import org.mobicents.media.server.announcement.AnnouncementEndpoint;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderImpl;
import org.mobicents.media.server.impl.resource.dtmf.DetectorImpl;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.recorder.Recorder;

/**
 *
 * @author kulikov
 */
public class IVREndpoint extends AnnouncementEndpoint {

    private Recorder recorder;
    private AudioSink audioSink;
    
    private DetectorImpl dtmfDetector;
    
    public IVREndpoint(String name) {
        super(name);
    }
    
    public IVREndpoint(String name,int endpointType) {
        super(name,endpointType);
    }
    
    @Override
    public MediaSink getSink(MediaType media) {
        switch(media) {
            case AUDIO : return audioSink;
            default : return null;
        }
    }

   
    @Override
    public void start() throws ResourceUnavailableException {
        //construct audio recorder
        recorder = new AudioRecorderImpl(getScheduler());
        
        //construct audio sink
        audioSink = new AudioSink(getScheduler(), getLocalName());
        audioSink.add(recorder);
        
        super.start();
    }
    
    @Override
    public Component getResource(MediaType mediaType, Class intf) {
        switch (mediaType) {
            case AUDIO:
                Component c = super.getResource(mediaType, intf);
                return c != null ? c : audioSink.getComponent(intf);
            default:
                return null;
        }
    }
    
}
