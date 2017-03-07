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
 * Format encoding name.
 * 
 * @author kulikov
 */
public class EncodingName extends Text implements Cloneable {
    public EncodingName() {
        super();
    }

    public EncodingName(Text text) {
    	byte[] newArray=new byte[text.length()];
    	this.strain(newArray,0, this.length());
    	text.duplicate(this);
    }

    public EncodingName(String s) {
        super(s);
    }
    
    @Override
    protected EncodingName clone() {
    	byte[] newArray=new byte[this.length()];
        Text t = new Text();
        t.strain(newArray,0, this.length());
        this.duplicate(t);
        return new EncodingName(t);
    }
}
