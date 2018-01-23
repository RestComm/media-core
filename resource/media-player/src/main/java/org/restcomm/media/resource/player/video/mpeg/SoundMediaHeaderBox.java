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
 * <b>8.4.5.3 Sound Media Header Box</b>
 * <p>
 * The sound media header contains general presentation information, independent of the coding, for audio media. This
 * header is used for all tracks containing audio.
 * </p>
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class SoundMediaHeaderBox extends FullBox {

	// File Type = smhd
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_s, AsciiTable.ALPHA_m, AsciiTable.ALPHA_h, AsciiTable.ALPHA_d };
	static String TYPE_S = "smhd";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private double balance;

	public SoundMediaHeaderBox(long size) {
		super(size, TYPE_S);
	}

	/**
	 * Gets the balance of the audio track in stereo space.
	 * 
	 * @return number that places mono audio tracks in a stereo space
	 */
	public double getBalance() {
		return balance;
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		super.load(fin);
		balance = fin.readByte() + fin.readByte() / 10;

		fin.readByte();
		fin.readByte();

		return (int) getSize();
	}
}
