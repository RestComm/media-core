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
package org.mobicents.media.server.spi.rtp;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;
import org.mobicents.media.Format;
import org.mobicents.media.format.AudioFormat;
import org.mobicents.media.format.VideoFormat;

/**
 * Defines relation between audio/video format and RTP payload number as
 * specified by Audio/Video Profile spec.
 * 
 * @author Oleg Kulikov
 */
public class AVProfile implements Cloneable {

    public final static String AUDIO = "audio";
    public final static String VIDEO = "video";
    
    public final static AudioFormat PCMU = new AudioFormat(AudioFormat.ULAW, 8000, 8, 1);
    public final static AudioFormat PCMA = new AudioFormat(AudioFormat.ALAW, 8000, 8, 1);
    public final static AudioFormat SPEEX = new AudioFormat(AudioFormat.SPEEX, 8000, AudioFormat.NOT_SPECIFIED, 1);
    public final static AudioFormat G729 =  new AudioFormat(AudioFormat.G729, 8000, AudioFormat.NOT_SPECIFIED, 1);
    public final static AudioFormat GSM =  new AudioFormat(AudioFormat.GSM, 8000, AudioFormat.NOT_SPECIFIED, 1);
    public final static AudioFormat MPEG4_GENERIC = new AudioFormat("mpeg4-generic", 8000, AudioFormat.NOT_SPECIFIED, 2);
    public final static AudioFormat L16_STEREO =  
            new AudioFormat(AudioFormat.LINEAR, 44100, 16, 2, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);
    public final static AudioFormat L16_MONO =  
            new AudioFormat(AudioFormat.LINEAR, 44100, 16, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);
    public final static AudioFormat DTMF =  new AudioFormat("telephone-event", 8000, AudioFormat.NOT_SPECIFIED, AudioFormat.NOT_SPECIFIED);
    
    public final static VideoFormat H261 =  new VideoFormat(VideoFormat.H261, 25, 90000);
    public final static VideoFormat MP4V =  new VideoFormat("MP4V-ES", 25, 90000);
    public final static VideoFormat H263 =  new VideoFormat("H263", 25, 90000);
    
    private final HashMap<Integer, AudioFormat> audio = new HashMap();
    private final HashMap<Integer, VideoFormat> video = new HashMap();
    
    
    public AVProfile() {
        audio.put(0, PCMU);
        audio.put(8, PCMA);
        //audio.put(97, SPEEX);
        audio.put(2, G729);
        audio.put(3, GSM);
        audio.put(16, L16_STEREO);
        audio.put(17, L16_MONO);        
        audio.put(101, DTMF);        
        video.put(45, H261);
        video.put(34, H263);
        //video.put(96, MP4V);
    }
    
    public void setProfile(Hashtable<Integer, Format> profile) {
        audio.clear();
        video.clear();
        Set<Integer> keys = profile.keySet();
        for (Integer key: keys) {
            Format f = profile.get(key);
            if (f instanceof AudioFormat) {
                audio.put(key,(AudioFormat) f);
            } else if (f instanceof VideoFormat) {
                video.put(key,(VideoFormat) f);
            }
        }
    }
    
    public Hashtable<Integer, Format> getProfile() {
        Hashtable<Integer, Format> profile = new Hashtable();
        profile.putAll(audio);
        profile.putAll(video);
        return profile;
    }
    
    public HashMap<Integer, AudioFormat> getAudioFormats() {
        return audio;
    }
    
    public HashMap<Integer, VideoFormat> getVideoFormats() {
        return video;
    }
    
    /**
     * Gets the audio format related to payload type.
     * 
     * @param pt the payload type
     * @return AudioFormat object.
     */
    public AudioFormat getAudioFormat(int pt) {
        return audio.get(pt);
    }

    /**
     * Gets the video format related to payload type.
     * 
     * @param pt the payload type
     * @return VideoFormat object.
     */
    public VideoFormat getVideoFormat(int pt) {
        return video.get(pt);
    }
    
    @Override
    public AVProfile clone() {
        AVProfile profile = new AVProfile();
        profile.setProfile(this.getProfile());
        return profile;
    }
}
