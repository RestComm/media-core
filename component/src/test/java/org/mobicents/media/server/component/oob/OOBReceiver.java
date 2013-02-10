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

package org.mobicents.media.server.component.oob;

import org.mobicents.media.ComponentType;
import org.mobicents.media.server.scheduler.Scheduler;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.memory.Frame;

/**
 * 
 * @author yulian oifa
 */
public class OOBReceiver extends AbstractSink {
	private final static AudioFormat dtmf = FormatFactory.createAudioFormat("telephone-event", 8000);
	private long period = 20000000L;
    private int packetSize = 4;
    private int count=0;
    
    private OOBOutput output;
    
    public OOBReceiver(String name,Scheduler scheduler) {
        super(name);
        output=new OOBOutput(scheduler,ComponentType.SPECTRA_ANALYZER.getType());
        output.join(this);
    }

    public OOBOutput getOOBOutput()
    {
    	return this.output;
    }
    
    public void activate()
    {
    	this.count = 0;
        System.out.println("start, count=" + count);
        output.start();
    }
    
    public void deactivate()
    {
    	output.stop();
    }        
    
    public void onMediaTransfer(Frame frame) throws IOException {
        byte[] data = frame.getData();
        if(frame.getLength()==packetSize)
        	count++;        
    }

    public int getPacketsCount() {
        return this.count;
    }    
}