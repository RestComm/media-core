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
package org.restcomm.media.control.mgcp.pkg.dtmf;

import org.restcomm.media.control.mgcp.controller.signal.Event;
import org.restcomm.media.spi.utils.Text;

/**
 *
 * @author kulikov
 */
public class Dtmf1 extends AbstractDtmfEvent {
    private final static Event dtmf_1 = new Event(new Text("1"));

    public Dtmf1(String name) {
        super(name);
    }

    @Override
    public void onEvent(String tone) {
        if (tone.equals("1")) {
            dtmf_1.fire(this, null);
        }
    }

    @Override
	protected Event getTone() {
		return dtmf_1;
	}
}