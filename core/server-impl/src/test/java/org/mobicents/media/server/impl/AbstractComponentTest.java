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

package org.mobicents.media.server.impl;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.Inlet;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.Outlet;
import org.mobicents.media.Server;
import org.mobicents.media.server.spi.rtp.AVProfile;

/**
 *
 * @author kulikov
 */
public class AbstractComponentTest {

    private Server server;
    
    private TestSource src = new TestSource("source");
    private TestSource2 src2 = new TestSource2("source2");
    private TestSink sink = new TestSink("sink");
    private TestSink2 sink2 = new TestSink2("sink2");

    private Inlet inlet = new InletImpl("test-inlet");
    private Outlet outlet = new OutletImpl("test-outlet");
    
    private Format FORMAT = AVProfile.L16_MONO;
    
    public AbstractComponentTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
//        server = new Server();
//        server.start();
    }

    @After
    public void tearDown() {
//        server.stop();
    }

    /**
     * Test of connect method, of class AbstractSink.
     */
    @Test
    public void testSource2SinkDirectConnectDisconnect() {
        src.connect(sink);
        assertEquals(true, src.isConnected());
        assertEquals(true, sink.isConnected());

        src.disconnect(sink);
        assertEquals(false, src.isConnected());
        assertEquals(false, sink.isConnected());
    }

    @Test
    public void testSource2OneSinkConnectDisconnect() {
        src.connect(sink);
        assertEquals(true, src.isConnected());
        assertEquals(true, sink.isConnected());

        sink.disconnect(src);
        assertEquals(false, src.isConnected());
        assertEquals(false, sink.isConnected());
    }

    @Test
    public void testSourceSet2SinkDirectConnectDisconnect() {
        src2.connect(sink);
        assertEquals(true, sink.isConnected());
        assertEquals(1, src2.getActiveSourceCount());

        src2.disconnect(sink);
        assertEquals(0, src2.getActiveSourceCount());
        assertEquals(false, sink.isConnected());

        sink.connect(src2);
        assertEquals(1, src2.getActiveSourceCount());
        assertEquals(true, sink.isConnected());

        sink.disconnect(src2);
        assertEquals(0, src2.getActiveSourceCount());
        assertEquals(false, sink.isConnected());
    }

    @Test
    public void testSourceSet2SinkCrosswayConnectDisconnect() {
        src2.connect(sink);
        assertEquals(1, src2.getActiveSourceCount());
        assertEquals(true, sink.isConnected());

        sink.disconnect(src2);
        assertEquals(0, src2.getActiveSourceCount());
        assertEquals(false, sink.isConnected());

        sink.connect(src2);
        assertEquals(1, src2.getActiveSourceCount());
        assertEquals(true, sink.isConnected());

        src2.disconnect(sink);
        assertEquals(0, src2.getActiveSourceCount());
        assertEquals(false, sink.isConnected());
    }
    
    @Test
    public void testSource2SinkSetDirectConnectDisconnect2() {
        src.connect(sink2);
        assertEquals(true, src.isConnected());
        assertEquals(1, sink2.getActiveSinkCount());

        src.disconnect(sink2);
        assertEquals(0, sink2.getActiveSinkCount());
        assertEquals(false, sink.isConnected());

        sink2.connect(src);
        assertEquals(true, src.isConnected());
        assertEquals(1, sink2.getActiveSinkCount());

        sink2.disconnect(src);
        assertEquals(false, src.isConnected());
        assertEquals(0, sink2.getActiveSinkCount());
    }

    @Test
    public void testSource2SinkSetCrosswayConnectDisconnect2() {
        src.connect(sink2);
        assertEquals(true, src.isConnected());
        assertEquals(1, sink2.getActiveSinkCount());

        sink2.disconnect(src);
        assertEquals(0, sink2.getActiveSinkCount());
        assertEquals(false, sink.isConnected());

        sink2.connect(src);
        assertEquals(true, src.isConnected());
        assertEquals(1, sink2.getActiveSinkCount());

        src.disconnect(sink2);
        assertEquals(false, src.isConnected());
        assertEquals(0, sink2.getActiveSinkCount());
    }


    @Test
    public void testSource2InletDirectConnectDisconnect2() {
        src.connect(inlet);
        assertEquals(true, src.isConnected());
        assertEquals(true, inlet.getInput().isConnected());

        src.disconnect(inlet);
        assertEquals(false, inlet.getInput().isConnected());
        assertEquals(false, sink.isConnected());

        inlet.connect(src);
        assertEquals(true, src.isConnected());
        assertEquals(true, inlet.getInput().isConnected());

        inlet.disconnect(src);
        assertEquals(false, src.isConnected());
        assertEquals(false, inlet.getInput().isConnected());
    }

    @Test
    public void testSource2InletCrosswayConnectDisconnect2() {
        src.connect(inlet);
        assertEquals(true, src.isConnected());
        assertEquals(true, inlet.getInput().isConnected());

        inlet.disconnect(src);
        assertEquals(false, inlet.getInput().isConnected());
        assertEquals(false, sink.isConnected());

        inlet.connect(src);
        assertEquals(true, src.isConnected());
        assertEquals(true, inlet.getInput().isConnected());

        src.disconnect(inlet);
        assertEquals(false, src.isConnected());
        assertEquals(false, inlet.getInput().isConnected());
    }

    @Test
    public void testSink2OutletDirectConnectDisconnect2() {
        sink.connect(outlet);
        assertEquals(true, sink.isConnected());
        assertEquals(true, outlet.getOutput().isConnected());

        sink.disconnect(outlet);
        assertEquals(false, sink.isConnected());
        assertEquals(false, outlet.getOutput().isConnected());

        outlet.connect(sink);
        assertEquals(true, sink.isConnected());
        assertEquals(true, outlet.getOutput().isConnected());

        outlet.disconnect(sink);
        assertEquals(false, sink.isConnected());
        assertEquals(false, outlet.getOutput().isConnected());
    }

    @Test
    public void testSink2OutletCrosswayConnectDisconnect2() {
        sink.connect(outlet);
        assertEquals(true, sink.isConnected());
        assertEquals(true, outlet.getOutput().isConnected());

        outlet.disconnect(sink);
        assertEquals(false, sink.isConnected());
        assertEquals(false, outlet.getOutput().isConnected());

        outlet.connect(sink);
        assertEquals(true, sink.isConnected());
        assertEquals(true, outlet.getOutput().isConnected());

        sink.disconnect(outlet);
        assertEquals(false, sink.isConnected());
        assertEquals(false, outlet.getOutput().isConnected());
    }
    
    private class TestSource extends AbstractSource {

        public TestSource(String name) {
            super(name);
        }

        public Format[] getFormats() {
            return new Format[]{FORMAT};
        }

        public String getOtherPartyName() {
            return this.otherParty.getName();
        }

        @Override
        public void evolve(Buffer buffer, long timestamp) {
        }
    }

    private class TestSink extends AbstractSink {

        public TestSink(String name) {
            super(name);
        }

        public Format[] getFormats() {
            return new Format[]{FORMAT};
        }

        public boolean isAcceptable(Format format) {
            return true;
        }

        public String getOtherPartyName() {
            return this.otherParty.getName();
        }

        @Override
        public void onMediaTransfer(Buffer buffer) {
        }
    }

    private class TestSource2 extends AbstractSourceSet {

        public TestSource2(String name) {
            super(name);
        }

        private class InnerSource extends AbstractSource {

            public InnerSource(String name) {
                super(name);
            }

            public Format[] getFormats() {
                return new Format[]{FORMAT};
            }

            public String getOtherPartyName() {
                return this.otherParty.getName();
            }

            @Override
            public void evolve(Buffer buffer, long timestamp) {
            }
        }

        @Override
        public AbstractSource createSource(MediaSink otherParty) {
            return new InnerSource("test");
        }


        @Override
        public void evolve(Buffer buffer, long timestamp) {
        }

        public Format[] getFormats() {
            return new Format[]{FORMAT};
        }

        @Override
        public void destroySource(AbstractSource source) {
        }
    }

    private class TestSink2 extends AbstractSinkSet {

        public TestSink2(String name) {
            super(name);
        }

        private class LocalSink extends AbstractSink {

            public LocalSink(String name) {
                super(name);
            }

            public Format[] getFormats() {
                return new Format[]{FORMAT};
            }

            public boolean isAcceptable(Format format) {
                return true;
            }

            @Override
            public void onMediaTransfer(Buffer buffer) {
            }
        }

        @Override
        public AbstractSink createSink(MediaSource otherParty) {
            return new LocalSink("inner.sink");
        }

        @Override
        public void onMediaTransfer(Buffer buffer) throws IOException {
        }

        public Format[] getFormats() {
            return new Format[]{FORMAT};
        }

        public boolean isAcceptable(Format format) {
            return true;
        }

        @Override
        public void destroySink(AbstractSink sink) {
        }
    }

    private class OutletImpl extends AbstractOutlet {

        private InnerSource source = new InnerSource("inner.source");
        
        public OutletImpl(String name) {
            super(name);
        }

        public void start() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void stop() {
        }

        public MediaSource getOutput() {
            return source;
        }

        private class InnerSource extends AbstractSource {

            public InnerSource(String name) {
                super(name);
            }

            public Format[] getFormats() {
                return new Format[]{FORMAT};
            }

            public String getOtherPartyName() {
                return this.otherParty.getName();
            }

            @Override
            public void evolve(Buffer buffer, long timestamp) {
            }
        }
    }
    
    private class InletImpl extends AbstractInlet {

        private LocalSink sink = new LocalSink("test");
        
        public InletImpl(String name) {
            super(name);
        }
        
        public void start() {
        }

        public void stop() {
        }

        public MediaSink getInput() {
            return sink;
        }

        private class LocalSink extends AbstractSink {

            public LocalSink(String name) {
                super(name);
            }

            public Format[] getFormats() {
                return new Format[]{FORMAT};
            }

            public boolean isAcceptable(Format format) {
                return true;
            }

            @Override
            public void onMediaTransfer(Buffer buffer) {
                
            }
        }
        
    }
}