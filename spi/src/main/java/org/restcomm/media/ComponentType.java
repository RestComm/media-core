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
 * Defines all component types available in mms
 * 
 * Examples of components are the audio player, recoder, DTMF detector, etc.
 * 
 * @author yulian oifa
 */
public enum ComponentType {
    DTMF_DETECTOR(0), DTMF_GENERATOR(1), PLAYER(2), RECORDER(3), SIGNAL_DETECTOR(4), SIGNAL_GENERATOR(5), SINE(6), SPECTRA_ANALYZER(7), SOUND_CARD(8), ASR_ENGINE(9);

    private int type;

    private ComponentType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
