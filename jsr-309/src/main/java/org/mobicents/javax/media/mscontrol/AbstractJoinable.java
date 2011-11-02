package org.mobicents.javax.media.mscontrol;

import java.io.Serializable;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.join.Joinable;

/**
 * 
 * @author amit bhayani
 * 
 */
public abstract class AbstractJoinable implements Joinable {

	protected AbstractJoinable other = null;
	protected Joinable[] joineesArray = new Joinable[1];
	protected Direction direction = null;

	public Joinable[] getJoinees() throws MsControlException {
		return joineesArray;
	}

	public Joinable[] getJoinees(Direction direction) throws MsControlException {
		Direction joineeDirection = this.other.getDirection();
		if (joineeDirection == direction || joineeDirection == Direction.DUPLEX) {
			return joineesArray;
		}

		return null;
	}

	public void join(Direction direction, Joinable other) throws MsControlException {
		if (this.other == null) {
			this.direction = direction;
			this.other = (AbstractJoinable) other;
			this.joineesArray[0] = other;

			if (direction == Direction.RECV) {
				this.other.direction = Direction.SEND;

				// other.join(Direction.SEND, this);
			} else if (direction == Direction.SEND) {
				this.other.direction = Direction.RECV;
				// other.join(Direction.RECV, this);
			} else {
				this.other.direction = Direction.DUPLEX;
				// other.join(Direction.DUPLEX, this);
			}
			this.other.joineesArray[0] = this;
			this.other.other = this;
		} else {
			throw new MsControlException("This Joinable is already joined to other with Direction " + this.direction
					+ " Call unjoin first and then join again");
		}

	}

	public void joinInitiate(Direction direction, Joinable other, Serializable context) throws MsControlException {

	}

	public void unjoin(Joinable other) throws MsControlException {
		if (this.other != null) {
			
			this.other.direction = null;
			this.other.joineesArray[0] = null;
			this.other.other = null;
			
			this.direction = null;
			this.other = null;
			this.joineesArray[0] = null;
		}
		

	}

	public void unjoinInitiate(Joinable other, Serializable context) throws MsControlException {

	}

	public Direction getDirection() {
		return this.direction;
	}

}
