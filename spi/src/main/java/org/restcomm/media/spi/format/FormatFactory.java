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

package org.restcomm.media.spi.format;

import org.restcomm.media.spi.format.audio.DTMFFormat;
import org.restcomm.media.spi.utils.Text;

/**
 * Constructs format descriptor object.
 * 
 * @author kulikov
 */
public class FormatFactory {
    private static Text DTMF = new Text("telephone-event");

    /**
     * Creates new audio format descriptor.
     *
     * @param name the encoding name.
     */
    public static AudioFormat createAudioFormat(EncodingName name) {
        //check name and create specific
        if (name.equals(DTMF)) {
            return new DTMFFormat();
        }

        //default format
        return new AudioFormat(name);
    }

    /**
     * Creates new format descriptor
     *
     * @param name the encoding
     * @param sampleRate sample rate value in Hertz
     * @param sampleSize sample size in bits
     * @param channels number of channels
     */
    public static AudioFormat createAudioFormat(EncodingName name, int sampleRate, int sampleSize, int channels) {
        AudioFormat fmt = createAudioFormat(name);
        fmt.setSampleRate(sampleRate);
        fmt.setSampleSize(sampleSize);
        fmt.setChannels(channels);
        return fmt;
    }

    /**
     * Creates new format descriptor
     *
     * @param name the encoding
     * @param sampleRate sample rate value in Hertz
     * @param sampleSize sample size in bits
     * @param channels number of channels
     */
    public static AudioFormat createAudioFormat(String name, int sampleRate, int sampleSize, int channels) {
        AudioFormat fmt = createAudioFormat(new EncodingName(name));
        fmt.setSampleRate(sampleRate);
        fmt.setSampleSize(sampleSize);
        fmt.setChannels(channels);
        return fmt;
    }

    /**
     * Creates new format descriptor
     *
     * @param name the encoding
     * @param sampleRate sample rate value in Hertz
     */
    public static AudioFormat createAudioFormat(String name, int sampleRate) {
        AudioFormat fmt = createAudioFormat(new EncodingName(name));
        fmt.setSampleRate(sampleRate);
        return fmt;
    }

    /**
     * Creates new format descriptor.
     *
     * @param name format encoding name.
     * @param  frameRate the number of frames per second.
     */
    public static VideoFormat createVideoFormat(EncodingName name, int frameRate) {
        //TODO : implement specific format here
        return new VideoFormat(name, frameRate);
    }

    /**
     * Creates new format descriptor.
     *
     * @param name format encoding name.
     */
    public static VideoFormat createVideoFormat(EncodingName name) {
        return new VideoFormat(name);
    }

    /**
     * Creates new format descriptor.
     *
     * @param name format encoding name.
     */
    public static VideoFormat createVideoFormat(String name) {
        return new VideoFormat(name);
    }

    /**
     * Creates new format descriptor.
     *
     * @param name format encoding name.
     * @param  frameRate the number of frames per second.
     */
    public static VideoFormat createVideoFormat(String name, int frameRate) {
        return new VideoFormat(name, frameRate);
    }

	/**
	 * Creates a new format descriptor for application line
	 * 
	 * @param name
	 *            format encoding name
	 * @return the format descriptor
	 */
    public static ApplicationFormat createApplicationFormat(EncodingName name) {
    	return new ApplicationFormat(name);
    }
    
	/**
	 * Creates a new format descriptor for application line
	 * 
	 * @param name
	 *            format encoding name
	 * @return the format descriptor
	 */
    public static ApplicationFormat createApplicationFormat(String name) {
    	return new ApplicationFormat(name);
    }

}
