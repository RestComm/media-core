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
package org.mobicents.media.server.impl.dsp.audio.g729;

/**
 * This is just a buffer that stores temporary info. FIFO/old info get's overwritten.
 * Used to compensate irregularly delivered RTP packets.
 * 
 * @author vralev
 *
 */
public class CircularBuffer {
	
	private byte[] buffer;
	private int cursor = 0;
	private int availableData = 0;
	
	public CircularBuffer(int size) {
		buffer = new byte[size];
	}
	
	synchronized public void addData(byte[] data) {
		boolean zeros = false;
		//for(int q=0; q<data.length; q++) if(data[q]!=0) zeros = false;
		if(!zeros) {
			for(int q=0; q<data.length; q++) {
				buffer[(cursor+q)%buffer.length] = data[q];
			}
			availableData += data.length;
			if(availableData > buffer.length) availableData = buffer.length;
		}
	}
	
	synchronized public byte[] getData(int size) {
		if(availableData<size) return null;
		
		byte[] data = new byte[size];
		for(int q=0; q<data.length; q++) {
			data[q] = buffer[(cursor+q)%buffer.length];
		}
		cursor = (cursor + data.length)%buffer.length;
		availableData -= size;
		return data;
	}

}
