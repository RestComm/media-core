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
