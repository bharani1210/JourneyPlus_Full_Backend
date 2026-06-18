package com.journeyplus.event;

public class StatusChangeEvent {

    private final Long userId;
    private final String title;
    private final String message;
    // optional actor information: who performed the action (employee, manager, etc.)
    private final Long actorId;
    private final String actorName;

    public StatusChangeEvent(Long userId, String title, String message) {
        this(userId, title, message, null, null);
    }

    public StatusChangeEvent(Long userId, String title, String message, Long actorId, String actorName) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.actorId = actorId;
        this.actorName = actorName;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getActorName() {
        return actorName;
    }
}
