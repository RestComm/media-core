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

package org.mobicents.media.server.ctrl.rtsp.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.server.ctrl.rtsp.RequestParser;
import org.mobicents.media.server.spi.MediaType;

/**
 * 
 * @author amit bhayani
 * 
 */
public class RequestParserTestCase {

	private Set<String> endpoints = null;

	public RequestParserTestCase() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {

	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		endpoints = new HashSet<String>();
		endpoints.add("/mobicents/media/aap/");
		endpoints.add("/mobicents/media/IVR/");
		endpoints.add("/mobicents/media/cnf/");

	}

	@After
	public void tearDown() {
	}

	@Test
	public void testURIWithoutMediaType() throws Exception {
		String URI = "/mobicents/media/aap/$/sample_100kbit.mp4";
		RequestParser rtpReqParser = new RequestParser(URI, endpoints);

		assertEquals("/mobicents/media/aap/$", rtpReqParser.getEndpointName());
		assertNull(rtpReqParser.getMediaType());
		assertEquals("sample_100kbit.mp4", rtpReqParser.getMediaFile());
	}

	@Test
	public void testURIWithMediaType() throws Exception {
		String URI = "/mobicents/media/aap/$/sample_100kbit.mp4/audio";
		RequestParser rtpReqParser = new RequestParser(URI, endpoints);

		assertEquals("/mobicents/media/aap/$", rtpReqParser.getEndpointName());
		assertEquals(MediaType.AUDIO, rtpReqParser.getMediaType());
		assertEquals("sample_100kbit.mp4", rtpReqParser.getMediaFile());
	}

	@Test
	public void testURIWithoutMediaFile() throws Exception {
		String URI = "/mobicents/media/cnf/1";
		RequestParser rtpReqParser = new RequestParser(URI, endpoints);

		assertEquals("/mobicents/media/cnf/1", rtpReqParser.getEndpointName());
		assertNull(rtpReqParser.getMediaType());
		assertNull(rtpReqParser.getMediaFile());
	}

	@Test
	public void testURIWithoutMediaType1() throws Exception {
		String URI = "/mobicents/media/aap/$/myapp/app2/sample_100kbit.mp4";
		RequestParser rtpReqParser = new RequestParser(URI, endpoints);

		assertEquals("/mobicents/media/aap/$", rtpReqParser.getEndpointName());
		assertNull(rtpReqParser.getMediaType());
		assertEquals("myapp/app2/sample_100kbit.mp4", rtpReqParser.getMediaFile());
	}
}
