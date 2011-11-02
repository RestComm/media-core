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
package org.mobicents.media.server.impl.resource.audio;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;
import org.mobicents.media.Buffer;

/**
 * Implements cache for recorded audio.
 * 
 * Recorder implementation uses <code>push(Buffer)</code> method to 
 * fill the cache. Java Sound AudioSystem can access this cache as
 * <code>java.io.InputStream</code> for constructing internal 
 * <code>AudioInputStream</code>.
 * 
 * The cache is locked for reading until will be explicitly unblocked by Recorder.
 * 
 * 
 * @author Oleg Kulikov
 */
public class RecorderCache extends InputStream {

    private int available = 0;
    private int offset;
    
    protected Semaphore semaphore = new Semaphore(0);
    protected volatile boolean blocked = false;
    
    private int bufferSize = 16000 * 60;
    private byte[] localBuffer = new byte[bufferSize];


    @Override
    public int available() {
        return available;
    }

    @Override
    public int read() throws IOException {
        if (available == 0) {
            return -1;
        }

        if (blocked) {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                throw new IOException(e.getMessage());
            }
        }
        
        available--;
        int b =localBuffer[offset++] & 0xff; 
        return b;
    }

    /**
     * Duplicates size of local buffer.
     */
    private void resizeLocalBuffer() {
        //save data in temp array
        byte[] temp = new byte[available];
        System.arraycopy(localBuffer, 0, temp, 0, available);
        
        //resize local bufer and restore data
        localBuffer = new byte[2 * localBuffer.length];        
        System.arraycopy(temp, 0, localBuffer, 0, available);
    }
    
    /**
     * Appends data from media buffer to the local buffer.
     * 
     * @param buffer the buffer which contains media data
     */
    protected void push(Buffer buffer) {
        int remainder = localBuffer.length - available;
        boolean enouphSpace = remainder > buffer.getLength();
        
        //@FIXME
        //Do not extend buffer without limit! Use disk cache instead!
        if (!enouphSpace) {
            resizeLocalBuffer();
        }
        
            byte[] data = buffer.getData();
            System.arraycopy(data, buffer.getOffset(), localBuffer, available, buffer.getLength());
            available += data.length;
    }
    
    /**
     * Unblocks cache and allow reading from this cache.
     */
    protected void unblock() {
        semaphore.release();
    }
}
