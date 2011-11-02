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

package org.mobicents.media.server.impl;

import java.util.concurrent.ConcurrentLinkedQueue;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.spi.io.Pipe;
import org.mobicents.media.server.spi.memory.Frame;

/**
 * Pipe implementation.
 * 
 * @author kulikov
 */
public class PipeImpl implements Pipe {
    //The inner buffer limit
    private final static int limit = 50;

    //source connected to this pipe
    protected MediaSource source;
    //sink connected to this pipe
    protected MediaSink sink;

    //inner buffer
    private volatile ConcurrentLinkedQueue<Frame> buffer = new ConcurrentLinkedQueue();

    //transmission statistics
    private int rxPackets;
    private int txPackets;

    private boolean isDebug;
    private long txTimestamp;
    
    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }
    
    
    /**
     * Writes frame into the inner buffer.
     * 
     * @param frame the frame to write
     */
    protected void write(Frame frame) {
        //ignore duplicate
        if (frame.getTimestamp() == txTimestamp && txTimestamp > 0) {
            System.out.println("Drop packet");
            return;
        }
        
        if (this.isDebug) {
            System.out.print(frame.getTimestamp() + " ");
            dump(frame.getData(), frame.getOffset(), frame.getLength());
            System.out.println();
            
        }
        //free space from queue's head
        if (buffer.size() == limit) {
            buffer.poll();
        }

        //queue frame
        buffer.offer(frame);
        txTimestamp = frame.getTimestamp();
        
        //send notification to the sink
        if (sink != null) {
            ((AbstractSink)sink).wakeup();
        }

        this.rxPackets++;
    }

    /**
     * Reads the frame from inner buffer and removes it from inner buffer.
     *
     * @return the packet read.
     */
    protected Frame read() {
        this.txPackets++;
        return buffer.poll();
    }

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.io.Pipe#connect(org.mobicents.media.MediaSource)
     */
    public void connect(MediaSource source) {
        if (!(source instanceof AbstractSource)) {
            throw new IllegalArgumentException(source + " can not be connected");
        }
        source.connect(this);
    }

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.io.Pipe#connect(org.mobicents.media.MediaSink)
     */
    public void connect(MediaSink sink) {
        if (!(sink instanceof AbstractSink)) {
            throw new IllegalArgumentException(sink + " can not be connected");
        }
        sink.connect(this);
    }

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.io.Pipe#disconnect(int)
     */
    public void disconnect(int termination) {
        if (termination == INPUT && source != null) {
            source.disconnect(this);
            return;
        }

        if (termination == OUTPUT && sink != null) {
            sink.disconnect(this);
            return;
        }
    }

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.io.Pipe#disconnect() 
     */
    public void disconnect() {
        if (source != null) {
            source.disconnect(this);
        }

        if (sink != null) {
            sink.disconnect(this);
        }
    }

    /**
     * Gets the current size of inner buffer.
     *
     * @return the number of frames inside inner buffer.
     */
    public int size() {
        return buffer.size();
    }

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.io.Pipe#start()
     */
    public void start() {
        if (source != null && sink != null) {
            //clear buffer
            buffer.clear();            
            this.txTimestamp=0;
            
            //start source and sink
            source.start();
            sink.start();

            this.rxPackets = 0;
            this.txPackets = 0;            
        }
    }

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.io.Pipe#stop()
     */
    public void stop() {    	        
        if (source != null && sink != null) {
            source.stop();
            sink.stop();
        }
    }

    public int getRxPackets() {
        return this.rxPackets;
    }

    public int getTxPackets() {
        return this.txPackets;
    }
    
    private void dump(byte[] data, int offset, int len) {
        for (int i = 0; i < len; i++) {
            System.out.print(data[i + offset] + " ");
        }
    }
}
