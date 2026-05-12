package com.bankmgmt.dao;

public interface IAdminDAO {

    boolean isAdmin(long userId);

    void linkAdmin(long userId);
}
