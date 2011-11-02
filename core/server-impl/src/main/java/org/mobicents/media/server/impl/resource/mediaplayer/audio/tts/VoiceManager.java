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
package org.mobicents.media.server.impl.resource.mediaplayer.audio.tts;

import java.util.HashMap;

import org.apache.log4j.Logger;

import com.sun.speech.freetts.Voice;

/**
 * This is set-up class for various Voice available for FreeTTS.
 * 
 * @author amit bhayani
 * 
 */
public class VoiceManager {

	private static final Logger logger = Logger.getLogger(VoiceManager.class);

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
