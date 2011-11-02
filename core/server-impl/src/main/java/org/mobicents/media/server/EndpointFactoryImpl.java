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
package org.mobicents.media.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.mobicents.media.Buffer;
import org.mobicents.media.Component;
import org.mobicents.media.ComponentFactory;
import org.mobicents.media.Format;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.naming.NameParser;
import org.mobicents.media.server.impl.naming.NameToken;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointFactory;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.MultimediaSink;
import org.mobicents.media.server.spi.MultimediaSource;
import org.mobicents.media.server.spi.ResourceGroup;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.rtp.RtpManager;

/**
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class EndpointFactoryImpl implements EndpointFactory {

    private String localName;

    private Map<String, ComponentFactory> sourceFactory;
    private Map<String, ComponentFactory> sinkFactory;
    
    private ComponentFactory groupFactory;    
    
    protected ConnectionFactory connectionFactory;    
    protected int connectionPoolSize = 1; //so by def it will atleast work.
    protected RtpManager rtpFactory;
    
    private ArrayList<MediaType> mediaTypes = new ArrayList();

    private NameParser nameParser;
    private static final Logger logger = Logger.getLogger(EndpointFactoryImpl.class);

    public EndpointFactoryImpl() {
        nameParser = new NameParser();
    }

    public EndpointFactoryImpl(String localName) {
        this.localName = localName;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public Collection<MediaType> getMediaTypes() {
        return mediaTypes;
    }

    public int getConnectionPoolSize() {
		return connectionPoolSize;
	}
    
	public void setConnectionPoolSize(int connectionTableSize) {
		this.connectionPoolSize = connectionTableSize;
	}
    
    public Collection<Endpoint> install() throws ResourceUnavailableException {
    	
        ArrayList<Endpoint> list = new ArrayList();
        
        Iterator<NameToken> tokens = nameParser.parse(localName).iterator();
        ArrayList<String> prefixes = new ArrayList();
        prefixes.add("");

        Collection<String> names = getNames(prefixes, tokens.next(), tokens);
        for (String name : names) {
            BaseEndpointImpl enp = new VirtualEndpointImpl(name);

            HashMap<MediaType, MediaSource> sources = null;
            HashMap<MediaType, MediaSink> sinks = null;
            
            if (groupFactory != null) {
                ResourceGroup group = (ResourceGroup) groupFactory.newInstance(enp);
                sources = this.createSource(group, enp);
                sinks = this.createSink(group, enp);
            } else {
                sources = this.createSources(enp);
                sinks = this.createSinks(enp);
            }
            
            enp.setMediaTypes(mediaTypes);
            enp.setSources(sources);
            enp.setSinks(sinks);
            
            enp.setConnectionFactory(connectionFactory);
            enp.setRtpManager(rtpFactory);
            enp.setConnectionPoolSize(connectionPoolSize);
            list.add(enp);
        }
        return list;
    }
    
    public void uninstall() {
        
    }
    
    protected Collection<String> getNames(Collection<String> prefixes, NameToken token, Iterator<NameToken> tokens) {
        ArrayList<String> list = new ArrayList();
        if (!tokens.hasNext()) {
            while (token.hasMore()) {
                String s = token.next();
                for (String prefix : prefixes) {
                    list.add(prefix + "/" + s);
                }
            }
            return list;
        } else {
            Collection<String> newPrefixes = new ArrayList();
            while (token.hasMore()) {
                String s = token.next();
                for (String prefix : prefixes) {
                    newPrefixes.add(prefix + "/" + s);
                }
            }
            return getNames(newPrefixes, tokens.next(), tokens);
        }
    }
    
    /**
     * Gets the list of the media types related to specified name
     * 
     * @param name
     *            the name of the media or wildrard symbol
     * @return
     */
    private MediaType[] getMediaTypes(String media) {
        if (media.equalsIgnoreCase("audio")) {
            return new MediaType[]{MediaType.AUDIO};
        } else if (media.equals("video")) {
            return new MediaType[]{MediaType.VIDEO};
        } else if (media.equals("*")) {
            return new MediaType[]{MediaType.AUDIO, MediaType.VIDEO};
        } else {
            throw new IllegalArgumentException("Unknown media type " + media);
        }
    }

    private HashMap<MediaType, MediaSource> createSources(Endpoint endpoint) throws ResourceUnavailableException {
        HashMap <MediaType, MediaSource> sources = new HashMap();
        
        if (sourceFactory == null) {
            sources.put(MediaType.AUDIO, new VirtualSource("virtual.source"));
            if (!mediaTypes.contains(MediaType.AUDIO)) {
                mediaTypes.add(MediaType.AUDIO);
            }
            return sources;
        }
        // which media types are configured?
        Set<String> types = sourceFactory.keySet();
        for (String media : types) {
            // get the factory configured for each media type
            ComponentFactory factory = sourceFactory.get(media);

            // creating media source
            Component source = factory.newInstance(endpoint);
            source.setEndpoint(endpoint);

            // determine media types related to this source
            MediaType[] list = getMediaTypes(media);
            // apply source
            for (int i = 0; i < list.length; i++) {
                if (source instanceof MediaSource) {
                    sources.put(list[i], (MediaSource) source);
                } else if (source instanceof MultimediaSource) {
                    sources.put(list[i], ((MultimediaSource) source).getMediaSource(list[i]));
                } else {
                    logger.warn("Component " + source.toString() + " is neither instance of MediaSource or MultimediaSource");
                }
                // update list of available media types
                if (!mediaTypes.contains(list[i])) {
                    mediaTypes.add(list[i]);
                }
            }
        }
        
        return sources;
    }

    private HashMap <MediaType, MediaSource> createSource(ResourceGroup group, Endpoint endpoint) {
        HashMap <MediaType, MediaSource> sources = new HashMap();
        Collection<MediaType> list = group.getMediaTypes();
        for (MediaType mediaType : list) {
            MediaSource source = group.getSource(mediaType);
            if (source != null) {
                source.setEndpoint(endpoint);
                sources.put(mediaType, source);

                // update list of available media types
                if (!mediaTypes.contains(mediaType)) {
                    mediaTypes.add(mediaType);
                }
            }
        }
        return sources;
    }

    private HashMap <MediaType, MediaSink> createSinks(Endpoint endpoint) throws ResourceUnavailableException {
        HashMap <MediaType, MediaSink> sinks = new HashMap();
        if (sinkFactory == null) {
            sinks.put(MediaType.AUDIO, new VirtualSink("virtual.sink"));
            if (!mediaTypes.contains(MediaType.AUDIO)) {
                mediaTypes.add(MediaType.AUDIO);
            }
            return sinks;
        }
        // which media types are configured?
        Set<String> types = sinkFactory.keySet();
        for (String media : types) {
            // get the factory configured for each media type
            ComponentFactory factory = sinkFactory.get(media);

            // creating media source
            Component sink = factory.newInstance(endpoint);
            sink.setEndpoint(endpoint);

            // determine media types related to this source
            MediaType[] list = getMediaTypes(media);
            // apply source
            for (int i = 0; i < list.length; i++) {
                if (sink instanceof MediaSink) {
                    sinks.put(list[i], (MediaSink) sink);
                } else if (sink instanceof MultimediaSink) {
                    sinks.put(list[i], ((MultimediaSink) sink).getMediaSink(list[i]));
                } else {
                    logger.warn("Component " + sink.toString() + " is neither instance of MediaSink or MultimediaSink");
                }
                // update list of available media types
                if (!mediaTypes.contains(list[i])) {
                    mediaTypes.add(list[i]);
                }
            }
        }
        
        return sinks;
    }

    private HashMap <MediaType, MediaSink> createSink(ResourceGroup group, Endpoint endpoint) {
        HashMap <MediaType, MediaSink> sinks = new HashMap();
        Collection<MediaType> list = group.getMediaTypes();
        for (MediaType mediaType : list) {
            MediaSink sink = group.getSink(mediaType);
            if (sink != null) {
                sink.setEndpoint(endpoint);
                sinks.put(mediaType, sink);
                // update list of available media types
                if (!mediaTypes.contains(mediaType)) {
                    mediaTypes.add(mediaType);
                }
            }
        }
        return sinks;
    }

    public void setSourceFactory(Map<String, ComponentFactory> sourceFactory) {
        this.sourceFactory = sourceFactory;
    }

    public Map<String, ComponentFactory> getSourceFactory() {
        return sourceFactory;
    }

    public void setSinkFactory(Map<String, ComponentFactory> sinkFactory) {
        this.sinkFactory = sinkFactory;
    }

    public Map<String, ComponentFactory> getSinkFactory() {
        return sinkFactory;
    }

    public ComponentFactory getGroupFactory() {
        return groupFactory;
    }

    public void setGroupFactory(ComponentFactory groupFactory) {
        this.groupFactory = groupFactory;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void setRtpManager(RtpManager rtpFactory) {
        this.rtpFactory = rtpFactory;
    }

    public RtpManager getRtpManager() {
        return this.rtpFactory;
    }


    private class VirtualSink extends AbstractSink {

        public VirtualSink(String name) {
            super(name);
        }

        public Format[] getFormats() {
            return new Format[]{Format.ANY};
        }

        public boolean isAcceptable(Format format) {
            return true;
        }

        @Override
        public void onMediaTransfer(Buffer buffer) throws IOException {
            // do nothing
        }
    }

    private class VirtualSource extends AbstractSource {

        public VirtualSource(String name) {
            super(name);
        }

        @Override
        public void evolve(Buffer buffer, long timestamp) {
            buffer.setFlags(Buffer.FLAG_SILENCE);
            buffer.setDuration(20);
        }

        public Format[] getFormats() {
            return new Format[]{Format.ANY};
        }
    }

}
