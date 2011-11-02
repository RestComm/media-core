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
