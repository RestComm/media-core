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
package org.mobicents.media.server.impl.resource.mediaplayer;

import java.io.IOException;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;

/**
 *
 * @author kulikov
 */
public interface Track {
    /**
     * Gets the format of this audio stream.
     * 
     * @return the format object.
     */
    public Format getFormat();
    
    /**
     * Gets the current media time.
     * 
     * @return time value expressed in milliseconds.
     */
    public long getMediaTime();
    
    /**
     * Rewinds track to the specified timestamp.
     * 
     * @param  timestamp the value of the time in millisconds
     */
    public void setMediaTime(long timestamp);
    
    /**
     * Gets duration of this track.
     * 
     * @return duration expressed in milliseconds.
     */
    public long getDuration();
    
    /**
     * Fills buffer with next portion of media data.
     * @param buffer
     */
    public void process(Buffer buffer) throws IOException;
    
    /**
     * Closes this stream.
     */
    public void close();

}
