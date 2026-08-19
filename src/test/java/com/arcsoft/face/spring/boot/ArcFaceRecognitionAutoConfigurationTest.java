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
import com.arcsoft.face.spring.boot.pool2.FaceEngineFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ArcFaceRecognitionAutoConfigurationTest {

    @Configuration
    @EnableConfigurationProperties(ArcFaceRecognitionProperties.class)
    static class TestConfiguration {
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ArcFaceRecognitionAutoConfiguration.class, TestConfiguration.class);

    @Test
    void autoConfigurationNotLoadedWhenDisabled() {
        this.contextRunner
                .withPropertyValues("arcface.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ArcFaceRecognitionAutoConfiguration.class);
                });
    }

    @Test
    void autoConfigurationLoadedWhenEnabled() {
        this.contextRunner
                .withPropertyValues("arcface.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ArcFaceRecognitionAutoConfiguration.class);
                    assertThat(context).hasSingleBean(ArcFaceRecognitionTemplate.class);
                    assertThat(context).hasSingleBean(ArcFaceRecognitionProperties.class);
                    assertThat(context).hasSingleBean(FaceEngineFactory.class);
                    assertThat(context).hasSingleBean(GenericObjectPool.class);
                });
    }

    @Test
    void faceEngineFactoryBeanIsConstructedFromProperties() {
        this.contextRunner
                .withPropertyValues(
                        "arcface.enabled=true",
                        "arcface.app-id=test-app",
                        "arcface.sdk-key=test-key",
                        "arcface.lib-path=/opt/lib"
                )
                .run(context -> {
                    FaceEngineFactory factory = context.getBean(FaceEngineFactory.class);
                    assertThat(factory).isNotNull();
                    ArcFaceRecognitionProperties properties =
                            context.getBean(ArcFaceRecognitionProperties.class);
                    assertThat(properties.getAppId()).isEqualTo("test-app");
                    assertThat(properties.getSdkKey()).isEqualTo("test-key");
                    assertThat(properties.getLibPath()).isEqualTo("/opt/lib");
                });
    }

    @Test
    void templateReceivesPropertiesAndPool() {
        this.contextRunner
                .withPropertyValues(
                        "arcface.enabled=true",
                        "arcface.app-id=test-app"
                )
                .run(context -> {
                    ArcFaceRecognitionTemplate template =
                            context.getBean(ArcFaceRecognitionTemplate.class);
                    assertThat(template).isNotNull();
                    assertThat(template.getProperties()).isNotNull();
                    assertThat(template.getProperties().getAppId()).isEqualTo("test-app");
                });
    }

    @Test
    void poolConfigIsExposed() {
        this.contextRunner
                .withPropertyValues(
                        "arcface.enabled=true",
                        "arcface.pool2.max-total=5",
                        "arcface.pool2.max-idle=3"
                )
                .run(context -> {
                    ArcFaceRecognitionProperties properties =
                            context.getBean(ArcFaceRecognitionProperties.class);
                    assertThat(properties.getPool2().getMaxTotal()).isEqualTo(5);
                    assertThat(properties.getPool2().getMaxIdle()).isEqualTo(3);
                });
    }

    @Test
    void faceEngineFactoryAndPoolAreBothRegistered() {
        this.contextRunner
                .withPropertyValues("arcface.enabled=true")
                .run(context -> {
                    FaceEngineFactory factory = context.getBean(FaceEngineFactory.class);
                    @SuppressWarnings("unchecked")
                    GenericObjectPool<FaceEngine> pool = (GenericObjectPool<FaceEngine>) context.getBean("faceEngineObjectPool");
                    assertThat(factory).isNotNull();
                    assertThat(pool).isNotNull();
                });
    }
}