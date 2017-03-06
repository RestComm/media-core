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

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 
 * @author amit bhayani
 * 
 */
public class RTPSampleConstructor extends RTPConstructor {

	public static final int TYPE = 2;

	private byte trackRefIndex;
	private int length;
	private long sampleNumber;
	private long sampleOffSet;
	private int bytesPerBlock = 1;
	private int samplesPerBlock = 1;

	public RTPSampleConstructor() {
		super(TYPE);
	}

	@Override
	public int load(RandomAccessFile raAccFile) throws IOException {
		trackRefIndex = raAccFile.readByte();
		length = (raAccFile.read() << 8) | raAccFile.read();
		sampleNumber = ((long) (raAccFile.read() << 24 | raAccFile.read() << 16 | raAccFile.read() << 8 | raAccFile
				.read())) & 0xFFFFFFFFL;

		sampleOffSet = ((long) (raAccFile.read() << 24 | raAccFile.read() << 16 | raAccFile.read() << 8 | raAccFile
				.read())) & 0xFFFFFFFFL;

		bytesPerBlock = (raAccFile.read() << 8) | raAccFile.read();

		samplesPerBlock = (raAccFile.read() << 8) | raAccFile.read();

		return 16;
	}

	public int getTrackRefIndex() {
		return trackRefIndex;
	}

	public int getLength() {
		return length;
	}

	public long getSampleNumber() {
		return sampleNumber;
	}

	public long getSampleOffSet() {
		return sampleOffSet;
	}

	public int getBytesPerBlock() {
		return bytesPerBlock;
	}

	public int getSamplesPerBlock() {
		return samplesPerBlock;
	}

}
