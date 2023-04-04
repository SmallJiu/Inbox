package cat.jiu.email.iface;

import cat.jiu.email.element.InboxTime;

public interface IInboxTime {
	long getAllTicks();
	
	long getDay();
	long getHour();
	long getMinute();
	long getSecond();
	long getTick();

	default boolean isDone() {
		return this.getAllTicks() <= 0;
	}

	void update();
	IInboxTime reset();

	default IInboxTime copy() {
		return new InboxTime(this.getAllTicks());
	}

}
