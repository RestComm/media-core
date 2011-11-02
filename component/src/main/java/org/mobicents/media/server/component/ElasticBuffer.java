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

package org.mobicents.media.server.component;

import org.mobicents.media.server.spi.memory.Frame;

/**
 * Elastic buffer is asynchronous FIFO buffer wich automatically compensates
 * for drift between receiver clock and transmitter clock signals.
 *
 * @author kulikov
 */
public class ElasticBuffer {
    /** the initial delay of signal measured in number of frames */
    private int delay;

    /** The limit of the buffer in frames */
    private int limit;

    /** backing array */
    private Frame[] buffer;

    /** reader indexes */
    private int rs, ro;

    /** writer indexes */
    private int ws, wo = -1;

    /** frame for reading */
    private Frame frame;

    /** buffer state */
    private boolean isReady;

    /** buffer state monitor */
    private BufferListener listener;
    private final Object LOCK = new Object();
    /**
     * Creates new elastic buffer.
     *
     * @param delay the initial delay of signal measured in frames
     * @param limit the physical limit of the buffer measured in frames.
     */
    public ElasticBuffer(int delay, int limit) {
        this.delay = delay;
        this.limit = limit;
        this.buffer = new Frame[limit];
        this.isReady = false;
    }

    /**
     * Sets buffer state monitor.
     * 
     * @param listener the monitor implementation.
     */
    public void setListener(BufferListener listener) {
        this.listener = listener;
    }

    /**
     * Writes given frame to the tail of the buffer.
     *
     * @param frame the frame to write
     */
    public void write(Frame frame) {
    	synchronized(LOCK) {
    		this.updateWrite();
        
    		if (this.wo == this.ro && this.isReady) {
    			//writer reaches reader from left? shift reader.
    			this.updateRead();
    		}

    		buffer[wo] = frame;

    		if (!this.isReady && (this.writeIndex() - this.readIndex()) >= delay) {
    			this.isReady = true;
    			if (listener != null) listener.onReady();
    		}
    	}
    }

    /**
     * Retreives and removes frame from the head of the buffer.
     *
     * @return the frame or null if buffer is empty.
     */
    public Frame read() {
    	synchronized(LOCK) {
    		if (!this.isReady) {
    			return null;
    		}
        
    		frame = buffer[ro];
    		buffer[ro] = null;
        
    		this.updateRead();

    		if (this.readIndex() - 1 == this.writeIndex()) {
    			this.isReady = false;
    		}

    		return frame;
    	}
    }

    /**
     * Gets the absolute index of reader position buffer.
     *
     * @return the index value
     */
    private long readIndex() {
        return rs * limit + ro;
    }

    /**
     * Gets the absolute index of writer position in buffer.
     *
     * @return the index value
     */
    private long writeIndex() {
        return ws * limit + wo;
    }

    /**
     * Modify write position on one step
     */
    private void updateWrite() {
        wo++;
        if (wo == limit) {
            wo = 0;
            ws++;
        }
    }

    /**
     * Modify reader position on one step.
     */
    private void updateRead() {
        ro++;
        if (ro == limit) {
            ro = 0;
            rs++;
        }
    }

    public void clear() {
        ro = 0; rs = 0;
        wo = 0; ws = 0;
        
        this.isReady = false;
    }

    public boolean isEmpty() {
        return !this.isReady;
    }
}
