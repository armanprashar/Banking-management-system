package com.bankmgmt.model;

/** Links an {@link User} with ADMIN role into the admins registry table. */
public class Admin extends AbstractEntity {

    private long userId;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }
}
