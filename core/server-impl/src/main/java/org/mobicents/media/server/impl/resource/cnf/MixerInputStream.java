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
package org.mobicents.media.server.impl.resource.cnf;

import java.io.IOException;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.server.impl.AbstractSink;

/**
 *
 * @author Oleg Kulikov
 */
public class MixerInputStream extends AbstractSink {

    protected AudioMixer mixer;
    private byte[] media = new byte[320];

    /** media buffer */
    private byte[] localBuffer = new byte[320];
    private long duration;
    
    /** read and write cursor positions */
    private int r,w;
    protected Object header;
    
    /** 
     * Creates new input stream.
     * 
     * @param mixer
     * @param jitter
     */
    public MixerInputStream(AudioMixer mixer) {
        super("MixerInputStream");
        this.mixer = mixer;
    }

    @Override
    public String getId() {
        return mixer.getId();
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.impl.AbstractSink#onMediaTransfer(org.mobicents.media.Buffer) 
     */
    public void onMediaTransfer(Buffer buffer) throws IOException {
        //save header first
        this.header = buffer.getHeader();
        
        //process with data
        byte[] data = buffer.getData();
        int remainder = (localBuffer.length - w) - buffer.getLength();        
        //data completely fits to the buffer? just append data
        if (remainder >= 0) {
            System.arraycopy(data, buffer.getOffset(), localBuffer, w, buffer.getLength());
            //move write index behind the last byte of data writen
            inc(w, buffer.getLength());
            duration += buffer.getDuration();
            return;
        }
        //data does not fit into the local buffer's free space at end of buffer
        //copying what we can
        remainder = -remainder;
        System.arraycopy(data, buffer.getOffset(), localBuffer, w, buffer.getLength() - remainder);

        //place in the free space at begining of buffer
        if (remainder > r) {
            remainder = r;
        }
        
        System.arraycopy(buffer.getData(), 
        buffer.getOffset() + (buffer.getLength() - remainder), 
        localBuffer, 0, remainder);
        w = remainder;
        duration += buffer.getDuration();
    }

    /**
     * Reads media buffer from this stream with specified duration.
     * 
     * @param duration the duration of the requested buffer in milliseconds.
     * @return buffer which contains duration ms media for 8000Hz, 16bit, linear audio.
     */
    public byte[] read(int duration) {
        int len = 320;
        //clean all data
        for (int i = 0; i < media.length; i++) {
            media[i] = 0;
        }
        if (this.duration > 0) {
            int remainder = (localBuffer.length - r) - len;
            if (remainder >= 0) {
                System.arraycopy(localBuffer, r, this.media, 0, len);
                inc(r, len);
            } else {
                System.arraycopy(localBuffer, r, media, 0, len - remainder);
                System.arraycopy(localBuffer, 0, media, len - remainder, remainder);
                r = remainder;
            }
            this.duration -= duration;
        }
        return media;
    }

    private int inc(int index, int amount) {
        index += amount;
        return index < this.localBuffer.length ? index : localBuffer.length - index;
    }
    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.MediaSink#getFormats() 
     */
    public Format[] getFormats() {
        return AudioMixer.formats;
    }

    @Override
    public String toString() {
        return mixer.toString();
    }
}
