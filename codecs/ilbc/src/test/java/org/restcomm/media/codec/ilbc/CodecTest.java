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

package org.restcomm.media.codec.ilbc;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

import org.junit.Test;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;
/**
 *
 * @author oifa yulian
 */
public class CodecTest {
	
    @Test
    public void testEncode() throws Exception {    	
    	//assertEquals(Short.MAX_VALUE, BasicFunctions.div((short)100,(short)100));        
        
    	//assertEquals(8192, BasicFunctions.div((short)100,(short)400));
    	
    	try
    	{
    	Frame frame = Memory.allocate(320);
    	byte[] data=frame.getData();
    	
    	Frame resFrame=Memory.allocate(38);
    	byte[] resData=resFrame.getData();
    	
    	Frame returnFrame;
    	byte[] returnData;
    	Encoder encoder=new Encoder();    	    
    	Decoder decoder=new Decoder();
    	
    	URL url = this.getClass().getResource("/iLBC.INP");
    	File input = new File(url.getFile());
    	FileInputStream fin = new FileInputStream(input);
    	URL resURL = this.getClass().getResource("/iLBC_Fix.enc");
    	File result = new File(resURL.getFile());
    	FileInputStream fRes = new FileInputStream(result);
    	
    	int currPos=0;
    	int errors=0;
    	int framesCount=0;
    	int framesEqual=0,currEqual;
    	while(currPos<6400)
    	{    	    	
    		currPos+=fin.read(data);
    		    		
    		fRes.read(resData);
    		System.out.println("ENCODING:" + (++framesCount) + " FRAME");
    		returnFrame=encoder.process(frame);
    		returnData=returnFrame.getData();
    		
    		currEqual=0;
    		for(int i=0;i<resData.length/2;i++)
    		{
    			if(resData[i*2]==returnData[i*2+1])
    				currEqual++;
    			
    			if(resData[i*2+1]==returnData[i*2])
    				currEqual++;
    		}
    		
    		if(currEqual==resData.length)
    		{
    			//good frame
    			framesEqual++;
    		}    		
    	}   	
    	    	
    	fin.close();
    	fRes.close();
    	
    	System.out.println("FRAMES:" + framesCount + ",EQUAL:" + framesEqual);
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    }    
}
