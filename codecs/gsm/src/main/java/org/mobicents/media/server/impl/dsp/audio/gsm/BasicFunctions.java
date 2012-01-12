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

package org.mobicents.media.server.impl.dsp.audio.gsm;

import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;
import org.tritonus.lowlevel.gsm.InvalidGSMFrameException;

/**
 * 
 * @author amit bhayani
 * @kulikov
 */
public class BasicFunctions {

    public BasicFunctions() {
    }
    
    public static short add(short var1,short var2)
    {
    	return 0;
    }
    
    public static short sub(short var1,short var2)
    {
    	return 0;
    }
    
    public static short mult(short var1,short var2)
    {
    	return 0;
    }
    
    public static int mult_r(int var1,int var2)
    {
    	return 0;
    }

    public static int abs(int var1,int var2)
    {
    	return 0;
    }
    
    public static int div(int var1,int var2)
    {
    	return 0;
    }
    
    public static int L_mult(int var1,int var2)
    {
    	return 0;
    }
    
    public static int L_add(int var1,int var2)
    {
    	return 0;
    }
    
    public static int L_sub(int var1,int var2)
    {
    	return 0;
    }

    public static int norm(int var1,int var2)
    {
    	return 0;
    }
    
    //L_var2 = var1
    //var2 = L_var1
}
