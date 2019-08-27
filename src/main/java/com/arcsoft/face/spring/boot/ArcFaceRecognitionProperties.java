/*
 * Copyright (c) 2018, vindell (https://github.com/vindell).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.arcsoft.face.spring.boot;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.arcsoft.face.EngineConfiguration;
import com.arcsoft.face.FaceEngine;

@ConfigurationProperties(ArcFaceRecognitionProperties.PREFIX)
public class ArcFaceRecognitionProperties extends EngineConfiguration {

	public static final String PREFIX = "arcface";

	/**
	 * Enable Arcsoft Face Recognition.
	 */
	private boolean enabled = false;
	/**
	 * 开发者中心获取的 APP ID
	 */
	private String appId;
	/**
	 * 开发者中心获取的 SDK Key
	 */
	private String sdkKey;
	/**
	 * SDK 库存放地址
	 */
	private String libPath;
	/**
	 * 人脸识别引擎对象池配置
	 */
	private GenericObjectPoolConfig<FaceEngine> pool2 = new GenericObjectPoolConfig<FaceEngine>();
	

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getSdkKey() {
		return sdkKey;
	}

	public void setSdkKey(String sdkKey) {
		this.sdkKey = sdkKey;
	}

	public String getLibPath() {
		return libPath;
	}

	public void setLibPath(String libPath) {
		this.libPath = libPath;
	}

	public GenericObjectPoolConfig<FaceEngine> getPool2() {
		return pool2;
	}

	public void setPool2(GenericObjectPoolConfig<FaceEngine> pool2) {
		this.pool2 = pool2;
	}
	
}
