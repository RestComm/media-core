/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.media.network.deprecated.channel;

import java.util.List;

import org.junit.Test;
import org.restcomm.media.network.deprecated.channel.PacketHandler;
import org.restcomm.media.network.deprecated.channel.PacketHandlerPipeline;

import junit.framework.Assert;


/**
 * Unit tests for {@link PacketHandlerPipeline}
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class PacketHandlerPipelineTest {

	private final PacketHandlerMock lpHandler = new LowPriorityPacketHandlerMock();
	private final PacketHandlerMock mpHandler = new MediumPriorityPacketHandlerMock();
	private final PacketHandlerMock hpHandler = new HighPriorityPacketHandlerMock();
	
	@Test
	public void testRegisterHandler() {
		// given
		PacketHandlerPipeline pipeline = new PacketHandlerPipeline();
		
		// when
		pipeline.addHandler(lpHandler);
		pipeline.addHandler(lpHandler);
		
		// then
		Assert.assertEquals(1, pipeline.count());
		Assert.assertEquals(lpHandler, pipeline.getHandlers().get(0));
	}
	
	@Test
	public void testPriorityQueue() {
		// given
		PacketHandlerPipeline pipeline = new PacketHandlerPipeline();
		
		// when
		pipeline.addHandler(mpHandler);
		pipeline.addHandler(hpHandler);
		pipeline.addHandler(lpHandler);
		
		// then
		Assert.assertEquals(3, pipeline.count());
		
		List<PacketHandler> handlers = pipeline.getHandlers();
		Assert.assertEquals(hpHandler, handlers.get(0));
		Assert.assertEquals(mpHandler, handlers.get(1));
		Assert.assertEquals(lpHandler, handlers.get(2));
	}
	
	@Test
	public void testFindHandler() {
		// given
		PacketHandlerPipeline pipeline = new PacketHandlerPipeline();
		
		// when
		pipeline.addHandler(hpHandler);
		pipeline.addHandler(lpHandler);
		
		// then
		Assert.assertTrue(pipeline.contains(lpHandler));
		Assert.assertTrue(pipeline.contains(hpHandler));
		Assert.assertFalse(pipeline.contains(mpHandler));
	}

	@Test
	public void testGetHandler() {
		// given
		PacketHandlerPipeline pipeline = new PacketHandlerPipeline();
		String msg = "low";
		byte[] msgData = msg.getBytes();
		
		// when
		pipeline.addHandler(hpHandler);
		pipeline.addHandler(lpHandler);
		pipeline.addHandler(mpHandler);
		
		PacketHandler handler = pipeline.getHandler(msgData);
		
		// then
		Assert.assertEquals(lpHandler, handler);
	}
	
	
}
