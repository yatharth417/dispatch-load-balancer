package com.assignment.dispatch.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Priority {
    HIGH(1),
    MEDIUM(2),
    LOW(3);

    private final int rank;

    Priority(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    @JsonCreator
    public static Priority fromString(String value) {
        if (value == null) {
            return null;
        }
        return Priority.valueOf(value.trim().toUpperCase());
    }
}