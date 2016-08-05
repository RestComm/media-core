/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.mobicents.media.control.mgcp.util.collections;

import com.google.common.base.Function;

/**
 * Static collection of Functions that help convert between two types of data.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ValueTransformers {

    public static final Function<String, Integer> STRING_TO_INTEGER = new IntegerTransformer();
    public static final Function<String, Integer> STRING_TO_INTEGER_BASE16 = new IntegerBase16Transformer();
    public static final Function<String, Long> STRING_TO_LONG = new LongTransformer();
    public static final Function<String, Boolean> STRING_TO_BOOLEAN = new BooleanTransformer();

    private ValueTransformers() {
        super();
    }

    private static final class IntegerTransformer implements Function<String, Integer> {

        @Override
        public Integer apply(String input) {
            if (input == null || input.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                return null;
            }
        }

    }

    private static final class IntegerBase16Transformer implements Function<String, Integer> {
        
        @Override
        public Integer apply(String input) {
            if (input == null || input.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(input, 16);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
    }

    private static final class LongTransformer implements Function<String, Long> {

        @Override
        public Long apply(String input) {
            if (input == null || input.isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                return null;
            }
        }

    }

    private static final class BooleanTransformer implements Function<String, Boolean> {

        @Override
        public Boolean apply(String input) {
            if (input == null || input.isEmpty()) {
                return null;
            }
            return Boolean.parseBoolean(input);
        }

    }

}
