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

package org.restcomm.media.spi.format;

import org.restcomm.media.spi.utils.Text;

/**
 * Media format descriptor.
 *
 * @author kulikov
 */
public class Format implements Cloneable {
    //encoding name
    private EncodingName name;

    //any specific options
    private Text options;

    private Boolean sendPTime=false;
    
    /**
     * Creates new descriptor.
     *
     * @param name the encoding name
     */
    public Format(EncodingName name) {
        this.name = name;
    }

    
    /**
     * Gets the encoding name.
     *
     * @return the encoding name.
     */
    public EncodingName getName() {
        return name;
    }

    /**
     * Modifies encoding name.
     *
     * @param name new encoding name.
     */
    public void setName(EncodingName name) {
        this.name = name;
    }

    /**
     * Gets options
     *
     * @return options as text.
     */
    public Text getOptions() {
        return options;
    }

    /**
     * Modify options.
     *
     * @param options new options.
     */
    public void setOptions(Text options) {
        this.options = options;
    }

    @Override
    public String toString() {
        return name.toString();
    }

    /**
     * Compares two format descriptors.
     *
     * @param fmt the another format descriptor
     * @return
     */
    public boolean matches(Format fmt) {
        return this.name.equals(fmt.name);
    }

    public boolean shouldSendPTime() {
        return sendPTime;
    }
    
    public void setSendPTime(Boolean newValue) {
        sendPTime=newValue;
    }
    
    @Override
    public Format clone() {
        return null;
    }
}
