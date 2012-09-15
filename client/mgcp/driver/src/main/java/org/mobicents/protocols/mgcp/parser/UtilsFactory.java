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

package org.mobicents.protocols.mgcp.parser;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class UtilsFactory {

	private static final Logger logger = Logger.getLogger(UtilsFactory.class);

	private List<Utils> list = new ArrayList<Utils>();
	private int size = 0;
	private int count = 0;

	public UtilsFactory(int size) {
		this.size = size;
		for (int i = 0; i < size; i++) {
			Utils utils = new Utils();
			list.add(utils);
		}
	}

	public Utils allocate() {
		Utils utils = null;

		if (!list.isEmpty()) {
			utils = list.remove(0);
			if (utils != null) {
				return utils;
			}
		}

		utils = new Utils();
		count++;

		if (logger.isInfoEnabled()) {
			logger.info("UtilsFactory underflow. Better to increase the size of Utils count. Count = " + count);
		}
		return utils;
	}

	public void deallocate(Utils utils) {
		if (list.size() < size && utils != null) {
			list.add(utils);
		}
	}

	public int getSize() {
		return this.size;
	}

	public int getCount() {
		return this.count;
	}

}
