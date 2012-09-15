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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DisplayTextSeg {
	private Map<String, String> segSelectorMap = new HashMap<String, String>();
	private String displayText = null;

	public DisplayTextSeg(String displayText) {
		this.displayText = displayText;
	}

	public Map<String, String> getSegSelectorMap() {
		return segSelectorMap;
	}

	public void setSegSelectorMap(Map<String, String> segSelectorMap) {
		this.segSelectorMap = segSelectorMap;
	}

	public String getTextToSpeech() {
		return displayText;
	}

	@Override
	public String toString() {
		String s = ParameterEnum.dt + "(" + this.displayText + ")";

		if (segSelectorMap.size() > 0) {
			s = s + "[";
			boolean first = true;
			Set<String> keys = segSelectorMap.keySet();
			for (String str : keys) {
				if (first) {
					s = s + str + "=" + segSelectorMap.get(str);
					first = false;
				} else {
					s = s + "," + str + "=" + segSelectorMap.get(str);
				}

			}
			s = s + "]";
		}
		return s;
	}
}
