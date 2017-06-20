/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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
        
package org.restcomm.media.rtp;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SsrcGeneratorTest {
    
    @Test
    public void testUniquenessAndSize() {
        // given
        final int iterations = 10000;
        final List<Long> ssrcs = new ArrayList<Long>(iterations);
        final SsrcGenerator ssrcGenerator = new SsrcGenerator();

        // when
        for (int i = 0; i < iterations; i++) {
            long ssrc = ssrcGenerator.generateSsrc();
            int size = Long.SIZE - Long.numberOfLeadingZeros(ssrc);
            
            // then
            Assert.assertFalse(ssrcs.contains(Long.valueOf(ssrc)));
            Assert.assertTrue(SsrcGenerator.MAX_SIZE >= size);

            ssrcs.add(ssrc);
        }
    }

}
