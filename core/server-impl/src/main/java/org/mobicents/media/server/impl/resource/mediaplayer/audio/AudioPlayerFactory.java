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
package org.mobicents.media.server.impl.resource.mediaplayer.audio;

import java.util.HashMap;
import java.util.Map;

import org.mobicents.media.ComponentFactory;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.tts.VoicesCache;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * @author baranowb
 * @author kulikov
 */
public class AudioPlayerFactory implements ComponentFactory {

    private String name;
    private String audioMediaDirectory;
    private VoicesCache voiceCache = new VoicesCache();
    private Map<String, Integer> voices = new HashMap<String, Integer>();

    public Map<String, Integer> getVoices() {
        return voices;
    }

    public void setVoices(Map<String, Integer> voices) {
        if (voices != null) {
            this.voices.putAll(voices);
        }

        try {
            voiceCache.clear();
            voiceCache.init(this.voices);
        } catch (Exception e) {
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the audioMediaDirectory
     */
    public String getAudioMediaDirectory() {
        return audioMediaDirectory;
    }

    /**
     * @param audioMediaDirectory
     *            the audioMediaDirectory to set
     */
    public void setAudioMediaDirectory(String audioMediaDirectory) {
        this.audioMediaDirectory = audioMediaDirectory;
    }

    public AudioPlayerImpl newInstance(Endpoint endpoint)
            throws ResourceUnavailableException {
        AudioPlayerImpl p = new AudioPlayerImpl(name, audioMediaDirectory,
                voiceCache);
        p.setEndpoint(endpoint);
        return p;
    }
}
