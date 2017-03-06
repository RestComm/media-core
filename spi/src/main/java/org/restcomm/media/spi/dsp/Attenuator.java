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

package org.restcomm.media.spi.dsp;

/**
 * Attenuator is used to reduce the power of signal without distorting it
 * 
 * @author amit bhayani
 * 
 */
public interface Attenuator {

	/**
	 * Set the voulme in decible by which you want to reduce the strength. For
	 * example setting it to -3db will reduce the power by half; setting it to
	 * -10db will reduce the power by 1/10th.
	 * 
	 * @param volume
	 */
	public void setVolume(int volume);

	public int getVolume();

}
