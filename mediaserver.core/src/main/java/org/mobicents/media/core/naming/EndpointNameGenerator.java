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

package org.mobicents.media.core.naming;

import java.util.ArrayList;

/**
 * Generates concrete names for endpoint using specified pattern.
 *
 * The local endpoint name is case-insensitive.  The syntax of the local
 * endpoint name is hierarchical, where the least specific component of
 * the name is the leftmost term, and the most specific component is the
 * rightmost term.  The precise syntax depends on the type of endpoint
 * being named and MAY start with a term that identifies the endpoint
 * type.
 *
 * Patterns follows to the name rules but allows to underspecify the individual
 * terms as numeric range. The range syntax is similar to regular expression
 * symbolic class syntax.
 *
 * Example: mobicents/aap/[1..100] 
 * 
 * @author yulian oifa
 */
public class EndpointNameGenerator {

    //individual terms of the name
    private ArrayList<Term> terms = new ArrayList<Term>();
    
    /**
     * Modifies name pattern.
     *
     * @param pattern the new pattern value
     */
    public void setPattern(String pattern) {
        String[] parts = pattern.split("/");
        for (String part: parts) {
            part = part.trim();
            
            if (part.length() == 0) {
                continue;
            }

            Term term = part.startsWith("[") ? new NumericRange(part) : new Term(part);
            if (terms.size() > 0) {
                terms.get(terms.size() - 1).setChild(term);
            }
            terms.add(term);
        }
    }

    /**
     * Indicates is it possible to generate more names.
     *
     * @return true if more names can be generated.
     */
    public boolean hasMore() {
        return terms.size() > 0 && terms.get(0).hasMore();
    }

    /**
     * Next generated name.
     *
     * @return the next generated name of endpoint.
     */
    public String next() {
        return terms.get(0).next();
    }

    private class Term {
        protected String term;
        private boolean hasMore = true;

        protected Term child;

        public Term(String term) {
            this.term = term;
        }

        public void reset() {
            hasMore = true;
        }

        public void setChild(Term child) {
            this.child = child;
        }
        
        public boolean hasMore() {
            return child != null ? child.hasMore() : hasMore;
        }

        public String next() {
            if (child == null) hasMore = false;
            return child != null? term + "/" + child.next() : term;
        }
    }

    private class NumericRange extends Term {

        private int low, high;
        private int value;

        public NumericRange(String term) {
            super(term);
            term = term.substring(1, term.length() - 1);
            term = term.replaceAll("]", "");

            String tokens[] = term.split("\\.\\.");
            low = Integer.parseInt(tokens[0]);
            high = Integer.parseInt(tokens[1]);
            value = low;
        }

        @Override
        public void reset() {
            term = term.substring(1, term.length() - 1);
            term = term.replaceAll("]", "");

            String tokens[] = term.split("\\.\\.");
            low = Integer.parseInt(tokens[0]);
            high = Integer.parseInt(tokens[1]);
            value = low;
        }

        @Override
        public boolean hasMore() {
            return child == null ? value <= high : value < high || child.hasMore();
        }

        @Override
        public String next() {
            if (child != null && !child.hasMore()) {
                value++;
                child.reset();
            }

            String s = child != null ? Integer.toString(value) + "/" + child.next() : Integer.toString(value);
            if (child == null) value++;

            return s;
        }
    }
}
