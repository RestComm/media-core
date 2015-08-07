/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag. 
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

package org.mobicents.media.server.component.video;

import java.io.IOException;

import org.mobicents.media.server.component.InbandInput;
import org.mobicents.media.server.spi.memory.Frame;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class VideoInput extends InbandInput {

    private static final long serialVersionUID = -5368081767032694922L;

    private static final String NAME_PREFIX = "compound.audio.input.";

    private int limit = 3;
    private Frame activeFrame = null;
    private byte[] activeData;
    private byte[] oldData;
    private int byteIndex = 0;
    private int count = 0;
    private int packetSize = 0;

    public VideoInput(int inputId, int packetSize) {
        super(NAME_PREFIX + inputId, inputId);
        this.packetSize = packetSize;
    }

    @Override
    public void activate() {
        // Does nothing
    }

    @Override
    public void deactivate() {
        // Does nothing
    }

    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
        // TODO Auto-generated method stub

    }

    public void recycle() {
        super.resetBuffer();

        if (this.activeFrame != null) {
            this.activeFrame.recycle();
        }

        this.activeFrame = null;
        this.activeData = null;
        this.byteIndex = 0;
    }

}
