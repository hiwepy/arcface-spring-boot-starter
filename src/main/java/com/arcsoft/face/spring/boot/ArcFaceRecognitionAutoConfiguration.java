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
/**\n * Auto-configuration for ArcFace face recognition SDK.\n *\n * @author <a href="https://github.com/loong10k">Loong Wan</a>\n * @since 1.0.0\n */
public class ArcFaceRecognitionAutoConfiguration {

	/**
	 * <p>face engine factory.</p>
	 * @param properties the properties
	 * @return the faceEngineFactory return value
	 */
	@Bean
	public FaceEngineFactory faceEngineFactory(ArcFaceRecognitionProperties properties) {
		return new FaceEngineFactory(properties);
	}

	/**
	 * <p>face engine object pool.</p>
	 * @param faceEngineFactory the face engine factory
	 * @param properties the properties
	 * @return the faceEngineObjectPool return value
	 */
	@Bean
	public GenericObjectPool<FaceEngine> faceEngineObjectPool(FaceEngineFactory faceEngineFactory, ArcFaceRecognitionProperties properties) {
		return new GenericObjectPool<FaceEngine>(faceEngineFactory, properties.getPool2());
	}

	/**
	 * <p>arc face recognition template.</p>
	 * @param properties the properties
	 * @param faceEngineObjectPool the face engine object pool
	 * @return the arcFaceRecognitionTemplate return value
	 */
	@Bean
	public ArcFaceRecognitionTemplate arcFaceRecognitionTemplate(ArcFaceRecognitionProperties properties,
			GenericObjectPool<FaceEngine> faceEngineObjectPool) {
		return new ArcFaceRecognitionTemplate(properties, faceEngineObjectPool);
	}

}
