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
 * @author amit bhayani
 * 
 */
public class RTPMovieHintInformation extends Box {

	// File Type = rtp
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_r, AsciiTable.ALPHA_t, AsciiTable.ALPHA_p, AsciiTable.SPACE };
	static String TYPE_S = "rtp ";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private String sdpText;
	private String descriptionFormat;

	public RTPMovieHintInformation(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		descriptionFormat = new String(this.read(fin));
		
		int lenOfSdp = (int) (this.getSize() - 12);
		byte[] sdpbytArr = new byte[lenOfSdp];
		fin.read(sdpbytArr, 0, (lenOfSdp));
		sdpText = new String(sdpbytArr, "UTF-8");	

		return (int) this.getSize();
	}

	public String getSdpText() {
		return sdpText;
	}

	public String getDescriptionFormat() {
		return descriptionFormat;
	}

}
