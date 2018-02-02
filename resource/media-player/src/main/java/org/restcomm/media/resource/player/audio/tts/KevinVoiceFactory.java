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

import java.net.URL;
import java.util.Locale;

import com.sun.speech.freetts.Age;
import com.sun.speech.freetts.Gender;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.en.us.CMUDiphoneVoice;
import com.sun.speech.freetts.en.us.CMULexicon;
import com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory;

/**
 * Factory for Kevin Voice's
 * @author amit bhayani
 *
 */
public class KevinVoiceFactory extends VoiceFactory {

	public static final String KEVIN_VOICE = "kevin";

	public static final String KEVIN16_VOICE = "kevin16";

	public final URL cmu_us_kal_url = KevinVoiceDirectory.class.getResource("cmu_us_kal.bin");
	public final URL cmu_us_kal16_url = KevinVoiceDirectory.class.getResource("cmu_us_kal16.bin");

	@Override
	public Voice getVoice(String voiceName) {
		Voice voice = null;
		if (KEVIN_VOICE.compareTo(voiceName) == 0) {
			CMULexicon lexicon = new CMULexicon("cmulex");
			voice = new CMUDiphoneVoice("kevin", Gender.MALE, Age.YOUNGER_ADULT, "default 8-bit diphone voice",
					Locale.US, "general", "cmu", lexicon, cmu_us_kal_url);
		} else if (KEVIN16_VOICE.compareTo(voiceName) == 0) {
			CMULexicon lexicon = new CMULexicon("cmulex");
			voice = new CMUDiphoneVoice(KEVIN16_VOICE, Gender.MALE, Age.YOUNGER_ADULT, "default 16-bit diphone voice",
					Locale.US, "general", "cmu", lexicon, cmu_us_kal16_url);
		}
		return voice;
	}

	@Override
	public Voice[] getVoices() {

		CMULexicon lexicon = new CMULexicon("cmulex");
		Voice kevin = new CMUDiphoneVoice(KEVIN_VOICE, Gender.MALE, Age.YOUNGER_ADULT, "default 8-bit diphone voice",
				Locale.US, "general", "cmu", lexicon, cmu_us_kal_url);
		Voice kevin16 = new CMUDiphoneVoice(KEVIN16_VOICE, Gender.MALE, Age.YOUNGER_ADULT,
				"default 16-bit diphone voice", Locale.US, "general", "cmu", lexicon, cmu_us_kal16_url);

		Voice[] voices = { kevin, kevin16 };
		return voices;

	}

}
