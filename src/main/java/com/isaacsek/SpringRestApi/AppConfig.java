package com.isaacsek.SpringRestApi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.isaacsek.SpringRestApi.interfaces.IShotDetectionService;
import com.isaacsek.SpringRestApi.services.ShotDetectionService;

@Configuration
public class AppConfig {
	
	@Bean
	public IShotDetectionService shotDetectionService() {
		return new ShotDetectionService();
	}
}
