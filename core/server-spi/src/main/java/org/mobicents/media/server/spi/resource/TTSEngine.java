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

package org.mobicents.media.server.spi.resource;

/**
 *
 * @author kulikov
 */
public interface TTSEngine extends Player {
    /**
     * Assign the voice name to speak
     * 
     * @param voiceName the name of the voice.
     */
    public void setVoiceName(String voiceName);
    
    /**
     * Gets the currently used voice name.
     * 
     * @return the name of the voice.
     */
    public String getVoiceName();
    
    /**
     * Sets the volume level of the speech.
     * 
     * @param volume the volume level.
     */
    public void setVolume(int volume);
    
    /**
     * Gets currently used volume level.
     * 
     * @return the volume level.
     */
    public int getVolume();
    
    /**
     * Sets text to speech.
     * 
     * @param text the text to speech.
     */
    public void setText(String text);
}
