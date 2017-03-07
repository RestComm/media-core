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
 * <b> 8.10.2.1 Definition</b>
 * <ul>
 * <li>Box Type: �?cprt’</li>
 * <li>Container: User data box (�?udta’)</li>
 * <li>Mandatory: No</li>
 * <li>Quantity: Zero or more</li>
 * </ul>
 * <p>
 * The Copyright box contains a copyright declaration which applies to the entire presentation, when contained within
 * the Movie Box, or, when contained in a track, to that entire track. There may be multiple copyright boxes using
 * different language codes.
 * </p>
 * 
 * @author amit bhayani
 * 
 */
public class CopyrightBox extends FullBox {

	// File Type = cprt
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_c, AsciiTable.ALPHA_p, AsciiTable.ALPHA_r, AsciiTable.ALPHA_t };
	static String TYPE_S = "cprt";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private String language;
	private String copyright;

	public CopyrightBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		super.load(fin);

		return (int) this.getSize();
	}

}
