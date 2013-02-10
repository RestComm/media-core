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

/**
 *
 * @author oifa yulian
 */

public class MapSegment<E> 
{
	private int high=0;
	private int low=0;
	private int size=0;
	private int medium=0; 
	
	private MapNode[] storage=new MapNode[256];
	
	private int segment=0;
	
	private Lock lock=new Lock();
	
	private ConcurrentMap parentMap;
	
	public MapSegment(ConcurrentMap parent)
	{
		this.parentMap=parent;
	}
	
	protected E get(int key)
	{
		aquireAccess();
		
		E result=null;
		
		if(size==0)
		{
			releaseAccess();				
			return result;
		}
		
		low=0;
		high=size-1;
		medium=high/2;						
		
		while(low!=high)
		{
			if(storage[medium].key==key)
			{
				result=(E)storage[medium].value;
				releaseAccess();
				return result;
			}
			else if(storage[medium].key<key)
			{
				if(low==medium)
				{
					//possible only if medium=low and low=high-1;
					//since medium key is lower then key then only high
					//element may be the ony
					if(storage[high].key==key)
						result=(E)storage[high].value;
					
					releaseAccess();
					
					return result;
				}
				else
				{
					low=medium;
					medium=(high+low)/2;
				}
			}
			else 
			{
				high=medium;
				medium=(high+low)/2;
			}
		}
		
		if(storage[medium].key==key)
			result=(E)storage[medium].value;
		
		releaseAccess();
		
		return result;
	}
	
	protected E remove(int key)
	{
		aquireAccess();
		
		E result=null;
		
		if(size==0)
		{
			releaseAccess();				
			return result;
		}
		
		low=0;
		high=size-1;
		medium=high/2;
		
		while(low!=high)
		{
			if(storage[medium].key==key)
			{
				result=(E)storage[medium].value;
				
				if(medium<size)
					System.arraycopy(storage, medium+1, storage, medium, size-medium-1);
				
				size--;
				releaseAccess();
				
				parentMap.release(storage[medium]);								
				return result;
			}
			else if(storage[medium].key<key)
			{
				if(low==medium)
				{
					//possible only if medium=low and low=high-1;
					//since medium key is lower then key then only high
					//element may be the ony
					if(storage[high].key==key)
					{
						result=(E)storage[high].value;
						
						if(high<size)
							System.arraycopy(storage, high+1, storage, high, size-high-1);
						
						size--;	
						releaseAccess();
						parentMap.release(storage[high]);
					}
					else
						releaseAccess();
					
					return result;
				}
				else
				{
					low=medium;
					medium=(high+low)/2;
				}
			}
			else 
			{
				high=medium;
				medium=(high+low)/2;
			}
		}
		
		if(storage[medium].key==key)
		{
			result=(E)storage[medium].value;
			if(medium<size)
				System.arraycopy(storage, medium+1, storage, medium, size-medium-1);
			
			size--;
			releaseAccess();
			parentMap.release(storage[medium]);
		}
		else
			releaseAccess();
		
		return result;
	}
			
