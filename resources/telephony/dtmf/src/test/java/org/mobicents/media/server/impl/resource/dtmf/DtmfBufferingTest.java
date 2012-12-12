/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.impl.resource.dtmf;

import java.io.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.component.audio.CompoundComponent;
import org.mobicents.media.server.component.audio.CompoundMixer;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.dtmf.DtmfEvent;
import org.mobicents.media.server.spi.listener.TooManyListenersException;

/**
 *
 * @author yulian oifa
 */
public class DtmfBufferingTest implements DtmfDetectorListener {
    
    private Clock clock;
    private Scheduler scheduler;
    
    private DetectorImpl detector;
    private GeneratorImpl generator;
    
    private CompoundComponent detectorComponent;
    private CompoundComponent generatorComponent;
    private CompoundMixer compoundMixer;
    
    private String tone;
    
    public DtmfBufferingTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() throws TooManyListenersException {
        clock = new DefaultClock();

        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();
        
        generator = new GeneratorImpl("dtmf", scheduler);
        generator.setToneDuration(100);
        generator.setVolume(-20);
        
        detector = new DetectorImpl("dtmf", scheduler);
        detector.setVolume(-35);
        detector.setDuration(40);
        
        compoundMixer=new CompoundMixer(scheduler);
        
        detectorComponent=new CompoundComponent(1);
        detectorComponent.addOutput(detector.getCompoundOutput());
        detectorComponent.updateMode(false,true);
        
        generatorComponent=new CompoundComponent(2);
        generatorComponent.addInput(generator.getCompoundInput());
        generatorComponent.updateMode(true,false);
                
        compoundMixer.addComponent(detectorComponent);
        compoundMixer.addComponent(generatorComponent);
        tone="";
    }
    
    @After
    public void tearDown() {
    	generator.deactivate();
    	detector.deactivate();
    	compoundMixer.stop();
        scheduler.stop();
    }

    /**
     * Test of setDuration method, of class DetectorImpl.
     */
    @Test
    public void testFlush() throws InterruptedException, TooManyListenersException {
        //queue "1" into detector's buffer
        generator.setDigit("1");
        generator.activate();
        detector.activate();
    	compoundMixer.start();
        
        Thread.sleep(200);        

        generator.deactivate();
        detector.deactivate();
    	compoundMixer.stop();
        
        //assign listener and flush digit
        detector.addListener(this);
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(300);        
        
        assertEquals("1", tone);
    }

    @Test
    public void testBuffering() throws InterruptedException, TooManyListenersException {
        //queue "1" into detector's buffer
        generator.setDigit("1");
        generator.activate();
        detector.activate();
    	compoundMixer.start();
        
        Thread.sleep(200);          
        
        //queue "2" into detector's buffer
        generator.setDigit("2");
        generator.wakeup();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
    	compoundMixer.stop();
        
        //assign listener and flush digit
        detector.addListener(this);
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(200);        
        
        assertEquals("12", tone);
    }

    @Test
    public void testDelivery() throws InterruptedException, TooManyListenersException {
        //assign listener
        detector.addListener(this);
        
        //queue "1" into detector's buffer
        generator.setDigit("1");
        generator.activate();
        detector.activate();
    	compoundMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
    	compoundMixer.stop();    	

        //queue "2" into detector's buffer
        generator.setDigit("2");
        generator.activate();
        detector.activate();
    	compoundMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
    	compoundMixer.stop();
        
        assertEquals("12", tone);
        
        //assign listener and flush digit
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(200);        
        
        assertEquals("12", tone);
    }
    
    @Test
    public void testClear() throws InterruptedException, TooManyListenersException {
        //queue "1" into detector's buffer
        generator.setDigit("1");
        generator.activate();
        detector.activate();
    	compoundMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
    	compoundMixer.stop();

        //queue "2" into detector's buffer
        generator.setDigit("2");
        generator.activate();
        detector.activate();
    	compoundMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
    	compoundMixer.stop();
        
        //assign listener and flush digit
        detector.addListener(this);
        detector.clearBuffer();
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(200);        
        
        assertEquals("", tone);
    }
    
    /*@Test
    public void testGenerateFile()
   	{
    	try
    	{
    		java.io.File currFile=new File("testOutput.txt");
    		java.io.FileOutputStream fout = new FileOutputStream(currFile);
    		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fout));
    		String currString;
    		
    		generator.setToneDuration(100);    		
    		generator.setVolume(-20);
			
    		for(int i=0;i<=9;i++)
    		{    		
    			writer.write("buffer[" + i + "]=new byte[] {");
    			
    			generator.setDigit(String.valueOf((char)(i+'0')));
    			for(int j=0;j<5;j++)
    			{
    				Frame currFrame=generator.evolve(0L);
    				byte[] data=currFrame.getData();
    				for(int k=0;k<data.length;k++)
    				{    					
    					currString="(byte) 0X" + String.format("%02X", data[k]);
    					if(k<data.length-1 || j<4)
    						 currString+= ",";
    					
    					writer.write(currString);
    				}
    			}
    			    			
    			writer.write("};");    			
    			writer.newLine();    			
    		}
    		
    		writer.write("buffer[10]=new byte[] {");
			
			generator.setDigit("*");
			for(int j=0;j<5;j++)
			{
				Frame currFrame=generator.evolve(0L);
				byte[] data=currFrame.getData();
				for(int k=0;k<data.length;k++)
				{
					currString="(byte) 0X" + String.format("%02X", data[k]);
					if(k<data.length-1 || j<4)
						 currString+= ",";
					
					writer.write(currString);
				}
			}
			    			
			writer.write("};");    			
			writer.newLine();  
			
			writer.write("buffer[11]=new byte[] {");
			
			generator.setDigit("#");
			for(int j=0;j<5;j++)
			{
				Frame currFrame=generator.evolve(0L);
				byte[] data=currFrame.getData();
				for(int k=0;k<data.length;k++)
				{
					currString="(byte) 0X" + String.format("%02X", data[k]);
					if(k<data.length-1 || j<4)
						 currString+= ",";
					
					writer.write(currString);
				}
			}
			    			
			writer.write("};");    			
			writer.newLine(); 
			
    		for(int i=0;i<4;i++)
    		{    		
    			writer.write("buffer[" + (i+12) + "]=new byte[] {");
    			
    			generator.setDigit(String.valueOf((char)(i+'A')));
    			for(int j=0;j<5;j++)
    			{
    				Frame currFrame=generator.evolve(0L);
    				byte[] data=currFrame.getData();
    				for(int k=0;k<data.length;k++)
    				{
    					currString="(byte) 0X" + String.format("%02X", data[k]);
    					if(k<data.length-1 || j<4)
    						 currString+= ",";
    					
    					writer.write(currString);
    				}
    			}
    			    			
    			writer.write("};");    			
    			writer.newLine();    			
    		}
    		
    		writer.flush();
    		fout.close();  
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
   	}*/
    	
    public void process(DtmfEvent event) {
        tone += event.getTone();
    }
}
