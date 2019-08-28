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

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.pool2.impl.GenericObjectPool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.Face3DAngle;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.FaceSimilar;
import com.arcsoft.face.FunctionConfiguration;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.IrLivenessInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.enums.ErrorInfo;
import com.arcsoft.face.toolkit.ImageFactory;
import com.arcsoft.face.toolkit.ImageInfo;
import com.google.common.collect.Lists;

/**
 * 基于虹软Arcface实现的人脸识别通用模板对象
 * @author ： <a href="https://github.com/vindell">wandl</a>
 */
public class ArcFaceRecognitionTemplate {

	private ArcFaceRecognitionProperties properties;
	private GenericObjectPool<FaceEngine> faceEngineObjectPool;

	public ArcFaceRecognitionTemplate(ArcFaceRecognitionProperties properties,
			GenericObjectPool<FaceEngine> faceEngineObjectPool) {
		this.properties = properties;
		this.faceEngineObjectPool = faceEngineObjectPool;
	}
	
	/**
	 * 不同的控制度下所对应的活体控制阈值，如果检测出来的活体分数小于控制阈值，则会返回错误信息。 LOW 0.05 NORMAL 0.3 HIGH 0.9
	 */
	protected int setLivenessParam(FaceEngine faceEngine, FaceLiveness liveness, JSONObject result) {
		switch (liveness) {
			case LOW: {
				// 设置活体检测参数
				return faceEngine.setLivenessParam(0.05f, 0.05f);
			}
			case NORMAL: {
				// 设置活体检测参数
				return faceEngine.setLivenessParam(0.3f, 0.3f);
			}
			case HIGH: {
				// 设置活体检测参数
				return faceEngine.setLivenessParam(0.9f, 0.9f);
			}
			default: {
				return 0;
			}
		}
	}

	/**
	 * RGB活体、年龄、性别、三维角度检测
	 * 
	 * @param imageBytes 输入的图像数据
	 * @author ： <a href="https://github.com/vindell">wandl</a>
	 * @return
	 */
	public JSONObject detect(byte[] imageBytes) {
		return detect(ImageFactory.getRGBData(imageBytes), FaceLiveness.NONE);
	}

	/**
	 * RGB活体、年龄、性别、三维角度检测
	 * 
	 * @param imageBytes 输入的图像数据
	 * @author ： <a href="https://github.com/vindell">wandl</a>
	 * @return
	 */
	public JSONObject detect(byte[] imageBytes, FaceLiveness liveness) {
		return detect(ImageFactory.getRGBData(imageBytes), liveness);
	}

	/**
	 * RGB活体、年龄、性别、三维角度检测
	 * 
	 * @param imageBytes 输入的图像数据
	 * @author ： <a href="https://github.com/vindell">wandl</a>
	 * @return
	 */
	public JSONObject detect(ImageInfo imageInfo) {
		return detect(imageInfo, FaceLiveness.NONE);
	}

