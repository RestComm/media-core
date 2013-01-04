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

package org.mobicents.media.server.impl.rtp.rfc2833;

import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.text.Format;
import org.mobicents.media.server.component.oob.OOBOutput;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.RTPDataChannel;
import org.mobicents.media.server.io.network.ProtocolHandler;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.memory.Frame;
import org.apache.log4j.Logger;
/**
 *
 * @author Yulian oifa
 */
/**
 * Transmitter implementation.
 *
 */
public class DtmfOutput extends AbstractSink {
	private final static AudioFormat dtmf = FormatFactory.createAudioFormat("telephone-event", 8000);
	
    private RTPDataChannel channel;
    
    private static final Logger logger = Logger.getLogger(DtmfOutput.class);
    
    private OOBOutput oobOutput;
    
    /**
     * Creates new transmitter
     */
    public DtmfOutput(Scheduler scheduler,RTPDataChannel channel) {
        super("Output");
        this.channel=channel;
        oobOutput=new OOBOutput(scheduler,1);
        oobOutput.join(this);        
    }
    
    public OOBOutput getOOBOutput()
    {
    	return this.oobOutput;
    }
    
    public void activate()
    {
    	oobOutput.start();
    }
    
    public void deactivate()
    {
    	oobOutput.stop();
    }    
    
    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
    	channel.sendDtmf(frame);
    }            
}
