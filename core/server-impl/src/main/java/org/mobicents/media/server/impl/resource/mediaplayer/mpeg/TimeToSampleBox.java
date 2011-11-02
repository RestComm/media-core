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
package org.mobicents.media.server.impl.resource.mediaplayer.mpeg;

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
