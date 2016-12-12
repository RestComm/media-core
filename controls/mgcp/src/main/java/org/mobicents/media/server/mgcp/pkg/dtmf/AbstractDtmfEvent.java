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
package org.mobicents.media.server.mgcp.pkg.dtmf;

import org.mobicents.media.ComponentType;
import org.mobicents.media.server.mgcp.controller.signal.Event;
import org.mobicents.media.server.mgcp.controller.signal.NotifyImmediately;
import org.mobicents.media.server.mgcp.controller.signal.Signal;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.dtmf.DtmfEvent;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.utils.Text;

/**
 *
 * @author yulian oifa
 */
public abstract class AbstractDtmfEvent extends Signal implements DtmfDetectorListener{

    private DtmfDetector dtmfDetector;
    protected Event tone;

    public AbstractDtmfEvent(String name) {
        super(name);
        this.tone = getTone();
        this.tone.add(new NotifyImmediately("N"));
    }

    @Override
    public void execute() {
        dtmfDetector = this.getDetector();

        if (dtmfDetector == null) {
            return;
        }

        try {
        	dtmfDetector.addListener(this); 
        	dtmfDetector.activate();
        } catch (TooManyListenersException e) {
        }

    }

    @Override
    public boolean doAccept(Text event) {
    	boolean b= !tone.isActive() && tone.matches(event);
    	if(b)
    	{
    		execute();
    	}
        return b;
    }

    @Override
    public void cancel() {
        if (dtmfDetector != null) {
            dtmfDetector.removeListener(this);
            dtmfDetector.deactivate();
            dtmfDetector=null;
        }
    }

    @Override
    public void process(DtmfEvent event) {
        this.onEvent(event.getTone());
    }

    @Override
    public void reset() {
        tone.reset();
    }

    public abstract void onEvent(String tone);

    protected abstract Event getTone();
    
    private DtmfDetector getDetector() {
        return (DtmfDetector) getEndpoint().getResource(MediaType.AUDIO, ComponentType.DTMF_DETECTOR);
    }
}