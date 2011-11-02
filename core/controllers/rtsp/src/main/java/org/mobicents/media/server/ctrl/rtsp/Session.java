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
package org.mobicents.media.server.ctrl.rtsp;

import java.util.HashMap;

/**
 * 
 * @author amit bhayani
 * 
 */
public class Session {

	private static int GEN = 1;
	private String id;
	private SessionState state = SessionState.INIT;
	private HashMap attributes = new HashMap();

	protected Session() {

		this.id = Integer.toHexString(GEN++);
		if (GEN == Integer.MAX_VALUE) {
			GEN = 1;
		}
	}

	protected String getId() {
		return this.id;
	}

	protected SessionState getState() {
		return state;
	}

	protected void setState(SessionState state) {
		this.state = state;
	}

	public void addAttribute(String name, Object value) {
		attributes.put(name, value);
	}

	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	public void removeAttribute(String name) {
		attributes.remove(name);
	}
}
