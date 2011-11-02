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

import org.mobicents.media.Component;
import org.mobicents.media.ComponentFactory;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerFactory;
import org.mobicents.media.server.impl.resource.mediaplayer.video.VideoPlayerFactory;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * @author baranowb
 * @author kulikov
 */
public class MediaPlayerFactory implements ComponentFactory {

	private String name;

	private AudioPlayerFactory audioPlayerFactory;
	private VideoPlayerFactory videoPlayerFactory;

	public AudioPlayerFactory getAudioPlayerFactory() {
		return audioPlayerFactory;
	}

	public void setAudioPlayerFactory(AudioPlayerFactory audioPlayerFactory) {
		this.audioPlayerFactory = audioPlayerFactory;
	}

	public VideoPlayerFactory getVideoPlayerFactory() {
		return videoPlayerFactory;
	}

	public void setVideoPlayerFactory(VideoPlayerFactory videoPlayerFactory) {
		this.videoPlayerFactory = videoPlayerFactory;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Component newInstance(Endpoint endpoint)
			throws ResourceUnavailableException {
		return new MediaPlayerImpl(name, this.audioPlayerFactory,
				this.videoPlayerFactory);

	}

	public void start() throws IllegalStateException {
		if (audioPlayerFactory == null) {
			throw new IllegalStateException("Audio media factory is not set!");
		}

		if (videoPlayerFactory == null) {
			throw new IllegalStateException("Video media factory is not set!");
		}
	}

}
