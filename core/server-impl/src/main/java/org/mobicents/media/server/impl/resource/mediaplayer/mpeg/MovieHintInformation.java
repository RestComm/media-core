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

public class MovieHintInformation extends Box {

	// File Type = hnti
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_h, AsciiTable.ALPHA_n, AsciiTable.ALPHA_t, AsciiTable.ALPHA_i };
	static String TYPE_S = "hnti";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private RTPMovieHintInformation rtpMovieHintInformation;

	public MovieHintInformation(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		long len = readU32(fin);

		byte[] type = read(fin);

		if (comparebytes(type, RTPMovieHintInformation.TYPE)) {
			rtpMovieHintInformation = new RTPMovieHintInformation(len);
			rtpMovieHintInformation.load(fin);
		}

		return (int) this.getSize();
	}

	public RTPMovieHintInformation getRtpMovieHintInformation() {
		return rtpMovieHintInformation;
	}

}
