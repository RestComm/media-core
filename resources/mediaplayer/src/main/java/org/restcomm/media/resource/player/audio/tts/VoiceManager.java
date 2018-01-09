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

package org.restcomm.media.resource.player.audio.tts;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.speech.freetts.Voice;

/**
 * This is set-up class for various Voice available for FreeTTS.
 * 
 * @author amit bhayani
 * 
 */
public class VoiceManager {

	private static final Logger logger = LogManager.getLogger(VoiceManager.class);

	private static final VoiceManager INSTANCE = new VoiceManager();

	private HashMap<String, VoiceFactory> voiceFactories = new HashMap<String, VoiceFactory>();

	private VoiceManager() {
		setupKevin();
		setupAlan();
		setupMbrola();

		if (logger.isInfoEnabled()) {
			StringBuffer b = new StringBuffer();

			for (String name : this.voiceFactories.keySet()) {
				b.append(name);
				b.append(", ");
			}
			logger.info("Available voices for TTS are : " + b.toString());
		}
	}

	private void setupKevin() {
		KevinVoiceFactory kevinVoiceFactory = new KevinVoiceFactory();
		Voice[] voices = kevinVoiceFactory.getVoices();
		for (Voice v : voices) {
			voiceFactories.put(v.getName(), kevinVoiceFactory);
		}
	}

	private void setupAlan() {
		AlanVoiceFactory alanVoiceFactory = new AlanVoiceFactory();
		Voice[] voices = alanVoiceFactory.getVoices();

		for (Voice v : voices) {
			voiceFactories.put(v.getName(), alanVoiceFactory);
		}
	}

	private void setupMbrola() {
		MbrolaVoiceFactory mbrolaVoiceFactory = new MbrolaVoiceFactory();
		Voice[] voices = mbrolaVoiceFactory.getVoices();
		for (Voice v : voices) {
			voiceFactories.put(v.getName(), mbrolaVoiceFactory);
		}
	}

	public static VoiceManager getInstance() {
		return INSTANCE;
	}

	public Voice getVoice(String voiceName) {
		Voice voice = null;
		VoiceFactory voiceFactory = this.voiceFactories.get(voiceName);
		if (voiceFactory != null) {
			voice = voiceFactory.getVoice(voiceName);
		}
		return voice;
	}

}
