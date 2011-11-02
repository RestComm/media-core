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
