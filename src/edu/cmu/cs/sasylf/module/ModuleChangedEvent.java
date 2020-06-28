package edu.cmu.cs.sasylf.module;

/**
 * Represents a change in a module id and
 * the type of change that occurred.
 */
public class ModuleChangedEvent {
	private ModuleId id;
	private EventType type;

	public enum EventType {
		CHANGED, ADDED, REMOVED
	}

	public ModuleChangedEvent(ModuleId id, EventType type) {
		this.id = id;
		this.type = type;
	}
	
	/**
	 * Get the event's module id.
	 * @return module id associated with this event
	 */
	public ModuleId getModuleId() { return id; }
	
	/**
	 * Get the type of the event.
	 * @return the type of event that occurred
	 */
	public EventType getEventType() { return type; }
	
	@Override
	public String toString() {
		return type + " module id " + id;
	}
}
