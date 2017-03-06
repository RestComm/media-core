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

package org.restcomm.media.spi.dtmf;

import org.restcomm.media.spi.listener.Event;

/**
 *
 * @author kulikov
 */
public interface DtmfEvent extends Event<DtmfDetector> {
    
    public final static int DTMF_0 = 0;
    public final static int DTMF_1 = 1;
    public final static int DTMF_2 = 2;
    public final static int DTMF_3 = 3;
    public final static int DTMF_4 = 4;
    public final static int DTMF_5 = 5;
    public final static int DTMF_6 = 6;
    public final static int DTMF_7 = 7;
    public final static int DTMF_8 = 8;
    public final static int DTMF_9 = 9;
    public final static int DTMF_A = 10;
    public final static int DTMF_B = 11;
    public final static int DTMF_C = 12;
    public final static int DTMF_D = 13;
    public final static int DTMF_HASH = 14;
    public final static int DTMF_STAR = 15;

    public int getVolume();
    public String getTone();
}
