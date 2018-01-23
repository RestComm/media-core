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
 * <b>8.1.2.1 Definition</b>
 * <ul>
 * <li>Box Types: �?free’, �?skip’</li>
 * <li>Container: File or other box</li>
 * <li>Mandatory: No</li>
 * <li>Quantity: Any number</li>
 * </ul>
 * <p>
 * The contents of a free-space box are irrelevant and may be ignored, or the object deleted, without affecting the
 * presentation. (Care should be exercised when deleting the object, as this may invalidate the offsets used in the
 * sample table, unless this object is after all the media data).
 * </p>
 * 
 * @author amit bhayani
 * 
 */
public class FreeSpaceBox extends Box {

	// File Type = free or skip
	static byte[] TYPE_FREE = new byte[] { AsciiTable.ALPHA_f, AsciiTable.ALPHA_r, AsciiTable.ALPHA_e,
			AsciiTable.ALPHA_e };
	static byte[] TYPE_SKIP = new byte[] { AsciiTable.ALPHA_s, AsciiTable.ALPHA_k, AsciiTable.ALPHA_i,
			AsciiTable.ALPHA_p };
	static String TYPE_FREE_S = "free";
	static String TYPE_SKIP_S = "skip";
	static {
		bytetoTypeMap.put(TYPE_FREE, TYPE_FREE_S);
		bytetoTypeMap.put(TYPE_SKIP, TYPE_SKIP_S);
	}

	private byte[] data;

	public FreeSpaceBox(long size, String type) {
		super(size, type);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		int count = 8;
		if (count < this.getSize()) {
			int length = (int) this.getSize() - count;
			data = new byte[length];
			fin.read(data, 0, length);

			count += length;
		}

		return (int)this.getSize();
	}

}
