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

package org.mobicents.media.server;

import java.util.ArrayList;
import java.util.Collection;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;

/**
 *
 * @author kulikov
 */
public class FormatsComparator{
    private ArrayList<RTPFormat> list = new ArrayList(10);

    public Collection intersection(Collection<RTPFormat> fmts1,  Collection<RTPFormat> fmts2) {
        for (RTPFormat f1 : fmts1) {
            for (RTPFormat f2 : fmts2) {
                if (f1.getFormat().matches(f2.getFormat())) list.add(f1);
            }
        }
        return list;
    }
}
