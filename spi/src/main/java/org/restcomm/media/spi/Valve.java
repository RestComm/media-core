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

package org.restcomm.media.spi;

/**
 * 
 * @author amit bhayani
 * 
 */
public enum Valve {
	CLOSE(0, "close"), OPEN(1, "open");

	private int code;
	private String name;

	Valve(int code, String name) {
		this.code = code;
		this.name = name;
	}

	public static Valve getInstance(String name) {
		if (name.equalsIgnoreCase("close")) {
			return CLOSE;
		} else if (name.equalsIgnoreCase("open")) {
			return OPEN;
		} else {
			throw new IllegalArgumentException("There is no Valve for: " + name);
		}
	}

	public static Valve getValve(int code) {
		if (code == 0) {
			return CLOSE;
		} else {
			return OPEN;
		}
	}

	public int getCode() {
		return code;
	}

	public String getName() {
		return name;
	}
}
