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
 * 
 * @author amit bhayani
 *
 */
public class RTPTrackSdpHintInformation extends Box {

	// File Type = sdp
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_s, AsciiTable.ALPHA_d, AsciiTable.ALPHA_p, AsciiTable.SPACE };
	static String TYPE_S = "sdp ";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private String sdpText;

	public RTPTrackSdpHintInformation(long size) {
		super(size, TYPE_S);

	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		int lenOfSdp = (int) (this.getSize() - 8);
		byte[] sdpbytArr = new byte[lenOfSdp];
		fin.read(sdpbytArr, 0, (lenOfSdp));
		sdpText = new String(sdpbytArr, "UTF-8");

		return (int) this.getSize();
	}

	public String getSdpText() {
		return sdpText;
	}

}