	protected E put(int key,E value)
	{
		aquireAccess();
		
		E result=null;
		
		if(size==0)
		{
			storage[0]=parentMap.newNode(key,value);
			size++;
			
			releaseAccess();				
			return result;
		}
		
		low=0;
		high=size-1;
		medium=high/2;
		
		while(low!=high)
		{
			if(storage[medium].key==key)
			{
				result=(E)storage[medium].value;
				storage[medium].value=value;
				
				releaseAccess();
				
				return result;
			}
			else if(storage[medium].key<key)
			{
				if(low==medium)
				{
					//possible only if medium=low and low=high-1;
					//since medium key is lower then key then only high
					//element may be the ony
					if(storage[high].key==key)
					{
						result=(E)storage[high].value;
						storage[high].value=value;
					}
					else
					{
						if(storage.length==size)
						{
							segment++;
							MapNode[] newStorage=ConcurrentMap.StorageCache.get(segment);
							System.arraycopy(storage, 0, newStorage, 0, high);
							System.arraycopy(storage, high, newStorage, high+1, size-high);
							newStorage[high]=parentMap.newNode(key,value);
							size++;
							
							ConcurrentMap.StorageCache.put(storage);
							storage=newStorage;
						}
						else
						{
							System.arraycopy(storage, high, storage, high+1, size-high);				
							storage[high]=parentMap.newNode(key,value);
							size++;
						}						
					}
					
					releaseAccess();
					
					return result;
				}
				else
				{
					low=medium;
					medium=(high+low)/2;
				}
			}
			else 
			{
				high=medium;
				medium=(high+low)/2;
			}
		}
		
		if(storage[medium].key==key)
		{
			result=(E)storage[medium].value;
			storage[medium].value=value;								
		}
		else
		{
			if(storage.length==size)
			{
				segment++;
				MapNode[] newStorage=ConcurrentMap.StorageCache.get(segment);
				System.arraycopy(storage, 0, newStorage, 0, medium);
				System.arraycopy(storage, medium, newStorage, medium+1, size-medium);
				newStorage[medium]=parentMap.newNode(key,value);
				size++;
				
				ConcurrentMap.StorageCache.put(storage);
				storage=newStorage;
			}
			else
			{
				System.arraycopy(storage, medium, storage, medium+1, size-medium);				
				storage[medium]=parentMap.newNode(key,value);
				size++;
			}
		}
		
		releaseAccess();
		
		return result;
	}
	
	protected E putIfAbscent(int key,E value)
	{
		aquireAccess();
		
		E result=null;
		
		if(size==0)
		{
			storage[0]=parentMap.newNode(key,value);
			size++;
			
			releaseAccess();				
			return value;
		}
		
		low=0;
		high=size-1;
		medium=high/2;
		
		while(low!=high)
		{
			if(storage[medium].key==key)
			{
				result=(E)storage[medium].value;
				
				releaseAccess();
				
				return result;
			}
			else if(storage[medium].key<key)
			{
				if(low==medium)
				{
					//possible only if medium=low and low=high-1;
					//since medium key is lower then key then only high
					//element may be the ony
					if(storage[high].key==key)
						result=(E)storage[high].value;						
					else
					{
						if(storage.length==size)
						{
							segment++;
							MapNode[] newStorage=ConcurrentMap.StorageCache.get(segment);
							System.arraycopy(storage, 0, newStorage, 0, high);
							System.arraycopy(storage, high, newStorage, high+1, size-high);
							newStorage[high]=parentMap.newNode(key,value);
							size++;
							
							ConcurrentMap.StorageCache.put(storage);
							storage=newStorage;
						}
						else
						{
							System.arraycopy(storage, high, storage, high+1, size-high);				
							storage[high]=parentMap.newNode(key,value);
							size++;
						}	
						
						result=value;
					}
					
					releaseAccess();
					
					return result;
				}
				else
				{
					low=medium;
					medium=(high+low)/2;
				}
			}
			else 
			{
				high=medium;
				medium=(high+low)/2;
			}
		}
		
		if(storage[medium].key==key)
			result=(E)storage[medium].value;
		else
		{
			if(storage.length==size)
			{
				segment++;
				MapNode[] newStorage=ConcurrentMap.StorageCache.get(segment);
				System.arraycopy(storage, 0, newStorage, 0, medium);
				System.arraycopy(storage, medium, newStorage, medium+1, size-medium);
				newStorage[medium]=parentMap.newNode(key,value);
				size++;
				
				ConcurrentMap.StorageCache.put(storage);
				storage=newStorage;
			}
			else
			{
				System.arraycopy(storage, medium, storage, medium+1, size-medium);				
				storage[medium]=parentMap.newNode(key,value);
				size++;
			}
		}
		
		releaseAccess();
		
		if(result==null)
			result=value;
		
		return result;
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
}
