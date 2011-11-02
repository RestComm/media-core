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

package org.mobicents.media.server.impl.resource.mediaplayer.mpeg;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.format.AudioFormat;

import static org.junit.Assert.*;

/**
 * 
 * @author amit bhayani
 * 
 */
public class MpegPresentationTest {

	MpegPresentation mpegPresentation = null;

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {

	}

	@After
	public void tearDown() {

	}

	/**
	 * Test of getMediaTypes method, of class MediaPlayerImpl.
	 */
	public void testAudio(URL url, AudioFormat amrFormat) {
		try {

			// System.out.println("url = " + url);

			mpegPresentation = new MpegPresentation(url);

			assertNotNull(mpegPresentation);
			assertNotNull(mpegPresentation.getAudioForamt());

			assertEquals(amrFormat, mpegPresentation.getAudioForamt());
			// System.out.println("AudioFormat = " + mpegPresentation.getAudioForamt());
		} catch (IOException e) {
			fail("Caught exception");
		}
	}

	@Test
	public void testsample_50kbit_3gp() {

		AudioFormat amrFormat = new AudioFormat(AudioFormat.AMR, 8000.0, AudioFormat.NOT_SPECIFIED, 1);
		URL url = null;
		// try {
		// url = new
		// URL("file:/home/abhayani/workarea/mobicents/svn/trunk/servers/media/core/server-impl/src/test/resources/org/mobicents/media/server/impl/resource/video/sample_50kbit.3gp");
		url = MpegPresentationTest.class.getClassLoader().getResource(
				"org/mobicents/media/server/impl/resource/video/sample_50kbit.3gp");
		testAudio(url, amrFormat);
		// } catch (MalformedURLException e) {
		// e.printStackTrace();
		// }

	}

	@Test
	public void testsample_100kbit_mov() {

		AudioFormat qdm = new AudioFormat("X-QDM", 22050.0, AudioFormat.NOT_SPECIFIED, 2);
		// URL url =
		// MpegPresentationTest.class.getClassLoader().getResource("org/mobicents/media/server/impl/resource/video/sample_50kbit.3gp");
		URL url = null;
		// try {
		// url = new
		// URL("file:/home/abhayani/workarea/mobicents/svn/trunk/servers/media/core/server-impl/src/test/resources/org/mobicents/media/server/impl/resource/video/sample_100kbit.mov");
		url = MpegPresentationTest.class.getClassLoader().getResource(
				"org/mobicents/media/server/impl/resource/video/sample_100kbit.mov");
		testAudio(url, qdm);
		// } catch (MalformedURLException e) {
		// e.printStackTrace();
		// }

	}

	@Test
	public void testsample_100kbit_mp4() {

		AudioFormat mpeg4Gen = new AudioFormat("mpeg4-generic", 8000.0, AudioFormat.NOT_SPECIFIED, 2);
		// URL url =
		// MpegPresentationTest.class.getClassLoader().getResource("org/mobicents/media/server/impl/resource/video/sample_50kbit.3gp");
		URL url = null;
		// try {
		// url = new URL(
		// "file:/home/abhayani/workarea/mobicents/svn/trunk/servers/media/core/server-impl/src/test/resources/org/mobicents/media/server/impl/resource/video/sample_100kbit.mp4");
		url = MpegPresentationTest.class.getClassLoader().getResource(
				"org/mobicents/media/server/impl/resource/video/sample_100kbit.mp4");
		testAudio(url, mpeg4Gen);
		// } catch (MalformedURLException e) {
		// e.printStackTrace();
		// }

	}

}
