package com.arcsoft.face.spring.boot;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.spring.boot.pool2.FaceEngineFactory;

@Configuration
@ConditionalOnProperty(prefix = ArcFaceRecognitionProperties.PREFIX, value = "enabled", havingValue = "true")
@EnableConfigurationProperties({ ArcFaceRecognitionProperties.class })
public class ArcFaceRecognitionAutoConfiguration {

	@Bean
	public FaceEngineFactory faceEngineFactory(ArcFaceRecognitionProperties properties) {
		return new FaceEngineFactory(properties);
	}

	@Bean
	public GenericObjectPool<FaceEngine> faceEngineObjectPool(FaceEngineFactory faceEngineFactory) {
		return new GenericObjectPool<FaceEngine>(faceEngineFactory);
	}

	@Bean
	public ArcFaceRecognitionTemplate arcFaceRecognitionTemplate(ArcFaceRecognitionProperties properties,
			GenericObjectPool<FaceEngine> faceEngineObjectPool) {
		return new ArcFaceRecognitionTemplate(properties, faceEngineObjectPool);
	}

}
