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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobicents.media.server.component.oob;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;

/**
 *
 * @author yulian oifa
 */
public class OOBTranslatorTest {

    private Clock clock;
    private Scheduler scheduler;

    private OOBSender sender1;
    private OOBSender sender2;
    private OOBSender sender3;

    private OOBReceiver receiver;
    private OOBMixer translator;

    private OOBComponent sender1Component;
    private OOBComponent sender2Component;
    private OOBComponent sender3Component;
    private OOBComponent receiverComponent;

    public OOBTranslatorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws IOException {
        clock = new DefaultClock();

        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        sender1 = new OOBSender(scheduler);
        sender2 = new OOBSender(scheduler);
        sender3 = new OOBSender(scheduler);
        receiver = new OOBReceiver("receiver", scheduler);

        sender1Component = new OOBComponent(1);
        sender1Component.addInput(sender1.getOOBInput());
        sender1Component.setReadable(true);
        sender1Component.setWritable(false);

        sender2Component = new OOBComponent(2);
        sender2Component.addInput(sender2.getOOBInput());
        sender2Component.setReadable(true);
        sender2Component.setWritable(false);

        sender3Component = new OOBComponent(3);
        sender3Component.addInput(sender3.getOOBInput());
        sender3Component.setReadable(true);
        sender3Component.setWritable(false);

        receiverComponent = new OOBComponent(4);
        receiverComponent.addOutput(receiver.getOOBOutput());
        receiverComponent.setReadable(false);
        receiverComponent.setWritable(true);

        translator = new OOBMixer(scheduler);
        translator.addComponent(sender1Component);
        translator.addComponent(sender2Component);
        translator.addComponent(sender3Component);
        translator.addComponent(receiverComponent);
    }

    @After
    public void tearDown() {
        scheduler.stop();
    }

    @Test
    public void testTranslate() throws InterruptedException {
        sender1.activate();
        translator.start();
        receiver.activate();
        Thread.sleep(1200);

        sender2.activate();
        Thread.sleep(1200);

        sender3.activate();
        Thread.sleep(1200);

        translator.stop();
        sender1.deactivate();
        sender2.deactivate();
        sender3.deactivate();
        receiver.deactivate();

        System.out.println("mix execution count: " + translator.getExecutionCount());

        int res = receiver.getPacketsCount();
        System.out.println("Received packets count:" + res);
        assertEquals(150, res, 5);
    }

    @Test
    public void testTranslateFailure() throws InterruptedException {
        int N = 5;// 100;
        for (int i = 0; i < N; i++) {
            System.out.println("Test # " + i);
            testTranslate();
        }
    }

    @Test
    public void testRecycle() throws InterruptedException {
        testTranslate();

        translator.removeComponent(sender1Component);
        translator.addComponent(sender1Component);

        testTranslate();
    }
}