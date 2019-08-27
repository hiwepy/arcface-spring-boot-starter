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
package com.arcsoft.face.spring.boot.pool2;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.util.StringUtils;

import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.enums.ErrorInfo;
import com.arcsoft.face.spring.boot.ArcFaceRecognitionProperties;

/**
 * TODO
 * @author 		： <a href="https://github.com/vindell">wandl</a>
 */
public class FaceEngineFactory extends BasePooledObjectFactory<FaceEngine> {

	private final ArcFaceRecognitionProperties properties;
	
    public FaceEngineFactory(ArcFaceRecognitionProperties properties) {
        this.properties = properties;
    }
    
	@Override
	public FaceEngine create() throws Exception {
        FaceEngine faceEngine = StringUtils.hasText(properties.getLibPath()) ? new FaceEngine(properties.getLibPath()) : new FaceEngine() ;
        //激活引擎
        int activeCode = faceEngine.activeOnline(properties.getAppId(), properties.getSdkKey());
        System.out.println("faceEngineActiveCode:" + activeCode + "==========================");
        //初始化引擎
        int initCode = faceEngine.init(properties);
        if (initCode != ErrorInfo.MOK.getValue()) {
            System.out.println("初始化引擎失败");
        }
        System.out.println("faceEngineInitCode:" + initCode + "==========================");
        return faceEngine;
	}
	
	@Override
    public PooledObject<FaceEngine> wrap(FaceEngine faceEngine) {
        return new DefaultPooledObject<>(faceEngine);
    }
	
    @Override
    public void destroyObject(PooledObject<FaceEngine> p) throws Exception {
        FaceEngine faceEngine = p.getObject();
        int unInitCode = faceEngine.unInit();
        System.out.println("faceEngineUnInitCode:" + unInitCode + "==========================");
        super.destroyObject(p);
    }
	
}
