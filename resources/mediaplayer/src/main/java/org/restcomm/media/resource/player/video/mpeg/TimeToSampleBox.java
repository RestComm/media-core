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
public class TimeToSampleBox extends FullBox {

	// File Type = stsd
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_s, AsciiTable.ALPHA_t, AsciiTable.ALPHA_t, AsciiTable.ALPHA_s };
	static String TYPE_S = "stts";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private long entryCount;
	private long[] sampleCount;
	private long[] sampleDelta;

	public TimeToSampleBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		super.load(fin);

		entryCount = readU32(fin);

		sampleCount = new long[(int)entryCount];
		sampleDelta = new long[(int)entryCount];
		for (int i = 0; i < entryCount; i++) {
			sampleCount[i] = readU32(fin);
			sampleDelta[i] = readU32(fin);
		}

		return (int) this.getSize();
	}

	public long[] getSampleCount() {
		return sampleCount;
	}

	public long[] getSampleDelta() {
		return sampleDelta;
	}

	public long getEntryCount() {
		return entryCount;
	}

}
