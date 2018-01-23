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
