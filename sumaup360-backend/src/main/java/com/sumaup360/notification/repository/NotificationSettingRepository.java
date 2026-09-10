package com.sumaup360.notification.repository;

import com.sumaup360.notification.domain.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, String> {

    List<NotificationSetting> findAllByOrderByKeyAsc();
}
