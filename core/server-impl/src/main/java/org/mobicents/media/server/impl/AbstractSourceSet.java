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
package org.mobicents.media.server.impl;

import java.util.Collection;
import java.util.HashMap;
import org.mobicents.media.MediaSink;

/**
 *
 * @author kulikov
 */
public abstract class AbstractSourceSet extends AbstractSource {

    private HashMap<String, AbstractSource> sources = new HashMap();
    
    public AbstractSourceSet(String name) {
        super(name);
    }
    
    public Collection<AbstractSource> getStreams() {
        return sources.values();
    }

    @Override
    public boolean isMultipleConnectionsAllowed() {
        return true;
    }
    
    @Override
    public void start() {
        Collection<AbstractSource> streams = sources.values();
/*        for (AbstractSource stream : streams) {
            if (stream.isConnected() && stream.getFormat() != null) {
                stream.start();
            }
        }
 */ 
    }
    
    @Override
    public void connect(MediaSink sink) {
        if (sink == null) {
            throw new IllegalArgumentException("Other party can not be nul");
        }
        AbstractSource source = createSource(sink);
        source.connect(sink);
        
//        source.start();
        sources.put(sink.getId(), source);
        
    }

    @Override
    public void disconnect(MediaSink otherParty) {
        AbstractSource source = sources.remove(otherParty.getId());
        if (source == null) {
            throw new IllegalArgumentException(otherParty + " is not connected to " + this);
        }
        source.stop();
        source.disconnect(otherParty);
    }

    public abstract AbstractSource createSource(MediaSink otherParty);
    public abstract void destroySource(AbstractSource source);
    
    public int getActiveSourceCount() {
        return sources.size();
    }
}
