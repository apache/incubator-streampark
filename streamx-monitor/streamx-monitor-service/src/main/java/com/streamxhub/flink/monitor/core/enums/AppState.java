package com.streamxhub.flink.monitor.core.enums;

import lombok.Getter;

@Getter
public enum AppState {

    /** Application which is currently deploying. */
    CREATED(0),

    /** Application which is currently deploying. */
    DEPLOYING(1),

    /** Application which was just created. */
    NEW(2),

    /** Application which is being saved. */
    NEW_SAVING(3),

    /** Application which has been submitted. */
    SUBMITTED(4),

    /** Application has been accepted by the scheduler */
    ACCEPTED(5),

    /** Application which is currently running. */
    RUNNING(6),

    /** Application which finished successfully. */
    FINISHED(7),

    /** Application which failed. */
    FAILED(8),

    /** Application which was terminated by a user or admin. */
    KILLED(9);

    int value;
    AppState(int value) {
        this.value = value;
    }

}
