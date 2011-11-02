/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media.server.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Logger;
import org.mobicents.media.Component;
import org.mobicents.media.ComponentFactory;
import org.mobicents.media.Inlet;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.Outlet;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.MultimediaSink;
import org.mobicents.media.server.spi.MultimediaSource;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * Factory class for creating channels.
 * 
 * @author kulikov
 */
public class ChannelFactory {

    private List<PipeFactory> pipes;
    private List<ComponentFactory> factories;
    private MediaType mediaType;
    private volatile boolean started = false;
    private static final Logger logger = Logger.getLogger(ChannelFactory.class);

    /**
     * Returns new channel.
     * 
     * if there is unused channels in the cache the existing channels will be 
     * returned and new instance other wise.
     * 
     * @return
     * @throws org.mobicents.media.server.resource.UnknownComponentException
     */
    public Channel newInstance(Endpoint endpoint, MediaType media) throws  ResourceUnavailableException{
        if (!started) {
            throw new IllegalStateException("Factory is not started");
        }
        return createNewChannel(endpoint,media);
    }

    /**
     * Constructs new channel instance.
     * @param media 
     * 
     * @return channel instance.
     * @throws org.mobicents.media.server.resource.UnknownComponentException
     */
    private Channel createNewChannel(Endpoint endpoint, MediaType media) throws ResourceUnavailableException {
        //creating components
        HashMap<String, MediaSource> sources = new HashMap<String, MediaSource>();
        HashMap<String, MediaSink> sinks = new HashMap<String, MediaSink>();
        HashMap<String, Inlet> inlets = new HashMap<String, Inlet>();
        HashMap<String, Outlet> outlets = new HashMap<String, Outlet>();
        
      
        for (ComponentFactory factory: factories) {
            Component component = factory.newInstance(endpoint);
           if(component instanceof MultimediaSource)
           {
        	   MultimediaSource ms = (MultimediaSource) component;
        	   Collection<MediaType> supportedTypes=ms.getMediaTypes();
        	   if(supportedTypes.contains(media))
        	   {
        		   component = ms.getMediaSource(media);
        	   }else
        	   {
        		   //?
        		   continue;
        	   }
           }
           
           if (component instanceof MultimediaSink) {
                MultimediaSink ms = (MultimediaSink) component;
                Collection<MediaType> supportedTypes=ms.getMediaTypes();
         	   if(supportedTypes.contains(media))
         	   {
         		   component = ms.getMediaSink(media);
         	   }else
         	   {
         		   //?
         		   continue;
         	   }
            }
            
            if (component instanceof MediaSink) {
                sinks.put(component.getName(), (MediaSink)component);
            }
            
            if (component instanceof MediaSource) {
                sources.put(component.getName(),(MediaSource)component);
            }
            
            if (component instanceof Inlet) {
                sinks.put(component.getName(), ((Inlet)component).getInput());
                inlets.put(component.getName(), (Inlet)component);
            }
            
            if (component instanceof Outlet) {
                sources.put(component.getName(), ((Outlet)component).getOutput());
                outlets.put(component.getName(), (Outlet)component);
            }
        }
        
        Channel channel = new Channel(sources, sinks, inlets, outlets);
        
        //creating pipes
        for (PipeFactory pipeFactory : pipes) {
            try {
                pipeFactory.openPipe(channel);
            } catch (UnknownComponentException e) {
                throw new ResourceUnavailableException(e);
            }
        }
        
        return channel;
    }

    /**
     * Modify pipe list.
     * 
     * @param pipes the list of pipes beans defining media flow path
     */
    public void setPipes(List<PipeFactory> pipes) {
        this.pipes = pipes;
    }

    /**
     * Gets the existing list of pipes.
     * 
     * @return the list of pipes.
     */
    public List getPipes() {
        return this.pipes;
    }

    /**
     * Gets the list of components which will be placed into the new channel.
     * 
     * @return the list of media component.
     */
    public List getComponents() {
        return factories;
    }

    /**
     * Sets the list of components which will be placed into the new channel.
     * 
     * @return the list of media component.
     */
    public void setComponents(List components) {
        this.factories = components;
    }

    /**
     * Gets the media type of the channel generated by this factory.
     * 
     * @return the media type identifier
     */
    public MediaType getMediaType() {
        return mediaType;
    }

    /**
     * Assigns media type which will be used by this factory for generation channels
     * 
     * @param mediaType media type identifier.
     */
    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    /**
     * Starts this factory.
     * 
     */
    public void start() throws Exception {
        started = true;
        if (factories == null) {
            factories = new ArrayList();
        }
        if (pipes == null) {
            pipes = new ArrayList();
        }
    }

    /**
     * Stop this factory.
     */
    public void stop() {
        started = false;
        pipes.clear();
        factories.clear();
    }
}
