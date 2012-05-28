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
package org.mobicents.media.server.ds0;

import java.util.ArrayList;
import org.mobicents.media.Component;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.component.Splitter;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.impl.PipeImpl;
import org.mobicents.media.server.BaseSS7EndpointImpl;
import org.mobicents.media.server.io.ss7.SS7Manager;
import org.mobicents.media.server.io.ss7.SS7DataChannel;
/**
 *
 * @author oifa yulian
 */
public class DS0Endpoint extends BaseSS7EndpointImpl {

    private SS7DataChannel ss7DataChannel;
    
    private AudioMixer audioMixer;    
    private Splitter audioSplitter;
    
    private ArrayList<MediaSource> components = new ArrayList();
    
    public DS0Endpoint(String name,int endpointType,SS7Manager ss7Manager,int channelID,boolean isALaw) {
        super(name,endpointType,ss7Manager,channelID,isALaw);
        
        try
        {
        	ss7DataChannel=new SS7DataChannel(ss7Manager,channelID,isALaw);
        }
        catch(Exception e)
        {
        	
        }
    }
    
    @Override
    public MediaSink getSink(MediaType media) {
        switch(media) {
            case AUDIO : 
            	return audioSplitter.getInput();
            default : 
            	return null;
        }
    }

    @Override
    public MediaSource getSource(MediaType media) {
        switch (media) {
            case AUDIO:
                return audioMixer.getOutput();
            default:
                return null;
        }
    }
    
    @Override
    public void start() throws ResourceUnavailableException {
    	try {
    		audioMixer = new AudioMixer(getScheduler());
        	MediaSink sink = audioMixer.newInput();
            MediaSource source=ss7DataChannel.getInput();
            PipeImpl p = new PipeImpl();
            p.connect(sink);
            p.connect(source);
            
            components.add(source);                           
            sink.start();
            
            audioSplitter = new Splitter(getScheduler());
            PipeImpl p1 = new PipeImpl();            
            p1.connect(audioSplitter.getInput());
            p1.connect(ss7DataChannel.getOutput());                
            
        } catch (Exception e) {
            throw new ResourceUnavailableException(e);
        }

        super.start();
    }
    
    @Override
    public Component getResource(MediaType mediaType, Class intf) {
        switch (mediaType) {
            case AUDIO:
            	for (int i = 0; i < components.size(); i++) {
                    if (components.get(i).getInterface(intf) != null) {
                        return components.get(i);
                    }
                }
            default:
                return null;
        }
    }
    
    @Override
    public void unblock() {
    }

    @Override
    public void block() {
    }
}
