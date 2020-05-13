package edu.cmu.cs.sasylf.module;

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
	
	public ModuleId getModuleId() { return id; }
	
	public EventType getEventType() { return type; }
	
	@Override
	public String toString() {
		return "moduleId: " + id + ", event type: " + type;
	}
}
