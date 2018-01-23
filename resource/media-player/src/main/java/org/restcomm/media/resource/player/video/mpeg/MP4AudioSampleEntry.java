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
 * Look 3Gpp TS 26.244 section 6.4
 * 
 * @author amit bhayani
 * 
 */
public class MP4AudioSampleEntry extends AudioSampleEntry {

	// File Type = mp4a
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_m, AsciiTable.ALPHA_p, AsciiTable.DIGIT_FOUR, AsciiTable.ALPHA_a };
	static String TYPE_S = "mp4a";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private ESDBox esdBox = null;

	public MP4AudioSampleEntry(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		int count = super.load(fin);
		if (count < getSize()) {
			long len = readU32(fin);
			byte[] type = read(fin);

			if (comparebytes(type, ESDBox.TYPE)) {
				esdBox = new ESDBox(len);
				count += esdBox.load(fin);
			} else {
				throw new IOException("Unknown box=" + new String(type) + "parent = AudioSampleEntry");
			}
		}
		return count;
	}

	public ESDBox getEsdBox() {
		return esdBox;
	}

}
