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

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceDirectory;

/**
 * Factory to get new instance of Voice that this Factory supports
 * 
 * @author amit bhayani
 * 
 */
public abstract class VoiceFactory extends VoiceDirectory {

	/**
	 * 
	 * @param voiceName
	 *            The Voice corresponding to this name will be created and returned if VoiceFactory supports this name,
	 *            null other wise.
	 * @return
	 */
	public abstract Voice getVoice(String voiceName);

}
