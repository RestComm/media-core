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

public class PosKeyValue extends Value {
	private String keyPadKey = null;
	private String posKeyAction = null;
	private Parameter parameter = null;

	public PosKeyValue(Parameter parameter, String keyPadKey, String posKeyAction) {
		super();
		this.parameter = parameter;
		this.keyPadKey = keyPadKey;
		this.posKeyAction = posKeyAction;
	}

	public String getKeyPadKey() {
		return keyPadKey;
	}

	public String getPosKeyAction() {
		return posKeyAction;
	}

	public Parameter getParameter() {
		return this.parameter;
	}

	@Override
	public String toString() {
		String s = this.parameter + "=" + this.keyPadKey + "," + this.posKeyAction;
		return s;
	}

}
