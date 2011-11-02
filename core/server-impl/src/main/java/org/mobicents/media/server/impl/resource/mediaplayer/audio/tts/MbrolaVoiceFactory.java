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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;

import com.sun.speech.freetts.Age;
import com.sun.speech.freetts.Gender;
import com.sun.speech.freetts.ValidationException;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.en.us.CMULexicon;

import de.dfki.lt.freetts.en.us.MbrolaVoice;
import de.dfki.lt.freetts.en.us.MbrolaVoiceValidator;

/**
 * Factory for Mbrola Voices
 * @author amit bhayani
 *
 */
public class MbrolaVoiceFactory extends VoiceFactory {

	private static final Logger logger = Logger.getLogger(MbrolaVoiceFactory.class);

	public static final String MBROLA_US1_VOICE = "mbrola_us1";
	public static final String MBROLA_US2_VOICE = "mbrola_us2";
	public static final String MBROLA_US3_VOICE = "mbrola_us3";

	@Override
	public Voice getVoice(String voiceName) {
		Voice voice = null;
		if (MBROLA_US1_VOICE.compareTo(voiceName) == 0) {
			CMULexicon lexicon = new CMULexicon("cmulex");
			voice = new MbrolaVoice("us1", "us1", 150f, 180F, 22F, MBROLA_US1_VOICE, Gender.FEMALE, Age.YOUNGER_ADULT,
					"MBROLA Voice us1", Locale.US, "general", "mbrola", lexicon);
		} else if (MBROLA_US2_VOICE.compareTo(voiceName) == 0) {
			CMULexicon lexicon = new CMULexicon("cmulex");
			voice = new MbrolaVoice("us2", "us2", 150f, 115F, 12F, MBROLA_US2_VOICE, Gender.MALE, Age.YOUNGER_ADULT,
					"MBROLA Voice us2", Locale.US, "general", "mbrola", lexicon);
		} else if (MBROLA_US3_VOICE.compareTo(voiceName) == 0) {
			CMULexicon lexicon = new CMULexicon("cmulex");
			voice = new MbrolaVoice("us3", "us3", 150f, 125F, 12F, MBROLA_US3_VOICE, Gender.MALE, Age.YOUNGER_ADULT,
					"MBROLA Voice us3", Locale.US, "general", "mbrola", lexicon);
		}
		return voice;
	}

	@Override
	public Voice[] getVoices() {
		List<Voice> voicesList = new ArrayList<Voice>();
		CMULexicon lexicon = new CMULexicon("cmulex");

		// Set-up MBROLA Voices.
		Voice mbrola1 = new MbrolaVoice("us1", "us1", 150f, 180F, 22F, MBROLA_US1_VOICE, Gender.FEMALE,
				Age.YOUNGER_ADULT, "MBROLA Voice us1", Locale.US, "general", "mbrola", lexicon);
		if (this.validate(mbrola1)) {
			voicesList.add(mbrola1);
		}

		Voice mbrola2 = new MbrolaVoice("us2", "us2", 150f, 115F, 12F, MBROLA_US2_VOICE, Gender.MALE,
				Age.YOUNGER_ADULT, "MBROLA Voice us2", Locale.US, "general", "mbrola", lexicon);
		if (this.validate(mbrola2)) {
			voicesList.add(mbrola2);
		}

		Voice mbrola3 = new MbrolaVoice("us3", "us3", 150f, 125F, 12F, MBROLA_US3_VOICE, Gender.MALE,
				Age.YOUNGER_ADULT, "MBROLA Voice us3", Locale.US, "general", "mbrola", lexicon);
		if (this.validate(mbrola3)) {
			voicesList.add(mbrola3);
		}

		Voice[] voicesArr = new Voice[voicesList.size()];
		return voicesList.toArray(voicesArr);
	}

	private boolean validate(Voice voice) {
		MbrolaVoiceValidator validator = new MbrolaVoiceValidator((MbrolaVoice) voice);
		try {
			validator.validate();
			return true;
		} catch (ValidationException ve) {
			logger.warn("MBROLA Voice " + voice.getName() + " set-up failed. \n" + ve.getMessage());
		}
		return false;
	}

}
