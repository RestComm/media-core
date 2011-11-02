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
package org.mobicents.media.server.spi.player;

import java.io.IOException;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.listener.TooManyListenersException;

/**
 *
 * @author kulikov
 */
public interface Player {
    /**
     * Assigns URL to play media from.
     * 
     * @param url the url to media file.
     * @throws java.io.IOException if file can not be read
     * @throws java.io.ResourceUnavailableException if file is not supported.
     */
    public void setURL(String url) throws IOException, ResourceUnavailableException;
    
    /**
     * Starts media processing.
     */
    public void start();
    
    /**
     * Terminates media processing.
     */
    public void stop();
    
    public void setMaxDuration(long duration);
    public void setMediaTime(long timestamp);
    
    public void setInitialDelay(long delay);
    
    public void addListener(PlayerListener listener) throws TooManyListenersException;
    public void removeListener(PlayerListener listener);
    

}
