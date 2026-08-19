/*
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
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

/**
 * <p>Configuration properties for ArcFaceRecognition.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
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
	

	/** Getter for enabled */
	public boolean isEnabled() {
		return enabled;
	}

	/** Setter for enabled */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/** Getter for app id */
	public String getAppId() {
		return appId;
	}

	/** Setter for app id */
	public void setAppId(String appId) {
		this.appId = appId;
	}

	/** Getter for sdk key */
	public String getSdkKey() {
		return sdkKey;
	}

	/** Setter for sdk key */
	public void setSdkKey(String sdkKey) {
		this.sdkKey = sdkKey;
	}

	/** Getter for lib path */
	public String getLibPath() {
		return libPath;
	}

	/** Setter for lib path */
	public void setLibPath(String libPath) {
		this.libPath = libPath;
	}

	/** Getter for pool2 */
	public GenericObjectPoolConfig<FaceEngine> getPool2() {
		return pool2;
	}

	/** Setter for pool2 */
	public void setPool2(GenericObjectPoolConfig<FaceEngine> pool2) {
		this.pool2 = pool2;
	}
	
}
