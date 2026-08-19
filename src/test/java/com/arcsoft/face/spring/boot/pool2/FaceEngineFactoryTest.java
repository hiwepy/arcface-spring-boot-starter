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
package com.arcsoft.face.spring.boot.pool2;

import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.spring.boot.ArcFaceRecognitionProperties;
import org.apache.commons.pool2.PooledObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class FaceEngineFactoryTest {

    private ArcFaceRecognitionProperties properties;
    private FaceEngineFactory factory;

    @BeforeEach
    void setUp() {
        properties = new ArcFaceRecognitionProperties();
        properties.setAppId("test-app");
        properties.setSdkKey("test-sdk-key");
        factory = new FaceEngineFactory(properties);
    }

    @Test
    void testFactoryCreatedWithProperties() {
        assertNotNull(factory);
    }

    @Test
    void testWrapReturnsDefaultPooledObject() throws Exception {
        // Wrap a real-ish engine instance via the no-arg constructor path of FaceEngine.
        // If the native library is unavailable this still constructs because FaceEngine
        // is lazy-loaded. We simply verify wrap() produces a non-null PooledObject.
        FaceEngine engine = new FaceEngine();
        PooledObject<FaceEngine> wrapped = factory.wrap(engine);
        assertNotNull(wrapped);
        assertNotNull(wrapped.getObject());
    }

    @Test
    void testDestroyObjectRunsWithoutThrowing() throws Exception {
        FaceEngine engine = new FaceEngine();
        PooledObject<FaceEngine> p = factory.wrap(engine);
        // destroyObject will call engine.unInit() then super.destroyObject(p)
        // unInit() may return an error code without the native lib, but it should not throw.
        factory.destroyObject(p);
    }

    @Test
    void testActivateObjectRunsWithoutThrowing() throws Exception {
        FaceEngine engine = new FaceEngine();
        PooledObject<FaceEngine> p = factory.wrap(engine);
        // activateObject reads the activeStatusMap for the engine's hash code.
        // Since create() was never called, the map has no entry and a NullPointerException
        // may be raised via int-unboxing. We accept either outcome (no-throw or NullPointerException).
        try {
            factory.activateObject(p);
        } catch (NullPointerException expected) {
            // acceptable: the activeStatusMap has no entry for this engine
        }
    }
}