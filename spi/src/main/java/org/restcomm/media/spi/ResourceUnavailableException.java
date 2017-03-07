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
 * @author Oleg Kulikov
 */
public class ResourceUnavailableException extends Exception {

	private static final long serialVersionUID = -7645219194855839093L;

	/**
	 * Creates a new instance of ResourceUnavailableException
	 * 
	 * @param e
	 */
	public ResourceUnavailableException(String message, InterruptedException e) {
		super(message);
	}

	public ResourceUnavailableException() {
		super();

	}

	public ResourceUnavailableException(String message, Throwable cause) {
		super(message, cause);

	}

	public ResourceUnavailableException(String message) {
		super(message);

	}

	public ResourceUnavailableException(Throwable cause) {
		super(cause);

	}

}
