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

package org.mobicents.media;

/**
 * Defines all component types available in mms 
 * 
 * Examples of components are the audio player, recoder, 
 * DTMF detector, etc. 
 * 
 * @author yulian oifa
 */
public enum ComponentType {
	DTMF_DETECTOR(0), PLAYER(1), RECORDER(2), SIGNAL_DETECTOR(3), SIGNAL_GENERATOR(4), SS7_INPUT(5), SS7_OUTPUT(6);
	
	private int type;
	 
	 private ComponentType(int type) {
	   this.type=type;
	 }
	 
	 public int getType() {
	   return type;
	 }
}
