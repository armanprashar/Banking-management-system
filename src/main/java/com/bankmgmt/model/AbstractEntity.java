package com.bankmgmt.model;

/**
 * Base type for persisted domain entities (inheritance / shared identity).
 */
public abstract class AbstractEntity {

    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
