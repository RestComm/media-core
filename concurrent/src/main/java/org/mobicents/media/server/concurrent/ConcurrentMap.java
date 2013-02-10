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

package org.mobicents.media.server.concurrent;

import java.util.Iterator;

/**
 *
 * @author oifa yulian
 */
public class ConcurrentMap<E> 
{	
	private MapNode head;
	private MapNode tail;
	
	private Integer availableSize=0;
	private Integer size=0;
	
	private Integer lastId=1;
	private Lock lock=new Lock();
	
	private MapSegment<E>[] segments=new MapSegment[256];
	
	public ConcurrentMap()
	{
		head=new MapNode(lastId++);
		tail=new MapNode(lastId++);
		
		head.next=tail;
		head.prev=tail;
		
		tail.next=head;
		tail.prev=head;
		
		for(int i=0;i<256;i++)
			segments[i]=new MapSegment<E>(this);
	}
	
	public E get(int key)
	{
		return segments[key & 0xFF].get(key);
	}
	
	public E remove(int key)
	{
		return segments[key & 0xFF].remove(key);
	}
	
	public E put(int key,E value)
	{
		return segments[key & 0xFF].put(key,value);
	}
	
	public E putIfAbscent(int key,E value)
	{
		return segments[key & 0xFF].putIfAbscent(key,value);
	}
	
	protected MapNode newNode(int key,Object value)
	{
		aquireAccess();
		
		MapNode result=null;
		if(availableSize==0)
		{
			result=new MapNode(lastId++);			
			
			result.next=head;
			result.prev=head.prev;
			
			head.prev=result;
			result.prev.next=result;						
		}
		else
		{
			result=head;
			head=head.next;
		
			//update counters
			availableSize--;			
		}
		
		size++;
		releaseAccess();
		
		result.key=key;
		result.value=value;
		
		return result;
	}
	
	protected void release(MapNode node)
	{
		aquireAccess();
		
		node.next.prev=node.prev;
		node.prev.next=node.next;
		
		head.next.prev=node;
		node.next=head.next;
		head.next=node;
		node.prev=head;
		
		//update counters
		availableSize++;
		size--;
		
		releaseAccess();
	}
	
	public boolean isEmpty()
	{
		return size==0;
	}
	
	public int size()
	{
		return size;
	}
	
	private void aquireAccess()
    {
    	try
    	{
    		lock.lock();
    	}
    	catch(InterruptedException e)
    	{
    		
    	}
    }
    
    private void releaseAccess()
    {
    	lock.unlock();    	
    }
	
    public Iterator<Integer> keys()
	{
		return new KeyIterator();		
	}
	
	public void resetKeyIterator(Iterator<Integer> iterator)
	{
		((KeyIterator)iterator).currNode=tail.next;
	}
	
	public Iterator<E> values()
	{
		return new ListIterator();		
	}
	
	public void resetIterator(Iterator<E> iterator)
	{
		((ListIterator)iterator).currNode=tail.next;
	}
	
	protected static class StorageCache
	{
		static ConcurrentCyclicFIFO<MapNode[]>[] cache=new ConcurrentCyclicFIFO[65536];
		
		static Lock lock=new Lock();
		
		static
		{
			cache[0]=new ConcurrentCyclicFIFO<MapNode[]>();
		}
		
		protected static MapNode[] get(int segment)
		{
			try
			{
				lock.lock();
			}
			catch(InterruptedException e)
			{
				
			}
			
			if(cache[segment]==null)
				cache[segment]=new ConcurrentCyclicFIFO<MapNode[]>();
				
			lock.unlock();
			
			MapNode[] result=cache[segment].poll();
			if(result==null)
				return new MapNode[(segment+1)*256];
			
			return result;
		}
		
		protected static void put(MapNode[] value)
		{
			//not required to check if segment is null since it was created in get
			cache[value.length/256].offer(value);
		}
	}		
	
	private class KeyIterator implements Iterator<Integer>
    {
    	private MapNode currNode;
    	
    	public KeyIterator()
    	{
    		this.currNode=tail.next;
    	}
    	
    	public boolean hasNext()
    	{
    		//when size is zero head may be equal to tail 
    		return size>0 && currNode.id!=head.id;
    	}
    	
    	public Integer next()
    	{
    		Integer result=currNode.key;
    		currNode=currNode.next;
    		return result;
    	}
    	
    	public void remove() throws UnsupportedOperationException
    	{
    		throw new UnsupportedOperationException();
    	}
    }
	
	private class ListIterator implements Iterator<E>
    {
    	private MapNode currNode;
    	public ListIterator()
    	{
    		this.currNode=tail.next;
    	}
    	
    	public boolean hasNext()
    	{
    		//when size is zero head may be equal to tail
    		return size>0 && currNode.id!=head.id;
    	}
    	
    	public E next()
    	{
    		E  result=(E)currNode.value;
    		currNode=currNode.next;
    		return result;
    	}
    	
    	public void remove() throws UnsupportedOperationException
    	{
    		throw new UnsupportedOperationException();
    	}
    }
}
