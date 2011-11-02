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

import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.server.impl.AbstractSource;

/**
 * Implements output stream of the audio mixer.
 * 
 * @author Oleg Kulikov
 */
public class MixerOutput extends AbstractSource {

    private AudioMixer mixer;
    
    /**
     * Creates new ouput stream
     * @param mixer
     */
    public MixerOutput(AudioMixer mixer) {
        super("AudioMixer[Output]");
        this.mixer = mixer;
    }
    
    @Override
    public void evolve(Buffer buffer, long timestamp) {
        mixer.evolve(buffer, timestamp);
        buffer.setTimeStamp(timestamp);

        buffer.setFormat(AudioMixer.LINEAR);
        buffer.setEOM(false);
        buffer.setDiscard(false);
    }    

    
    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.MediaSource#getFormats() 
     */
    public Format[] getFormats() {
        return AudioMixer.formats;
    }

}
