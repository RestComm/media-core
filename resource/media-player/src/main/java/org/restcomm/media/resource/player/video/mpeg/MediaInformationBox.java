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
 * <b>8.4.4.1 Definition Box</b>
 * <ul>
 * <li>Type: �?minf’</li>
 * <li>Container: {@link MediaBox} (�?mdia’)</li>
 * <li>Mandatory: Yes </li>
 * <li>Quantity: Exactly one </li>
 * </ul>
 * This box contains all the objects that declare characteristic information of the media in the track.
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class MediaInformationBox extends Box {

	// File Type = minf
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_m, AsciiTable.ALPHA_i, AsciiTable.ALPHA_n, AsciiTable.ALPHA_f };
	static String TYPE_S = "minf";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private SoundMediaHeaderBox soundMediaHeaderBox;
	private VideoMediaHeaderBox videoMediaHeaderBox;
	private HintMediaHeaderBox hintMediaHeaderBox;
	private NullMediaHeaderBox nullMediaHeaderBox;
	private DataInformationBox dataInformationBox;
	private SampleTableBox sampleTableBox;
	private HandlerReferenceBox handlerReferenceBox;

	public MediaInformationBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		int count = 8;
		while (count < getSize()) {
			long len = readU32(fin);
			byte[] type = read(fin);
			if (comparebytes(type, VideoMediaHeaderBox.TYPE)) {
				videoMediaHeaderBox = new VideoMediaHeaderBox(len);
				count += videoMediaHeaderBox.load(fin);
			} else if (comparebytes(type, SoundMediaHeaderBox.TYPE)) {
				soundMediaHeaderBox = new SoundMediaHeaderBox(len);
				count += soundMediaHeaderBox.load(fin);
			} else if (comparebytes(type, HintMediaHeaderBox.TYPE)) {
				hintMediaHeaderBox = new HintMediaHeaderBox(len);
				count += hintMediaHeaderBox.load(fin);
			} else if (comparebytes(type, NullMediaHeaderBox.TYPE)) {
				nullMediaHeaderBox = new NullMediaHeaderBox(len);
				count += nullMediaHeaderBox.load(fin);
			} else if (comparebytes(type, DataInformationBox.TYPE)) {
				dataInformationBox = new DataInformationBox(len);
				count += dataInformationBox.load(fin);
			} else if (comparebytes(type, SampleTableBox.TYPE)) {
				sampleTableBox = new SampleTableBox(len);
				count += sampleTableBox.load(fin);
			} else if (comparebytes(type, HandlerReferenceBox.TYPE)) {
				handlerReferenceBox = new HandlerReferenceBox(len);
				count += handlerReferenceBox.load(fin);
			} else {
				UndefinedBox box = new UndefinedBox(len, new String(type));
				count+=box.load(fin);
			}

		}
		return (int) getSize();
	}

	public SoundMediaHeaderBox getSoundMediaHeaderBox() {
		return soundMediaHeaderBox;
	}

	public VideoMediaHeaderBox getVideoMediaHeaderBox() {
		return videoMediaHeaderBox;
	}

	public HintMediaHeaderBox getHintMediaHeaderBox() {
		return hintMediaHeaderBox;
	}

	public NullMediaHeaderBox getNullMediaHeaderBox() {
		return nullMediaHeaderBox;
	}

	public DataInformationBox getDataInformationBox() {
		return dataInformationBox;
	}

	public SampleTableBox getSampleTableBox() {
		return sampleTableBox;
	}

	public HandlerReferenceBox getHandlerReferenceBox() {
		return handlerReferenceBox;
	}

}
