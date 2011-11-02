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
package org.mobicents.media.server.impl.resource.prelay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.Server;
import org.mobicents.media.server.ConnectionImpl;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSinkSet;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.AbstractSourceSet;
import org.mobicents.media.server.impl.BaseComponent;
import org.mobicents.media.server.impl.dsp.DspFactory;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceGroup;

/**
 *
 * @author kulikov
 */
public class Bridge extends BaseComponent implements ResourceGroup {
    
    private AudioChannel[] audioChannels = new AudioChannel[2];
    private Input input;
    private Output output;

    
    private static ArrayList<MediaType> mediaTypes = new ArrayList();
    static {
        mediaTypes.add(MediaType.AUDIO);
    }
    
    public Bridge(String name, DspFactory dspFactory, Endpoint endpoint) {
        super(name);
        
        audioChannels[0] = new AudioChannel(dspFactory);
        audioChannels[1] = new AudioChannel(dspFactory);
        
        input = new Input("PacketRelay[Input]");
        output = new Output("PacketRelay[Output]");
    }

    public Collection<MediaType> getMediaTypes() {
        return mediaTypes;
    }
    
    public MediaSource getSource(MediaType media) {
        return media == MediaType.AUDIO ? output : null;
    }
    
    public MediaSink getSink(MediaType media) {
        return media == MediaType.AUDIO ? input : null;
    }

    public void start() {
//        processors[0].start();
//        processors[1].start();
    }

    public void stop() {
//        processors[0].stop();
//        processors[1].stop();
    }
    
    private class Input extends AbstractSinkSet {

        public Input(String name) {
            super(name);
        }
        
        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        public Format[] getFormats() {
            return null;
        }

        public boolean isAcceptable(Format format) {
            return false;
        }

        @Override
        public void onMediaTransfer(Buffer buffer) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public AbstractSink createSink(MediaSource otherParty) {
            ConnectionImpl connection = (ConnectionImpl) otherParty.getConnection();
            int idx = connection.getIndex();
//            return (AbstractSink) processors[idx].getInput();
            return (AbstractSink) audioChannels[idx].getInput();
        }

        @Override
        public void destroySink(AbstractSink sink) {
        }

        
    }
    
    private class Output extends AbstractSourceSet {

        public Output(String name) {
            super(name);
        }
        

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public void evolve(Buffer buffer, long timestamp) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Format[] getFormats() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public AbstractSource createSource(MediaSink otherParty) {
            ConnectionImpl connection = (ConnectionImpl) otherParty.getConnection();
            int idx = Math.abs(connection.getIndex() - 1);
//            return (AbstractSource) processors[idx].getOutput();
            return (AbstractSource) audioChannels[idx].getOutput();
        }

        @Override
        public void destroySource(AbstractSource source) {
        }
        
    }

}
