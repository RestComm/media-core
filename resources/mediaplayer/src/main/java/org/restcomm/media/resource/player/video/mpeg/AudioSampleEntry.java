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

package org.restcomm.media.resource.player.video.mpeg;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * 
 * @author amit bhayani
 * 
 */
public abstract class AudioSampleEntry extends SampleEntry {

	// ChannelCount is either 1 (mono) or 2 (stereo)
	private int channelCount;

	// SampleSize is in bits, and takes the default value of 16
	private int sampleSize = 16;

	// SampleRate is the sampling rate expressed as a 16.16 fixed-point number (hi.lo)
	private double sampleRate;

	public AudioSampleEntry(long size, String type) {
		super(size, type);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		super.load(fin);

		// int[2] reserved
		fin.skip(8);

		channelCount = read16(fin);
		sampleSize = read16(fin);

		fin.skip(2);

		// reserved
		fin.skip(2);

		sampleRate = readFixedPoint1616(fin);//(a >> 16) + (a & 0xffff) / 10;

		int count = 28 + 8;

		return count;
	}

	public int getChannelCount() {
		return channelCount;
	}

	public int getSampleSize() {
		return sampleSize;
	}

	public double getSampleRate() {
		return sampleRate;
	}

}
