package com.bankmgmt.dao;

import com.bankmgmt.model.ActivityLog;

import java.util.List;

public interface IActivityLogDAO {

    void insert(Long userId, String action, String details);

    List<ActivityLog> recent(int limit);

    List<ActivityLog> filter(String actionContains);
}
