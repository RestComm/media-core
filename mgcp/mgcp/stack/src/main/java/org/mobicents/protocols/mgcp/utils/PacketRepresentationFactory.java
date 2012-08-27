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

package org.mobicents.protocols.mgcp.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class PacketRepresentationFactory {

	private static final Logger logger = Logger.getLogger(PacketRepresentationFactory.class);

	private List<PacketRepresentation> list = new ArrayList<PacketRepresentation>();
	private int size = 0;
	private int dataArrSize = 0;
	private int count = 0;

	public PacketRepresentationFactory(int size, int dataArrSize) {
		this.size = size;
		this.dataArrSize = dataArrSize;
		for (int i = 0; i < size; i++) {
			PacketRepresentation pr = new PacketRepresentation(dataArrSize, this); 
			list.add(pr);
		}
	}

	public PacketRepresentation allocate() {
		PacketRepresentation pr = null;

		if (!list.isEmpty()) {
			pr = list.remove(0);
			
		}
		
		if(pr!=null){
			pr.setLength(0);
			return pr;
		}

		pr = new PacketRepresentation(this.dataArrSize, this);
		count++;

		if (logger.isDebugEnabled()) {
			logger.debug("UtilsFactory underflow. Count = " + count);			
		}
		
		logger.error("UtilsFactory underflow. Count = " + count);
		
		return pr;
	}

	public void deallocate(PacketRepresentation pr) {
		if (list.size() < size && pr != null) {
			list.add(pr);
		} else{
			System.out.println("Discarding the PR "+pr);
		}
	}

	public int getSize() {
		return this.size;
	}

	public int getDataArrSize() {
		return this.dataArrSize;
	}

	public int getCount() {
		return this.count;
	}

}
