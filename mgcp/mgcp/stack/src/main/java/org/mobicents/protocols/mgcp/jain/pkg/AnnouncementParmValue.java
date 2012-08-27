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

package org.mobicents.protocols.mgcp.jain.pkg;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementParmValue extends Value {

	List<SegmentId> segmentIds = new ArrayList<SegmentId>();
	List<TextToSpeechSeg> textToSpeechSegs = new ArrayList<TextToSpeechSeg>();
	List<DisplayTextSeg> displayTextSegs = new ArrayList<DisplayTextSeg>();
	List<SilenceSeg> silenceSegs = new ArrayList<SilenceSeg>();
	Parameter parameter = null;

	public AnnouncementParmValue(Parameter parameter) {
		this.parameter = parameter;
	}

	public List<SegmentId> getSegmentIds() {
		return segmentIds;
	}

	public void addSegmentId(SegmentId s) {
		segmentIds.add(s);
	}

	public List<TextToSpeechSeg> getTextToSpeechSegs() {
		return textToSpeechSegs;
	}

	public void addTextToSpeechSeg(TextToSpeechSeg textToSpeechSeg) {
		textToSpeechSegs.add(textToSpeechSeg);
	}

	public List<DisplayTextSeg> getDisplayTextSegs() {
		return displayTextSegs;
	}

	public void addDisplayTextSeg(DisplayTextSeg displayTextSeg) {
		displayTextSegs.add(displayTextSeg);
	}

	public List<SilenceSeg> getSilenceSegs() {
		return silenceSegs;
	}

	public void addSilenceSeg(SilenceSeg silenceSeg) {
		silenceSegs.add(silenceSeg);
	}

	@Override
	public String toString() {

		String s = this.parameter + "=";
		boolean first = true;
		if (segmentIds.size() > 0) {

			for (SegmentId sId : segmentIds) {
				if (first) {
					s = s + sId.toString();
					first = false;
				} else {
					s = s + "," + sId.toString();
				}
			}
		}

		if (textToSpeechSegs.size() > 0) {
			for (TextToSpeechSeg ttsSeg : textToSpeechSegs) {
				if (first) {
					s = s + ttsSeg.toString();
					first = false;
				} else {
					s = s + "," + ttsSeg.toString();
				}
			}
		}

		if (displayTextSegs.size() > 0) {
			for (DisplayTextSeg dtSeg : displayTextSegs) {
				if (first) {
					s = s + dtSeg.toString();
					first = false;
				} else {
					s = s + "," + dtSeg.toString();
				}
			}
		}

		if (silenceSegs.size() > 0) {
			for (SilenceSeg siSeg : silenceSegs) {
				if (first) {
					s = s + siSeg.toString();
					first = false;
				} else {
					s = s + "," + siSeg.toString();
				}
			}
		}

		return s;
	}

}
