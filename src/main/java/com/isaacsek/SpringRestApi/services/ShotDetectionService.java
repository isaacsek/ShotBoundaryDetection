package com.isaacsek.SpringRestApi.services;

import org.springframework.stereotype.Service;

import com.isaacsek.SpringRestApi.interfaces.IShotDetectionService;

@Service
public class ShotDetectionService implements IShotDetectionService {

	@Override
	public int getBoundaries() {
		return 0;
	}

}
