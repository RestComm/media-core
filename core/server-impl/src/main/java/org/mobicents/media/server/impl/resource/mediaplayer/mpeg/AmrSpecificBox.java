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
 * The AMRSpecificBox fields for AMR and AMR-WB shall be as defined in table 6.6. The AMRSpecificBox for the
 * AMRSampleEntry Box shall always be included if the 3GP file contains AMR or AMR-WB media.
 * 
 * <p>
 * Table 6.6: The AMRSpecificBox fields for AMRSampleEntry
 * <p>
 * Look at 6.7 section of 3GPP TS 26.244 Transparent end-to-end packet switched streaming service (PSS); 3GPP file
 * format (3GP)
 * <p>
 * http://www.3gpp.org/ftp/Specs/html-info/26244.htm
 * 
 * @author amit bhayani
 * 
 */
public class AmrSpecificBox extends Box {

	private String vendor;
	private int decoderVersion;
	private int modeSet;
	private int modeChangePeriod;
	private int framesPerSample;

	public AmrSpecificBox(long size, String type) {
		super(size, type);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		//Here size should 8+9 = 17
		
		int count = 8;
		
		vendor = new String(read(fin));
		decoderVersion = fin.readByte();
		modeSet = super.read16(fin);
		modeChangePeriod = fin.readByte();
		framesPerSample = fin.readByte();

		return count + 9;
	}

	public String getVendor() {
		return vendor;
	}

	public int getDecoderVersion() {
		return decoderVersion;
	}

	public int getModeSet() {
		return modeSet;
	}

	public int getModeChangePeriod() {
		return modeChangePeriod;
	}

	public int getFramesPerSample() {
		return framesPerSample;
	}

}
