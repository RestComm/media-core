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
 * <b>4.3.1 Definition<b>
 * <ul>
 * <li>Box Type: �?ftyp’</li>
 * <li>Container: File</li>
 * <li>Mandatory: Yes</li>
 * <li>Quantity: Exactly one</li>
 * </ul>
 * 
 * @author kulikov
 * @author amit abhayani
 */
public class FileTypeBox extends Box {

	// File Type = ftyp
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_f, AsciiTable.ALPHA_t, AsciiTable.ALPHA_y, AsciiTable.ALPHA_p };
	static String TYPE_S = "ftyp";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private String majorBrand;
	private long minorVersion;
	private String[] compatibleBrands;

	public FileTypeBox(long size) {
		super(size, TYPE_S);
	}

	public String getMajorBrand() {
		return this.majorBrand;
	}

	public long getMinorVersion() {
		return this.minorVersion;
	}

	public String[] getCompatibleBrands() {
		return this.compatibleBrands;
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		this.majorBrand = new String(read(fin));
		this.minorVersion = this.readU32(fin);

		long remainder = getSize() - 16;
		int count = (int) (remainder / 4);

		compatibleBrands = new String[count];
		for (int i = 0; i < count; i++) {
			compatibleBrands[i] = new String(read(fin));
		}
		return (int) getSize();
	}

	@Override
	public String toString() {
		StringBuffer b = new StringBuffer("FileTypeBox(ftyp)[majorBrand=").append(this.majorBrand).append(
				",minorVersion=").append(this.minorVersion).append(",compatibleBrands[");
		for (String s : this.compatibleBrands) {
			b.append(s);
			b.append(",");
		}
		b.append("]]");
		return b.toString();
	}
}
