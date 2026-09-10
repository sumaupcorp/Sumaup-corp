package com.sumaup360.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas programadas del backend: poller de campanas de push
 * (CampaignService) y recordatorios automaticos diarios (AutoReminderJob).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
