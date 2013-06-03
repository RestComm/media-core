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

package org.mobicents.media.server.impl.dsp.audio.ilbc;

import java.nio.channels.FileChannel;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;
/**
 *
 * @author oifa yulian
 */
public class CodecTest 
{
	public CodecTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {        
    }

    @After
    public void tearDown() {    	
    }

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
    	
    	Frame returnFrame,decodedFrame;
    	byte[] decodedData;
    	
    	URL url = this.getClass().getResource("/iLBC.INP");
    	File input = new File(url.getFile());
    	FileInputStream fin = new FileInputStream(input);
    	URL resURL = this.getClass().getResource("/iLBC_20ms.BIT");
    	File result = new File(resURL.getFile());
    	FileInputStream fRes = new FileInputStream(result);
    	
    	int currPos=0;
    	int errors=0;
    	int framesCount=0;
    	int framesEqual=0,currEqual;
    	while(currPos<320)
    	//while(currPos<6400)
    	{    	    	
    		Encoder encoder=new Encoder();    	    
    		Decoder decoder=new Decoder();
        	
    		currPos+=fin.read(data);    		    	
    		fRes.read(resData);
    		
    		returnFrame=encoder.process(frame);
    		decodedFrame=decoder.process(returnFrame);
    		decodedData=decodedFrame.getData();
    		currEqual=0;
    		for(int i=0;i<returnFrame.getData().length/2;i++)
    		{
    			if(returnFrame.getData()[i*2]==resData[i*2+1])
    				currEqual++;
    			
    			if(returnFrame.getData()[i*2+1]==resData[i*2])
    				currEqual++;    			    			    		
    		}
    		
    		if(currEqual==resData.length)
    		{
    			//good frame
    			System.out.println("GOOD FRAME!!!!");
    			framesEqual++;
    		}   
    		else
    			System.out.println("EQUAL DATA:" + currEqual);
    		
    		framesCount++;
    	}   	
    	    	
    	fin.close();
    	//fRes.close();
    	
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    }    
}
