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

/**
 * <b>8.4.5.5 Null Media Header Box</b>
 * <p>
 * Streams other than visual and audio (e.g., timed metadata streams) may use a null Media Header Box, as defined here.
 * </p>
 * 
 * @author kulikov
 */
public class NullMediaHeaderBox extends FullBox {

	// File Type = moov
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_n, AsciiTable.ALPHA_m, AsciiTable.ALPHA_h, AsciiTable.ALPHA_d };
	static String TYPE_S = "nmhd";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	public NullMediaHeaderBox(long size) {
		super(size, TYPE_S);
	}

}
