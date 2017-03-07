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

package org.restcomm.media;

/**
 * Standard JMF class -- see <a
 * href="http://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/MediaException.html"
 * target="_blank">this class in the JMF Javadoc</a>.
 * 
 * Complete.
 * 
 * @author Ken Larson
 * 
 */
public class MediaException extends Exception {

	private static final long serialVersionUID = -1186245127611339648L;

	public MediaException() {
		super();
	}

	public MediaException(String message) {
		super(message);
	}

}
