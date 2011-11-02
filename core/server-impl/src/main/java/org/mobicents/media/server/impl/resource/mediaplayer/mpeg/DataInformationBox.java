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
 * <b>8.7.1.1 Definition</b>
 * <ul>
 * <li>Box Type: �?dinf’ </li>
 * <li>Container: {@link MediaInformationBox} (�?minf’) or Meta Box (�?meta’)</li>
 * <li> Mandatory: Yes (required within �?minf’ box) and No (optional within �?meta’ box)</li>
 * <li> Quantity: Exactly one</li>
 * </ul>
 * The data information box contains objects that declare the location of the media information in a track.
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class DataInformationBox extends Box {

	// File Type = dinf
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_d, AsciiTable.ALPHA_i, AsciiTable.ALPHA_n, AsciiTable.ALPHA_f };
	static String TYPE_S = "dinf";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private DataReferenceBox dataReferenceBox;

	public DataInformationBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		long len = readU32(fin);
		byte[] type = read(fin);

		if (comparebytes(type, DataReferenceBox.TYPE)) {
			dataReferenceBox = new DataReferenceBox(len);
			dataReferenceBox.load(fin);
		} else {
			throw new IOException();
		}

		return (int) getSize();
	}

	public DataReferenceBox getDataReferenceBox() {
		return dataReferenceBox;
	}

}
