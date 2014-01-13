package org.mobicents.media.core.ice;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.ice4j.ice.Agent;
import org.ice4j.ice.Component;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.IceProcessingState;

/**
 * Default listener that captures changes on the state of an ICE process.
 * 
 * @author Henrique Rosa
 * 
 */
public class IceProcessingListener implements PropertyChangeListener {

	public void propertyChange(PropertyChangeEvent evt) {
		Object state = evt.getNewValue();

		if (state == null) {
			throw new NullPointerException("Undefined Ice Processing State");
		}
		if (!(state instanceof IceProcessingState)) {
			throw new IllegalArgumentException("Invalid Ice Processing State.");
		}

		// TODO Log state

		switch ((IceProcessingState) state) {
		case COMPLETED:
			// Agent agent = (Agent) evt.getSource();
			// List<IceMediaStream> streams = agent.getStreams();

			// for (IceMediaStream stream : streams) {
			// String streamName = stream.getName();
			// TODO Log stream name

			// List<Component> components = stream.getComponents();
			// for (Component component : components) {
			// String componentName = component.getName();
			// TODO Log component name
			// }
			// }
			break;
		case RUNNING:
			break;
		case WAITING:
			break;
		case TERMINATED:
		case FAILED:
			/*
			 * Demonstrate that Agent instances are to be explicitly prepared
			 * for garbage collection.
			 */
			((Agent) evt.getSource()).free();
			break;
		default:
			break;
		}

	}

}
