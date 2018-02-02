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
 * <b>8.1.1.1 Definition</b>
 * <ul>
 * <li>Box Type: �?mdat’</li>
 * <li>Container: File</li>
 * <li>Mandatory: No</li>
 * <li>Quantity: Any number</li>
 * </ul>
 * <p>
 * This box contains the media data. In video tracks, this box would contain video frames. A presentation may contain
 * zero or more Media Data Boxes. The actual media data follows the type field; its structure is described by the
 * metadata (see particularly the {@link SampleTableBox}, subclause 8.5, and the item location box, subclause 8.11.3).
 * </p>
 * <p>
 * In large presentations, it may be desirable to have more data in this box than a 32-bit size would permit. In this
 * case, the large variant of the size field, above in subclause 6.2, is used.
 * </p>
 * <p>
 * There may be any number of these boxes in the file (including zero, if all the media data is in other files). The
 * metadata refers to media data by its absolute offset within the file (see subclause 8.7.5, the Chunk Offset Box); so
 * Media Data Box headers and free space may easily be skipped, and files without any box structure may also be
 * referenced and used.
 * </p>
 * 
 * @author amit bhayani
 * 
 */
public class MediaDataBox extends Box {

	// File Type = mdat
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_m, AsciiTable.ALPHA_d, AsciiTable.ALPHA_a, AsciiTable.ALPHA_t };
	static String TYPE_S = "mdat";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private byte[] data;

	public MediaDataBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		int count = 8;
		int length = (int) this.getSize() - count;
		// data = new byte[length];

		// We are just skiping. The RandomAccessFile should be used for reading byte[]
		count += fin.skip(length);
		// count += fin.read(data, 0, length);
		return count;
	}

}
