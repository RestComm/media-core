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

package org.restcomm.media.rtp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.junit.Test;
import org.restcomm.media.rtp.CnameGenerator;

/**
 * Tests validity of generated CNAME to be used in RTP sessions.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class CnameGeneratorTest {

    private final static Logger log = LogManager.getLogger(CnameGeneratorTest.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public void after() {
        executor.shutdown();
    }

    @Test(timeout = 10)
    public void testCnameGeneratorSpeed() {
        CnameGenerator.generateCname();
    }

    @Test
    public void testCnameGenerator() {
        // given
        int minCnameSize = 98;
        int capacity = 10000;
        List<String> cnameList = new ArrayList<String>(capacity);

        for (int i = 0; i < capacity; i++) {
            // when
            long start = System.currentTimeMillis();
            String cname = CnameGenerator.generateCname();
            long time = System.currentTimeMillis() - start;

            // then
            // test minimum size
            assertEquals((minCnameSize / 8), Base64.decode(cname).length);
            // test uniqueness
            assertFalse(cnameList.contains(cname));
            cnameList.add(cname);
        }

        // cleanup
        cnameList.clear();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConcurrentCnameGenerator() throws InterruptedException, ExecutionException {
        // given
        int threads = Runtime.getRuntime().availableProcessors() * 2;
        Future<Set<String>>[] futures = new Future[threads];

        // when
        for (int i = 0; i < threads; i++) {
            Callable<Set<String>> worker = new GeneratorWorker("worker-" + i);
            Future<Set<String>> future = executor.submit(worker);
            futures[i] = future;
        }
        Thread.sleep(5000L);

        // then
        for (int i = 0; i < threads; i++) {
            Future<Set<String>> futureA = futures[i];
            for (int j = i + 1; j < futures.length; j++) {
                Future<Set<String>> futureB = futures[j];

                HashSet<String> intersection = new HashSet<String>(futureA.get());
                intersection.retainAll(futureB.get());
                assertTrue(intersection.isEmpty());
            }
        }
    }

    private class GeneratorWorker implements Callable<Set<String>> {

        private final String name;

        public GeneratorWorker(String name) {
            this.name = name;
        }

        @Override
        public Set<String> call() throws Exception {
            // given
            int minCnameSize = 98;
            int capacity = 10000;
            Set<String> cnameList = new HashSet<String>(capacity);

            for (int i = 0; i < capacity; i++) {
                // when
                long start = System.currentTimeMillis();
                String cname = CnameGenerator.generateCname();
                long time = System.currentTimeMillis() - start;

                if (log.isDebugEnabled()) {
                    log.debug(this.name + " took " + time + "ms to generate the cname " + cname);
                }

                // then
                // test minimum size
                assertEquals((minCnameSize / 8), Base64.decode(cname).length);
                // test uniqueness
                assertFalse(cnameList.contains(cname));
                cnameList.add(cname);
            }

            // cleanup
            return cnameList;
        }

    }

}
