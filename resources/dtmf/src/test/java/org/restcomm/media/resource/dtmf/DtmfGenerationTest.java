/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.restcomm.media.resource.dtmf;

import java.io.FileOutputStream;
import java.io.StringWriter;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioMixer;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.component.oob.OOBMixer;
import org.restcomm.media.resource.dtmf.GeneratorImpl;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.WallClock;
import org.restcomm.media.spi.dtmf.DtmfDetectorListener;
import org.restcomm.media.spi.dtmf.DtmfEvent;
import org.restcomm.media.spi.listener.TooManyListenersException;
import org.restcomm.media.spi.memory.Frame;

/**
 *
 * @author yulian oifa
 */
public class DtmfGenerationTest {
    
    private Clock clock;
    private PriorityQueueScheduler scheduler;
    
    private GeneratorImpl generator;
    
    private String tone;
    
    public DtmfGenerationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() throws TooManyListenersException {
    	clock = new WallClock();

        scheduler = new PriorityQueueScheduler();
        scheduler.setClock(clock);
        scheduler.start();
        
        generator = new GeneratorImpl("dtmf", scheduler);               
    }
    
    @After
    public void tearDown() {    	    
    }

    /**
     * Test of setDuration method, of class DetectorImpl.
     */    
    @Test
    public void testGenerator() throws InterruptedException {    	
    	/*generator.setVolume(-10);
    	generator.setToneDuration(100);
    	    	
    	FileOutputStream stream=null;
    	StringWriter stringWriter=null;
    	try
    	{   
    		java.io.File file = new java.io.File("/opt/mobsource/medianew/resources/telephony/dtmf/tones.txt");  
    		stream=new FileOutputStream(file);
    		stringWriter=new StringWriter();
    	}
    	catch(Exception ex)
    	{
    		ex.printStackTrace();
    	}
    	
    	for(int i=0;i<=9;i++)
    	{
    		generator.setDigit(new String(new char[] {(char)('0' + i)}));
    		generator.activate();
    		generator.deactivate();
    		
    		stringWriter.write("buffer[" + i + "]=new byte[] {");
    		for(int j=0;j<5;j++)
    		{
    			Frame currFrame=generator.evolve(0);    			
    			byte[] data=currFrame.getData();
    			for(int k=0;k<data.length;k++)
    				if(k==data.length-1 && j==4)
    					stringWriter.write("(byte) 0x" + String.format("%02X", data[k]&0xff));
    				else
    					stringWriter.write("(byte) 0x" + String.format("%02X", data[k]&0xff) + ",");
    		}
    		stringWriter.write("};\r\n");
    	}
    	
    	generator.setDigit(new String(new char[] {'*'}));
		generator.activate();
		generator.deactivate();
		
		stringWriter.write("buffer[10]=new byte[] {");
		for(int j=0;j<5;j++)
		{
			Frame currFrame=generator.evolve(0);    			
			byte[] data=currFrame.getData();
			for(int k=0;k<data.length;k++)
				if(k==data.length-1 && j==4)
					stringWriter.write("(byte) 0x" + String.format("%02X", data[k]&0xff));
				else
					stringWriter.write("(byte) 0x" + String.format("%02X", data[k]&0xff) + ",");
		}
		stringWriter.write("};\r\n");
		
		generator.setDigit(new String(new char[] {'#'}));
		generator.activate();
		generator.deactivate();
		
		stringWriter.write("buffer[11]=new byte[] {");
		for(int j=0;j<5;j++)
		{
			Frame currFrame=generator.evolve(0);    			
			byte[] data=currFrame.getData();
			for(int k=0;k<data.length;k++)
				if(k==data.length-1 && j==4)
					stringWriter.write("(byte) 0x" + String.format("%02X", data[k]&0xff));
				else
					stringWriter.write("(byte) 0x" + String.format("%02X", data[k]&0xff) + ",");
		}
		stringWriter.write("};\r\n");
		
		for(int i=0;i<4;i++)
    	{
    		generator.setDigit(new String(new char[] {(char)('A' + i)}));
    		generator.activate();
    		generator.deactivate();
    		
    		stringWriter.write("buffer[" + (i+12) + "]=new byte[] {");
    		for(int j=0;j<5;j++)
    		{
    			Frame currFrame=generator.evolve(0);    			
    			byte[] data=currFrame.getData();
    			for(int k=0;k<data.length;k++)
    				if(k==data.length-1 && j==4)
    					stringWriter.write("(byte) 0x" + String.format("%02X", data[k]&0xff));
    				else
    					stringWriter.write("(byte) 0x" + String.format("%02X", data[k]&0xff) + ",");
    		}
    		stringWriter.write("};\r\n");
    	}
		
    	try
    	{
    		stream.write(stringWriter.getBuffer().toString().getBytes());        	
        	stream.close();
    	}
    	catch(Exception ex)
    	{
    		
    	}*/
    }    
}
