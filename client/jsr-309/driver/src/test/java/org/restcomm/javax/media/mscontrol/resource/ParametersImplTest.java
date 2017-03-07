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

package org.restcomm.javax.media.mscontrol.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mediagroup.CodecConstants;
import javax.media.mscontrol.mediagroup.Recorder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.javax.media.mscontrol.ParametersImpl;

/**
 * 
 * @author amit bhayani
 *
 */
public class ParametersImplTest {
	Parameters parameters = null;
	Object obj = null;

	@Before
	public void setUp() {
		parameters = new ParametersImpl();
		parameters.put(Recorder.AUDIO_CODEC, CodecConstants.MULAW_PCM_64K);
	}

	@After
	public void tearDown() {
		parameters.clear();
		obj = null;
	}

	@Test
	public void testParameters() {
		assertTrue(parameters.containsKey(Recorder.AUDIO_CODEC));
		assertTrue(parameters.containsValue(CodecConstants.MULAW_PCM_64K));
		
		Set<java.util.Map.Entry<Parameter, Object>> set = parameters.entrySet();
		assertNotNull(set);
		assertEquals(1, set.size());
		
		Object objTemp = parameters.get(Recorder.AUDIO_CODEC);
		assertEquals(CodecConstants.MULAW_PCM_64K, objTemp);
		
		assertEquals(false, parameters.isEmpty());
		
		assertNotNull(parameters.remove(Recorder.AUDIO_CODEC));

	}
}
