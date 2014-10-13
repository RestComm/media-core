package org.mobicents.media.server.io.network.channel;

import java.util.List;

import org.junit.Test;

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
	public void testGetHandlersCopy() {
		// given
		PacketHandlerPipeline pipeline = new PacketHandlerPipeline();
		
		// when
		pipeline.addHandler(hpHandler);
		pipeline.addHandler(lpHandler);
		
		List<PacketHandler> handlers = pipeline.getHandlers();
		
		pipeline.addHandler(mpHandler);
		
		// then
		Assert.assertTrue(handlers.contains(hpHandler));
		Assert.assertFalse(handlers.contains(mpHandler));
	}
	
}