	/**
	 * RGB活体、年龄、性别、三维角度检测
	 * 
	 * @param imageInfo 输入的图像信息
	 * @author ： <a href="https://github.com/vindell">wandl</a>
	 * @return
	 */
	public JSONObject detect(ImageInfo imageInfo, FaceLiveness liveness) {

		JSONObject result = new JSONObject();
		FaceEngine faceEngine = null;

		try {

			// 获取引擎对象
			faceEngine = faceEngineObjectPool.borrowObject();

			// 设置活体检测参数
			int paramCode = this.setLivenessParam(faceEngine, liveness, result);
			if (ErrorInfo.getValidEnum(paramCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", paramCode);
				result.put("error_msg", "");
			}

			// 人脸检测得到人脸列表
			List<FaceInfo> faceInfoList = new ArrayList<FaceInfo>();
			// 人脸检测
			int detectCode = faceEngine.detectFaces(imageInfo.getImageData(), imageInfo.getWidth(),
					imageInfo.getHeight(), imageInfo.getImageFormat(), faceInfoList);
			if (ErrorInfo.getValidEnum(detectCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", detectCode);
				result.put("error_msg", "");
				return result;
			}
			// 人脸属性检测
	        FunctionConfiguration configuration = FunctionConfiguration.builder()
	        		.supportAge(properties.getFunctionConfiguration().isSupportAge())
	        		.supportFace3dAngle(properties.getFunctionConfiguration().isSupportFace3dAngle())
	        		//.supportFaceDetect(properties.getFunctionConfiguration().isSupportFaceDetect())
	        		.supportGender(properties.getFunctionConfiguration().isSupportGender())
	        		.supportLiveness(properties.getFunctionConfiguration().isSupportLiveness()).build();
			int processCode = faceEngine.process(imageInfo.getImageData(), imageInfo.getWidth(), imageInfo.getHeight(),
					imageInfo.getImageFormat(), faceInfoList, configuration);
			if (ErrorInfo.getValidEnum(processCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", processCode);
				result.put("error_msg", "");
				return result;
			}
			List<LivenessInfo> livenessInfoList = Lists.newLinkedList();
			List<AgeInfo> ageInfoList = new ArrayList<AgeInfo>();
			List<GenderInfo> genderInfoList = new ArrayList<GenderInfo>();
			List<Face3DAngle> face3DAngleList = Lists.newLinkedList();

			// RGB活体检测
			int livenessCode = faceEngine.getLiveness(livenessInfoList);
			if (ErrorInfo.getValidEnum(livenessCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", processCode);
				result.put("error_msg", "");
				return result;
			}
			// 年龄检测
			int ageCode = faceEngine.getAge(ageInfoList);
			if (ErrorInfo.getValidEnum(ageCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", processCode);
				result.put("error_msg", "");
				return result;
			}
			// 性别检测
			int genderCode = faceEngine.getGender(genderInfoList);
			if (ErrorInfo.getValidEnum(genderCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", processCode);
				result.put("error_msg", "");
				return result;
			}
			// 三维角度检测
			int angleCode = faceEngine.getFace3DAngle(face3DAngleList);
			if (ErrorInfo.getValidEnum(angleCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", processCode);
				result.put("error_msg", "");
				return result;
			}

			// 检测到的图片中的人脸数量
			result.put("face_num", faceInfoList.size());
			JSONArray face_list = new JSONArray(faceInfoList.size());
			// 人脸识别结果数据
			for (int index = 0; index < faceInfoList.size(); index++) {

				FaceInfo faceInfo = faceInfoList.get(index);

				JSONObject face = new JSONObject();
				// 人脸图片的唯一标识，IMAGE模式下不返回faceId
				face.put("face_token", faceInfo.getFaceId());
				// 人脸角度
				face.put("orient", faceInfo.getOrient());
				// 人脸在图片中的位置
				face.put("location", faceInfo.getRect());
				// 性别，，未知性别=-1 、男性=0 、女性=1
				face.put("gender", genderInfoList.get(index).getGender());
				// 年龄，若为0表示检测失败
				face.put("age", ageInfoList.get(index).getAge());
				// RGB活体信息
				face.put("liveness", livenessInfoList.get(index).getLiveness());
				// 人脸三维角度信息
				face.put("angel", face3DAngleList.get(index));

				// 特征提取
				FaceFeature faceFeature = new FaceFeature();
				int extractCode = faceEngine.extractFaceFeature(imageInfo.getImageData(), imageInfo.getWidth(),
						imageInfo.getHeight(), imageInfo.getImageFormat(), faceInfo, faceFeature);
				if (ErrorInfo.getValidEnum(extractCode).compareTo(ErrorInfo.MERR_NONE) == 0) {
					// 人脸特征数据
					face.put("feature", Base64.getEncoder().encodeToString(faceFeature.getFeatureData()));
				}
				
				face_list.add(index, face);
			}
			
			result.put("face_list", face_list);
			result.put("error_code", 0);
			
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			result.put("error_code", 500);
			result.put("error_msg", "");
		} finally {
			if (faceEngine != null) {
				// 释放引擎对象
				faceEngineObjectPool.returnObject(faceEngine);
			}
		}
		return null;
	}

	/**
	 * IR活体、年龄、性别、三维角度检测
	 * 
	 * @param imageBytes 输入的图像数据
	 * @author ： <a href="https://github.com/vindell">wandl</a>
	 * @return
	 */
	public JSONObject irDetect(byte[] imageBytes, FaceLiveness liveness) {
		return irDetect(ImageFactory.getGrayData(imageBytes), liveness);
	}

	/**
	 * IR活体、年龄、性别、三维角度检测
	 * 
	 * @param imageInfo 输入的图像信息
	 * @author ： <a href="https://github.com/vindell">wandl</a>
	 * @return
	 */
	public JSONObject irDetect(ImageInfo imageInfo, FaceLiveness liveness) {

		JSONObject result = new JSONObject();
		FaceEngine faceEngine = null;

		try {

			// 获取引擎对象
			faceEngine = faceEngineObjectPool.borrowObject();

			// 设置活体检测参数
			int paramCode = this.setLivenessParam(faceEngine, liveness, result);
			if (ErrorInfo.getValidEnum(paramCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", paramCode);
				result.put("error_msg", "");
			}

			// 人脸检测得到人脸列表
			List<FaceInfo> faceInfoList = new ArrayList<FaceInfo>();
			// 人脸检测
			int detectCode = faceEngine.detectFaces(imageInfo.getImageData(), imageInfo.getWidth(),
					imageInfo.getHeight(), imageInfo.getImageFormat(), faceInfoList);
			if (ErrorInfo.getValidEnum(detectCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", detectCode);
				result.put("error_msg", "");
				return result;
			}

			// 人脸属性检测
	        FunctionConfiguration configuration = FunctionConfiguration.builder()
	        		.supportAge(properties.getFunctionConfiguration().isSupportAge())
	        		.supportFace3dAngle(properties.getFunctionConfiguration().isSupportFace3dAngle())
	        		//.supportFaceDetect(properties.getFunctionConfiguration().isSupportFaceDetect())
	        		.supportGender(properties.getFunctionConfiguration().isSupportGender())
	        		.supportIRLiveness(properties.getFunctionConfiguration().isSupportIRLiveness()).build();
			int processCode = faceEngine.processIr(imageInfo.getImageData(), imageInfo.getWidth(),
					imageInfo.getHeight(), imageInfo.getImageFormat(), faceInfoList, configuration);
			if (ErrorInfo.getValidEnum(processCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", processCode);
				result.put("error_msg", "");
				return result;
			}

			List<IrLivenessInfo> livenessInfoList = Lists.newLinkedList();
			List<AgeInfo> ageInfoList = new ArrayList<AgeInfo>();
			List<GenderInfo> genderInfoList = new ArrayList<GenderInfo>();
			List<Face3DAngle> face3DAngleList = Lists.newLinkedList();

			// IR活体检测
			int livenessCode = faceEngine.getLivenessIr(livenessInfoList);
			if (ErrorInfo.getValidEnum(livenessCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", processCode);
				result.put("error_msg", "");
				return result;
			}
			// 年龄检测
			int ageCode = faceEngine.getAge(ageInfoList);
			if (ErrorInfo.getValidEnum(ageCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", processCode);
				result.put("error_msg", "");
				return result;
			}
			// 性别检测
			int genderCode = faceEngine.getGender(genderInfoList);
			if (ErrorInfo.getValidEnum(genderCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", processCode);
				result.put("error_msg", "");
				return result;
			}
			// 三维角度检测
			int angleCode = faceEngine.getFace3DAngle(face3DAngleList);
			if (ErrorInfo.getValidEnum(angleCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", processCode);
				result.put("error_msg", "");
				return result;
			}
			
			// 检测到的图片中的人脸数量
			result.put("face_num", faceInfoList.size());
			JSONArray face_list = new JSONArray(faceInfoList.size());
			// 人脸识别结果数据
			for (int index = 0; index < faceInfoList.size(); index++) {

				FaceInfo faceInfo = faceInfoList.get(index);

				JSONObject face = new JSONObject();
				// 人脸图片的唯一标识，IMAGE模式下不返回faceId
				face.put("face_token", faceInfo.getFaceId());
				// 人脸角度
				face.put("orient", faceInfo.getOrient());
				// 人脸在图片中的位置
				face.put("location", faceInfo.getRect());
				// 性别，，未知性别=-1 、男性=0 、女性=1
				face.put("gender", genderInfoList.get(index).getGender());
				// 年龄，若为0表示检测失败
				face.put("age", ageInfoList.get(index).getAge());
				// RGB活体信息
				face.put("liveness", livenessInfoList.get(index).getLiveness());
				// 人脸三维角度信息
				face.put("angel", face3DAngleList.get(index));

				// 特征提取
				FaceFeature faceFeature = new FaceFeature();
				int extractCode = faceEngine.extractFaceFeature(imageInfo.getImageData(), imageInfo.getWidth(),
						imageInfo.getHeight(), imageInfo.getImageFormat(), faceInfo, faceFeature);
				if (ErrorInfo.getValidEnum(extractCode).compareTo(ErrorInfo.MERR_NONE) == 0) {
					// 人脸特征数据
					face.put("feature", Base64.getEncoder().encodeToString(faceFeature.getFeatureData()));
				}
				
				face_list.add(index, face);
				
			}

			result.put("face_list", face_list);
			result.put("error_code", 0);
			
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			result.put("error_code", 500);
			result.put("error_msg", "");
		} finally {
			if (faceEngine != null) {
				// 释放引擎对象
				faceEngineObjectPool.returnObject(faceEngine);
			}
		}
		return null;
	}

	public JSONObject match(ImageInfo sourceImage, byte[] feature, FaceLiveness liveness) {

		JSONObject result = new JSONObject();
		FaceEngine faceEngine = null;
		try {

			// 获取引擎对象
			faceEngine = faceEngineObjectPool.borrowObject();

			// 设置活体检测参数
			int paramCode = this.setLivenessParam(faceEngine, liveness, result);
			if (ErrorInfo.getValidEnum(paramCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", paramCode);
				result.put("error_msg", "");
			}

			// 人脸检测得到人脸列表
			List<FaceInfo> faceInfoList = new ArrayList<FaceInfo>();
			// 人脸检测
			int detectCode = faceEngine.detectFaces(sourceImage.getImageData(), sourceImage.getWidth(),
					sourceImage.getHeight(), sourceImage.getImageFormat(), faceInfoList);
			if (ErrorInfo.getValidEnum(detectCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", detectCode);
				result.put("error_msg", "");
				return result;
			}

			// 特征提取
			FaceFeature sourceFaceFeature = new FaceFeature();
			int extractCode = faceEngine.extractFaceFeature(sourceImage.getImageData(), sourceImage.getWidth(),
					sourceImage.getHeight(), sourceImage.getImageFormat(), faceInfoList.get(0), sourceFaceFeature);
			if (ErrorInfo.getValidEnum(extractCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", extractCode);
				result.put("error_msg", "");
				return result;
			}

			// 特征比对
			FaceFeature targetFaceFeature = new FaceFeature();
			targetFaceFeature.setFeatureData(feature);
			
			FaceSimilar faceSimilar = new FaceSimilar();

			int compareCode = faceEngine.compareFaceFeature(targetFaceFeature, sourceFaceFeature, faceSimilar);
			if (ErrorInfo.getValidEnum(compareCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", compareCode);
				result.put("error_msg", "");
				return result;
			}

			// 特征相似值
			result.put("score", faceSimilar.getScore());
			JSONArray face_list = new JSONArray(faceInfoList.size());
			for (int index = 0; index < faceInfoList.size(); index++) {
				FaceInfo faceInfo = faceInfoList.get(index);
				JSONObject face = new JSONObject();
				// 人脸图片的唯一标识，IMAGE模式下不返回faceId
				face.put("face_token", faceInfo.getFaceId());
				
				face_list.add(index, face);
			}
			result.put("face_list", face_list);
			result.put("error_code", 0);
			
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			result.put("error_code", 500);
			result.put("error_msg", "");
			return result;
		} finally {
			if (faceEngine != null) {
				faceEngineObjectPool.returnObject(faceEngine);
			}
		}
	}
	
	public JSONObject match(byte[] sourceImage, byte[] targetImage, FaceLiveness liveness) {
		return match(ImageFactory.getRGBData(sourceImage), ImageFactory.getRGBData(targetImage), liveness);
	}
	
	public JSONObject match(ImageInfo sourceImage, ImageInfo targetImage, FaceLiveness liveness) {

		JSONObject result = new JSONObject();
		FaceEngine faceEngine = null;
		try {

			// 获取引擎对象
			faceEngine = faceEngineObjectPool.borrowObject();

			// 设置活体检测参数
			int paramCode = this.setLivenessParam(faceEngine, liveness, result);
			if (ErrorInfo.getValidEnum(paramCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", paramCode);
				result.put("error_msg", "");
			}

			// 人脸检测
			List<FaceInfo> faceInfoList = new ArrayList<FaceInfo>();
			int detectCode = faceEngine.detectFaces(sourceImage.getImageData(), sourceImage.getWidth(),
					sourceImage.getHeight(), sourceImage.getImageFormat(), faceInfoList);
			if (ErrorInfo.getValidEnum(detectCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", detectCode);
				result.put("error_msg", "");
				return result;
			}
			
			// 人脸检测
			List<FaceInfo> faceInfoList2 = new ArrayList<FaceInfo>();
			int detectCode2 = faceEngine.detectFaces(targetImage.getImageData(), targetImage.getWidth(),
					targetImage.getHeight(), targetImage.getImageFormat(), faceInfoList2);
			if (ErrorInfo.getValidEnum(detectCode2).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", detectCode2);
				result.put("error_msg", "");
				return result;
			}
			
			// 源图片特征提取
			FaceFeature sourceFaceFeature = new FaceFeature();
			int extractCode = faceEngine.extractFaceFeature(sourceImage.getImageData(), sourceImage.getWidth(),
					sourceImage.getHeight(), sourceImage.getImageFormat(), faceInfoList.get(0), sourceFaceFeature);
			if (ErrorInfo.getValidEnum(extractCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", extractCode);
				result.put("error_msg", "");
				return result;
			}
			// 目标图片特征提取
			FaceFeature targetFaceFeature = new FaceFeature();
			int extractCode2 = faceEngine.extractFaceFeature(targetImage.getImageData(), targetImage.getWidth(),
					targetImage.getHeight(), targetImage.getImageFormat(), faceInfoList2.get(0), targetFaceFeature);
			if (ErrorInfo.getValidEnum(extractCode2).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", extractCode2);
				result.put("error_msg", "");
				return result;
			}

			FaceSimilar faceSimilar = new FaceSimilar();

			int compareCode = faceEngine.compareFaceFeature(targetFaceFeature, sourceFaceFeature, faceSimilar);
			if (ErrorInfo.getValidEnum(compareCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", compareCode);
				result.put("error_msg", "");
				return result;
			}

			// 特征相似值
			result.put("score", faceSimilar.getScore());
			JSONArray face_list = new JSONArray(faceInfoList.size());
			for (int index = 0; index < faceInfoList.size(); index++) {
				FaceInfo faceInfo = faceInfoList.get(index);
				JSONObject face = new JSONObject();
				// 人脸图片的唯一标识，IMAGE模式下不返回faceId
				face.put("face_token", faceInfo.getFaceId());
				
				face_list.add(index, face);
			}
			result.put("face_list", face_list);
			result.put("error_code", 0);
			
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			result.put("error_code", 500);
			result.put("error_msg", "");
			return result;
		} finally {
			if (faceEngine != null) {
				faceEngineObjectPool.returnObject(faceEngine);
			}
		}
	}

	public JSONObject verify(byte[] imageBytes, FaceLiveness liveness) {
		return verify(ImageFactory.getRGBData(imageBytes), liveness);
	}

	public JSONObject verify(ImageInfo imageInfo, FaceLiveness liveness) {

		JSONObject result = new JSONObject();
		FaceEngine faceEngine = null;
		try {

			// 获取引擎对象
			faceEngine = faceEngineObjectPool.borrowObject();

			// 设置活体检测参数
			int paramCode = this.setLivenessParam(faceEngine, liveness, result);
			if (ErrorInfo.getValidEnum(paramCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", paramCode);
				result.put("error_msg", "");
			}

			// 人脸检测得到人脸列表
			List<FaceInfo> faceInfoList = new ArrayList<FaceInfo>();
			// IR属性处理
			int detectCode = faceEngine.detectFaces(imageInfo.getImageData(), imageInfo.getWidth(),
					imageInfo.getHeight(), imageInfo.getImageFormat(), faceInfoList);
			if (ErrorInfo.getValidEnum(detectCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", detectCode);
				result.put("error_msg", "");
				return result;
			}

			// 活体检测
	        FunctionConfiguration configuration = FunctionConfiguration.builder()
	        		//.supportFaceDetect(properties.getFunctionConfiguration().isSupportFaceDetect())
	        		.supportLiveness(properties.getFunctionConfiguration().isSupportLiveness()).build();
			int processCode = faceEngine.process(imageInfo.getImageData(), imageInfo.getWidth(), imageInfo.getHeight(),
					imageInfo.getImageFormat(), faceInfoList, configuration);
			if (ErrorInfo.getValidEnum(processCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", processCode);
				result.put("error_msg", "");
				return result;
			}

			// 活体检测
			List<LivenessInfo> livenessList = new ArrayList<>();
			int livenessCode = faceEngine.getLiveness(livenessList);
			if (ErrorInfo.getValidEnum(livenessCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", livenessCode);
				result.put("error_msg", "");
				return result;
			}

			// 人脸识别结果数据
			JSONArray face_list = new JSONArray(faceInfoList.size());
			for (int index = 0; index < faceInfoList.size(); index++) {
				FaceInfo faceInfo = faceInfoList.get(index);
				JSONObject face = new JSONObject();
				// 人脸图片的唯一标识，IMAGE模式下不返回faceId
				face.put("face_token", faceInfo.getFaceId());
				face.put("liveness", livenessList.get(index).getLiveness());
				face_list.add(index, face);
			}
			result.put("face_list", face_list);
			result.put("error_code", 0);
			
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			result.put("error_code", 500);
			result.put("error_msg", "");
			return result;
		} finally {
			if (faceEngine != null) {
				faceEngineObjectPool.returnObject(faceEngine);
			}
		}
	}

	public JSONObject irVerify(byte[] imageBytes, FaceLiveness liveness) {
		return irVerify(ImageFactory.getGrayData(imageBytes), liveness);
	}

	/**
	 * IR活体检测
	 * 
	 * @author ： <a href="https://github.com/vindell">wandl</a>
	 * @param imageInfo 图片信息(总数据大小应小于10M)，图片上传方式根据image_type来判断
	 * @param option    场景信息，程序会视不同的场景选用相对应的模型。当前支持的场景有COMMON(通用场景)，GATE(闸机场景)，默认使用COMMON
	 * @return
	 */
	public JSONObject irVerify(ImageInfo imageInfo, FaceLiveness liveness) {

		JSONObject result = new JSONObject();
		FaceEngine faceEngine = null;
		try {

			// 获取引擎对象
			faceEngine = faceEngineObjectPool.borrowObject();

			// 设置活体检测参数
			int paramCode = this.setLivenessParam(faceEngine, liveness, result);
			if (ErrorInfo.getValidEnum(paramCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", paramCode);
				result.put("error_msg", "");
			}

			// 人脸检测得到人脸列表
			List<FaceInfo> faceInfoList = new ArrayList<FaceInfo>();
			// IR属性处理
			int detectCode = faceEngine.detectFaces(imageInfo.getImageData(), imageInfo.getWidth(),
					imageInfo.getHeight(), imageInfo.getImageFormat(), faceInfoList);
			if (ErrorInfo.getValidEnum(detectCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", detectCode);
				result.put("error_msg", "");
				return result;
			}

			// 人脸属性检测
	        FunctionConfiguration configuration = FunctionConfiguration.builder()
	        		//.supportFaceDetect(properties.getFunctionConfiguration().isSupportFaceDetect())
	        		.supportIRLiveness(properties.getFunctionConfiguration().isSupportIRLiveness()).build();
			int processCode = faceEngine.processIr(imageInfo.getImageData(), imageInfo.getWidth(),
					imageInfo.getHeight(), imageInfo.getImageFormat(), faceInfoList, configuration);
			if (ErrorInfo.getValidEnum(processCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", processCode);
				result.put("error_msg", "");
				return result;
			}

			// IR活体检测
			List<IrLivenessInfo> irLivenessList = new ArrayList<>();
			int livenessIrCode = faceEngine.getLivenessIr(irLivenessList);
			if (ErrorInfo.getValidEnum(livenessIrCode).compareTo(ErrorInfo.MERR_NONE) != 0) {
				result.put("error_code", livenessIrCode);
				result.put("error_msg", "");
				return result;
			}

			// 人脸识别结果数据
			JSONArray face_list = new JSONArray(faceInfoList.size());
			for (int index = 0; index < faceInfoList.size(); index++) {
				FaceInfo faceInfo = faceInfoList.get(index);
				JSONObject face = new JSONObject();
				// 人脸图片的唯一标识，IMAGE模式下不返回faceId
				face.put("face_token", faceInfo.getFaceId());
				face.put("liveness", irLivenessList.get(index).getLiveness());
				face_list.add(index, face);
			}
			result.put("face_list", face_list);
			result.put("error_code", 0);
			
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			result.put("error_code", 500);
			result.put("error_msg", "");
			return result;
		} finally {
			if (faceEngine != null) {
				faceEngineObjectPool.returnObject(faceEngine);
			}
		}
	}
	
	public ArcFaceRecognitionProperties getProperties() {
		return properties;
	}

}
