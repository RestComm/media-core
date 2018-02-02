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
 * <b>8.5.2.1 Definition</b>
 * <ul>
 * <li>Box Types: �?stsd’</li>
 * <li>Container: {@link SampleTableBox} (�?stbl’)</li>
 * <li>Mandatory: Yes</li>
 * <li>Quantity: Exactly one</li>
 * </ul>
 * <p>
 * The sample description table gives detailed information about the coding type used, and any initialization
 * information needed for that coding.
 * </p>
 * <p>
 * The information stored in the sample description box after the entry-count is both track-type specific as documented
 * here, and can also have variants within a track type (e.g. different codings may use different specific information
 * after some common fields, even within a video track). For video tracks, a VisualSampleEntry is used, for audio
 * tracks, an AudioSampleEntry and for metadata tracks, a MetaDataSampleEntry. Hint tracks use an entry format specific
 * to their protocol, with an appropriate name.
 * </p>
 * <p>
 * For hint tracks, the sample description contains appropriate declarative data for the streaming protocol being used,
 * and the format of the hint track. The definition of the sample description is specific to the protocol.
 * </p>
 * 
 * 
 * @author amit bhayani
 * 
 */
public class SampleDescriptionBox extends FullBox {

	// File Type = stsd
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_s, AsciiTable.ALPHA_t, AsciiTable.ALPHA_s, AsciiTable.ALPHA_d };
	static String TYPE_S = "stsd";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private SampleEntry[] sampleEntries = null;

	public SampleDescriptionBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		super.load(fin);
		long entryCount = readU32(fin);
		sampleEntries = new SampleEntry[(int)entryCount];
		for (int i = 0; i < entryCount; i++) {
			long len1 = readU32(fin);
			byte[] type = read(fin);
			SampleEntry sampleEntry = null;
			if (comparebytes(type, AMRSampleEntry.TYPE)) {
				sampleEntry = new AMRSampleEntry(len1);
				sampleEntry.load(fin);

			} else if (comparebytes(type, AMRWidebandSampleEntry.TYPE)) {
				sampleEntry = new AMRWidebandSampleEntry(len1);
				sampleEntry.load(fin);

			} else if (comparebytes(type, MP4AudioSampleEntry.TYPE)) {
				sampleEntry = new MP4AudioSampleEntry(len1);
				sampleEntry.load(fin);

			} else if (comparebytes(type, VisualSampleEntry.TYPE)) {
				sampleEntry = new VisualSampleEntry(len1);
				sampleEntry.load(fin);
			} else if (comparebytes(type, RtpHintSampleEntry.TYPE)) {
				sampleEntry = new RtpHintSampleEntry(len1);
				sampleEntry.load(fin);
			} else {
				//TODO : Do we care to keep reference for UndefinedBox? 
				UndefinedBox undefinedBox = new UndefinedBox(len1, new String(type));
				undefinedBox.load(fin);
			}

			sampleEntries[i] = sampleEntry;
		}

		return (int) getSize();
	}

	public SampleEntry[] getSampleEntries() {
		return sampleEntries;
	}

}
