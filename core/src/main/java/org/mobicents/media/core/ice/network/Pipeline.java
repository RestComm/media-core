package org.mobicents.media.core.ice.network;

/**
 * 
 * @author Henrique Rosa
 * 
 * @param <T>
 *            The type of components that compose the pipeline.
 */
public interface Pipeline<T extends Expiring> {

	T getCurrent();

	boolean hasNext();

	boolean hasPrevious();

	void add(T element);

}
