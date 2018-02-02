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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.speech.freetts.Voice;

/**
 * Class to store voices. It allocates them at start and caches - just like pool.
 * This is required to avoid cost of voice.allocate(); method.
 * @author baranowb
 *
 */
public class VoicesCache {

	private HashMap<String, LinkedList<Voice>> voicePool = new HashMap<String, LinkedList<Voice>>();
	private VoiceManager voiceManager;
	/**
	 * 
	 */
	public VoicesCache() {
		this.voiceManager = VoiceManager.getInstance();
		//init some default pool?
	}

	public Voice allocateVoice(String voiceName)
	{
		LinkedList<Voice> voicesList = this.voicePool.get(voiceName);
		if(voicesList == null)
		{
			voicesList = new LinkedList<Voice>();
			this.voicePool.put(voiceName, voicesList);
		}
		
		if(voicesList.size() == 0)
		{
			Voice v = voiceManager.getVoice(voiceName);
			v.allocate();
			return v;
		}else
		{
			return voicesList.removeFirst();
		}
		
		
		
	}
	
	public void releaseVoice(Voice v)
	{
		v.setAudioPlayer(null);
		this.voicePool.get(v.getName().toLowerCase()).add(v);
	}
	
	public void clear()
	{
		Iterator<Entry<String, LinkedList<Voice>>> it =this.voicePool.entrySet().iterator();
		while(it.hasNext())
		{
			Entry<String, LinkedList<Voice>> entry = it.next();
			it.remove();
			for(Voice v: entry.getValue())
			{
				v.deallocate();
			}
			entry.getValue().clear();
		}
		
		
	}

	public void init(Map<String, Integer> voices) {
		for(String voiceName: voices.keySet())
		{
			int voiceCount = voices.get(voiceName);
			LinkedList<Voice> list = new LinkedList<Voice>();
			this.voicePool.put(voiceName, list);
			while(voiceCount>0)
			{
				
				Voice v = voiceManager.getVoice(voiceName);
				v.allocate();
				list.add(v);
				voiceCount--;
			}
		}
		
		
	}

}
