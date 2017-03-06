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
 * <b>8.6.1.3.1 Definition</b>
 * <ul>
 * <li>Box Type: �?ctts’</li>
 * <li>Container: {@link SampleTableBox} (�?stbl’)</li>
 * <li>Mandatory: No</li>
 * <li>Quantity: Zero or one</li>
 * </ul>
 * <p>
 * This box provides the offset between decoding time and composition time. Since decoding time must be less than the
 * composition time, the offsets are expressed as unsigned numbers such that CT(n) = DT(n) + CTTS(n) where CTTS(n) is
 * the (uncompressed) table entry for sample n.
 * </p>
 * <p>
 * The composition time to sample table is optional and must only be present if DT and CT differ for any samples. Hint
 * tracks do not use this box.
 * </p>
 * 
 * @author amit bhayani
 * 
 */
public class CompositionOffsetBox extends FullBox {

	// File Type = ctts
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_c, AsciiTable.ALPHA_t, AsciiTable.ALPHA_t, AsciiTable.ALPHA_s };
	static String TYPE_S = "ctts";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private long[] sampleCount = null;
	private long[] sampleOffset = null;

	public CompositionOffsetBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		super.load(fin);

		int count = 12;
		long entryCount = readU32(fin);
		count += 4;
		sampleCount = new long[(int)entryCount];
		sampleOffset = new long[(int)entryCount];

		for (int i = 0; i < entryCount; i++) {
			sampleCount[i] = readU32(fin);
			sampleOffset[i] = readU32(fin);
			count += 8;
		}
		return (int) this.getSize();
	}

	public long[] getSampleCount() {
		return sampleCount;
	}

	public long[] getSampleOffset() {
		return sampleOffset;
	}

}
