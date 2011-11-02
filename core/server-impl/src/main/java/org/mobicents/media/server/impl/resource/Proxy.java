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
package org.mobicents.media.server.impl.resource;

import java.io.IOException;
import java.util.Collection;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.Inlet;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.Outlet;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.BaseComponent;

/**
 * Implements media proxy component. 
 * 
 * This component can be used to join media source and media.
 * 
 * @author kulikov
 */
public class Proxy extends BaseComponent implements Inlet, Outlet {

    //input channel
    private Input input;
    //output channel
    private Output output;
    
    //by default proxy can transmitt any format
    private Format[] formats = new Format[]{Format.ANY};
    
    //currently processes buffer
    private Buffer buff;
    
    //timestamp of the currently processed media buffer
    private long timestamp;
    
    /**
     * Creates new instance of the proxy.
     * 
     * @param name the name of the component
     */
    public Proxy(String name) {
        super(name);
        input = new Input(name);
        output = new Output(name);
    }
    
    /**
     * Gets the list of currently supported formats.
     * 
     * @return list of supported formats;
     */
    public Format[] getFormats() {
        return formats;
    }
    
    /**
     * Chanhes the list of supportd formats.
     * 
     * By default Proxy can transmit any formats but user can restrict transition 
     * capabilities
     * 
     * @param formats the list of supported formats.
     */
    public void setFormat(Format[] formats) {
        this.formats = formats;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.Component#start() 
     */
    public void start() {
        //start input and output channel
        if (!input.isStarted()) {
            input.start();
        }        
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.Component#stop() 
     */
    public void stop() {
        if (input.isStarted()) {
            input.stop();
        }        
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.Inlet#getInput(). 
     */
    public MediaSink getInput() {
        return input;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.Inlet#getOutput(). 
     */
    public MediaSource getOutput() {
        return output;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.Inlet#connect(org.mobicents.media.MediaSource) 
     */
    public void connect(MediaSource source) {
        input.connect(source);
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.Inlet#disconnect(org.mobicents.media.MediaSource) 
     */
    public void disconnect(MediaSource source) {
        input.disconnect(source);
    }


    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.Inlet#connect(org.mobicents.media.MediaSink) 
     */
    public void connect(MediaSink sink) {
        output.connect(sink);
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.Inlet#connect(org.mobicents.media.MediaSink) 
     */
    public void disconnect(MediaSink sink) {
        output.disconnect(sink);
    }

    /**
     * Implements input channel.
     */
    private class Input extends AbstractSink {

        private volatile boolean started = false;
        /**
         * Creates new instance of input channel
         * 
         * @param name the name of proxy component
         */
        public Input(String name) {
            super(name + "[input]");
        }
        
        @Override
        public boolean isStarted() {
            return this.started;
        }
        
        @Override
        public void start() {
            this.started = true;
            if (!output.isStarted()) {
                output.start();
            }
            if (otherParty != null && !otherParty.isStarted()) {
                otherParty.start();
            }
        }
        
        @Override
        public void stop() {
            this.started = false;
            if (output.isStarted()) {
                output.stop();
            }
            if (otherParty != null && otherParty.isStarted()) {
                otherParty.stop();
            }
        }
        
        @Override
        public void onMediaTransfer(Buffer buffer) throws IOException {
            buff = buffer;
            timestamp = buffer.getTimeStamp();
            output.perform();
        }

        @Override
        public Format selectPreffered(Collection<Format> set) {
            //proxy itslef can not choose preffered format
            //if output channel is conncted then output's sink should select preffered
            //other wise this method will return null what means that preffred format
            //was not choosen
            return output.isConnected() ? output.getOtherPartyPreffered(set) : null;
        }
        
        
        /**
         * (Non Java-doc).
         * 
         * @see org.mobicents.media.MediaSink#getFormats() 
         */
        public Format[] getFormats() {
            //the logic of this method is follows:
            //if output channel is connected then this call should be delegated to
            //output's sink and if output is not connected yet then return the default list
            //of supported formats.
            return output.isConnected() ? output.getOtherPartyFormats() : formats ;
        }
        
        /**
         * Gets the list of formats supported by media source connected to 
         * this channel
         * 
         * @return return list of supported formats
         */
        public Format[] getOtherPartyFormats() {
            return otherParty.getFormats();
        }

        public long getTimestamp() {
            return timestamp;
        }

    }
    
    /**
     * Implements output channel
     */
    private class Output extends AbstractSource  {

        private volatile boolean started = false;
        
        /**
         * Creates new instance of output channel.
         * 
         * @param name the name of proxy.
         */
        public Output(String name) {
            super(name + "[output]");
        }
        
        @Override
        public boolean isStarted() {
            return this.started;
        }
        
        @Override
        public void start() {
            this.started = true;
            if (!input.isStarted()) {
                input.start();
            }
            if (otherParty != null && !otherParty.isStarted()) {
                otherParty.start();
            }            
        }
        
        @Override
        public void stop() {
            this.started = false;
            if (input.isStarted()) {
                input.stop();
            }
            if (otherParty != null && otherParty.isStarted()) {
                otherParty.stop();
            }
        }

        @Override
        public void setPreffered(Format format) {
            //doing default job
            super.setPreffered(format);
            //the output should also notify input's source which format 
            //is selected as preffered
            input.assignPreffered(format);
        }
        
        /**
         * Asks the sink connected to this channel to determine preffred format
         * 
         * @param set the set of format to choose preffred from
         * @return the selected preffred format
         */
        public Format getOtherPartyPreffered(Collection<Format> set) {
            return ((AbstractSink)otherParty).getPreffered(set);
        }
        
        @Override
        public void evolve(Buffer buffer, long timestamp) {
            if (buff != null) {
                buffer.copy(buff);
            }
        }

        /**
         * Gets the list of formats supported by media sink connected to 
         * this channel
         * 
         * @return return list of supported formats
         */
        public Format[] getOtherPartyFormats() {
            return otherParty.getFormats();
        }
        
        /**
         * (Non Java-doc).
         * 
         * @see org.mobicents.media.MediaSource#getFormats() 
         */
        public Format[] getFormats() {
            //this method works as proxy:
            //if media source is connected to input then media source about formats
            //if nothing is connected to proxy return list of default formats.
            return input.isConnected() ? input.getOtherPartyFormats() : formats ;
        }

    }
}
