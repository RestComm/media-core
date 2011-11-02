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

import java.net.URL;
import java.util.Locale;

import com.sun.speech.freetts.Age;
import com.sun.speech.freetts.Gender;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.en.us.CMUClusterUnitVoice;
import com.sun.speech.freetts.en.us.CMULexicon;
import com.sun.speech.freetts.en.us.cmu_time_awb.AlanVoiceDirectory;

/**
 * Factory for Alan Voice.
 * 
 * @author amit bhayani
 *
 */
public class AlanVoiceFactory extends VoiceFactory {

	public static final String ALAN_VOICE = "alan";

	public final URL cmu_time_awb_url = AlanVoiceDirectory.class.getResource("cmu_time_awb.bin");

	@Override
	public Voice getVoice(String voiceName) {
		Voice voice = null;
		if (ALAN_VOICE.compareTo(voiceName) == 0) {
			CMULexicon lexicon = new CMULexicon("cmutimelex");
			voice = new CMUClusterUnitVoice(ALAN_VOICE, Gender.MALE, Age.YOUNGER_ADULT,
					"default time-domain cluster unit voice", Locale.US, "time", "cmu", lexicon, cmu_time_awb_url);
		}
		return voice;
	}

	@Override
	public Voice[] getVoices() {
		CMULexicon lexicon = new CMULexicon("cmutimelex");
		Voice alan = new CMUClusterUnitVoice(ALAN_VOICE, Gender.MALE, Age.YOUNGER_ADULT,
				"default time-domain cluster unit voice", Locale.US, "time", "cmu", lexicon, cmu_time_awb_url);
		Voice[] voices = { alan };
		return voices;
	}

}
