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

package org.restcomm.media.spi.format;

import java.util.ArrayList;

/**
 * Represents collection of formats.
 *
 * @author kulikov
 */
public class Formats {
    //the default size of format list
    public final static int DEFAULT_SIZE = 15;

    //backing array
    private ArrayList<Format> list;

    /**
     * Creates new collection with default size;
     */
    public Formats() {
        list = new ArrayList<Format>(DEFAULT_SIZE);
    }

    /**
     * Creates collection with specified size.
     *
     * @param size the size of the collection.
     */
    public Formats(int size) {
        list = new ArrayList<Format>(size);
    }

    /**
     * Adds specified format to this collection.
     *
     * @param format the format object to be added
     */
    public void add(Format format) {
        list.add(format);
    }

    /**
     * Adds multiple formats.
     * 
     * @param other the collection of formats to be added to this collection.
     */
    public void addAll(Formats other) {
        this.list.addAll(other.list);
    }

    /**
     * Removes specified format from this collection.
     *
     * @param format the format object to be removed.
     */
    public void remove(Format format) {
        list.remove(format);
    }

    /**
     * Gets the collection element.
     * 
     * @param i the position of the element in collection
     * @return format descriptor.
     */
    public Format get(int i) {
        return list.get(i);
    }

    /**
     * Checks that collection has specified format.
     *
     * @param format the format to be checked
     * @return true if collection contains specified format and false otherwise
     */
    public boolean contains(Format format) {
        for (Format f : list) {
            if (f.matches(format)) return true;
        }
        return false;
    }
    
    /**
     * Gets the number of formats contained inside collection.
     * 
     * @return the number of objects in the collection.
     */
    public int size() {
        return list.size();
    }

    /**
     * Checks collection's size.
     *
     * @return true if collection is empty and its size equals to zero.
     */
    public boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     * Removes all formats from the collection.
     */
    public void clean() {
        list.clear();
    }
    
    /**
     * Find the intersection between this collection and other
     * 
     * @param other the other collection
     * @param intersection the resulting collection.
     */
    public void intersection(Formats other, Formats intersection) {
        intersection.list.clear();
        for (Format f1 : list) {
            for (Format f2 : other.list) {
                if (f1.matches(f2)) intersection.list.add(f2);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("Formats{");
        for (int i = 0; i < list.size(); i++) {
            buff.append(list.get(i));
            if (i != list.size() - 1) buff.append(",");
        }
        buff.append("}");
        return buff.toString();
    }
}
