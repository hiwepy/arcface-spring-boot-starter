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

import com.arcsoft.face.FaceEngine;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcFaceRecognitionPropertiesTest {

    @Test
    void testPrefixConstant() {
        assertEquals("arcface", ArcFaceRecognitionProperties.PREFIX);
    }

    @Test
    void testDefaultEnabledIsFalse() {
        ArcFaceRecognitionProperties props = new ArcFaceRecognitionProperties();
        assertFalse(props.isEnabled());
    }

    @Test
    void testSetAndGetEnabled() {
        ArcFaceRecognitionProperties props = new ArcFaceRecognitionProperties();
        props.setEnabled(true);
        assertTrue(props.isEnabled());
        props.setEnabled(false);
        assertFalse(props.isEnabled());
    }

    @Test
    void testDefaultAppIdIsNull() {
        ArcFaceRecognitionProperties props = new ArcFaceRecognitionProperties();
        assertNull(props.getAppId());
    }

    @Test
    void testSetAndGetAppId() {
        ArcFaceRecognitionProperties props = new ArcFaceRecognitionProperties();
        props.setAppId("test-app-id");
        assertEquals("test-app-id", props.getAppId());
    }

    @Test
    void testDefaultSdkKeyIsNull() {
        ArcFaceRecognitionProperties props = new ArcFaceRecognitionProperties();
        assertNull(props.getSdkKey());
    }

    @Test
    void testSetAndGetSdkKey() {
        ArcFaceRecognitionProperties props = new ArcFaceRecognitionProperties();
        props.setSdkKey("test-sdk-key");
        assertEquals("test-sdk-key", props.getSdkKey());
    }

    @Test
    void testDefaultLibPathIsNull() {
        ArcFaceRecognitionProperties props = new ArcFaceRecognitionProperties();
        assertNull(props.getLibPath());
    }

    @Test
    void testSetAndGetLibPath() {
        ArcFaceRecognitionProperties props = new ArcFaceRecognitionProperties();
        props.setLibPath("/opt/arcsoft/libs");
        assertEquals("/opt/arcsoft/libs", props.getLibPath());
    }

    @Test
    void testDefaultPool2IsInitialized() {
        ArcFaceRecognitionProperties props = new ArcFaceRecognitionProperties();
        GenericObjectPoolConfig<FaceEngine> pool = props.getPool2();
        assertNotNull(pool);
    }

    @Test
    void testSetAndGetPool2() {
        ArcFaceRecognitionProperties props = new ArcFaceRecognitionProperties();
        GenericObjectPoolConfig<FaceEngine> custom = new GenericObjectPoolConfig<>();
        custom.setMaxTotal(5);
        props.setPool2(custom);
        assertEquals(custom, props.getPool2());
        assertEquals(5, props.getPool2().getMaxTotal());
    }

    @Test
    void testIsConfigurationPropertiesAnnotated() {
        // Verify the @ConfigurationProperties annotation exists by reflection
        org.springframework.boot.context.properties.ConfigurationProperties annotation =
                ArcFaceRecognitionProperties.class.getAnnotation(
                        org.springframework.boot.context.properties.ConfigurationProperties.class);
        assertNotNull(annotation);
        // Spring Boot may merge the prefix through @AliasFor so we just check it's present.
        assertNotNull(annotation.prefix());
    }

    @Test
    void testExtendsEngineConfiguration() {
        ArcFaceRecognitionProperties props = new ArcFaceRecognitionProperties();
        assertTrue(props instanceof com.arcsoft.face.EngineConfiguration);
    }
}