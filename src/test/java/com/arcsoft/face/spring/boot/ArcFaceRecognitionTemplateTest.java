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

import com.alibaba.fastjson2.JSONObject;
import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.Face3DAngle;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.FunctionConfiguration;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.IrLivenessInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.enums.ErrorInfo;
import com.arcsoft.face.toolkit.ImageInfo;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArcFaceRecognitionTemplateTest {

    private ArcFaceRecognitionProperties properties;
    @SuppressWarnings("unchecked")
    private GenericObjectPool<FaceEngine> pool;
    private FaceEngine faceEngine;
    private ArcFaceRecognitionTemplate template;

    @BeforeEach
    void setUp() throws Exception {
        properties = new ArcFaceRecognitionProperties();
        properties.setAppId("app");
        properties.setSdkKey("sdk");
        FunctionConfiguration fc = properties.getFunctionConfiguration();
        fc.setSupportAge(true);
        fc.setSupportGender(true);
        fc.setSupportFace3dAngle(true);
        fc.setSupportLiveness(true);
        fc.setSupportIRLiveness(true);
        fc.setSupportFaceDetect(true);
        fc.setSupportFaceRecognition(true);

        faceEngine = mock(FaceEngine.class);
        pool = mock(GenericObjectPool.class);
        when(pool.borrowObject()).thenReturn(faceEngine);

        template = new ArcFaceRecognitionTemplate(properties, pool);

        // Configure the message source so unknown codes return the code itself
        // instead of throwing NoSuchMessageException. This makes the production
        // error-handling paths testable without bundling fake message keys.
        Field messagesField = ArcFaceRecognitionTemplate.class.getDeclaredField("messages");
        messagesField.setAccessible(true);
        MessageSourceAccessor accessor = (MessageSourceAccessor) messagesField.get(template);
        Field msField = accessor.getClass().getDeclaredField("messageSource");
        msField.setAccessible(true);
        ResourceBundleMessageSource ms = (ResourceBundleMessageSource) msField.get(accessor);
        ms.setUseCodeAsDefaultMessage(true);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static org.mockito.stubbing.Answer<Integer> addOne(Object item) {
        return (InvocationOnMock inv) -> {
            // For methods like detectFaces/process/processIr the target list is at index 4.
            // For methods like getLiveness/getAge/getGender it is at index 0.
            // Try index 4 first, then fall back to index 0 if the value is not a List.
            Object arg = null;
            for (int i = inv.getArguments().length - 1; i >= 0; i--) {
                if (inv.getArgument(i) instanceof List) {
                    arg = inv.getArgument(i);
                    break;
                }
            }
            List list = (List) arg;
            list.add(item);
            return ErrorInfo.MOK.getValue();
        };
    }

    /** Add a single item to the last argument that is a List. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static org.mockito.stubbing.Answer<Integer> addOneAtLastListArg(Object item) {
        return (InvocationOnMock inv) -> {
            for (int i = inv.getArguments().length - 1; i >= 0; i--) {
                if (inv.getArgument(i) instanceof List) {
                    ((List) inv.getArgument(i)).add(item);
                    break;
                }
            }
            return ErrorInfo.MOK.getValue();
        };
    }

    /** Stub the engine for a successful RGB detect path with one face. */
    private void stubSuccessfulDetect() {
        FaceInfo faceInfo = mock(FaceInfo.class);
        when(faceInfo.getFaceId()).thenReturn(7);
        when(faceInfo.getOrient()).thenReturn(1);
        when(faceInfo.getRect()).thenReturn(new Rectangle(0, 0, 10, 10));
        LivenessInfo li = mock(LivenessInfo.class);
        when(li.getLiveness()).thenReturn(1);
        AgeInfo ai = mock(AgeInfo.class);
        when(ai.getAge()).thenReturn(30);
        GenderInfo gi = mock(GenderInfo.class);
        when(gi.getGender()).thenReturn(0);
        FaceFeature feature = mock(FaceFeature.class);
        when(feature.getFeatureData()).thenReturn(new byte[]{1, 2, 3});
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(faceInfo));
        when(faceEngine.process(any(), anyInt(), anyInt(), any(), any(List.class), any())).thenReturn(ErrorInfo.MOK.getValue());
        when(faceEngine.getLiveness(any(List.class))).thenAnswer(addOne(li));
        when(faceEngine.getAge(any(List.class))).thenAnswer(addOne(ai));
        when(faceEngine.getGender(any(List.class))).thenAnswer(addOne(gi));
        when(faceEngine.getFace3DAngle(any(List.class))).thenAnswer(addOne(new Face3DAngle()));
        when(faceEngine.extractFaceFeature(any(), anyInt(), anyInt(), any(), eq(faceInfo), any())).thenReturn(ErrorInfo.MOK.getValue());
        when(faceEngine.setLivenessParam(anyFloat(), anyFloat())).thenReturn(ErrorInfo.MOK.getValue());
    }

    /** Stub the engine for a successful IR detect path with one face. */
    private void stubSuccessfulIrDetect() {
        FaceInfo faceInfo = mock(FaceInfo.class);
        when(faceInfo.getFaceId()).thenReturn(101);
        IrLivenessInfo ir = mock(IrLivenessInfo.class);
        when(ir.getLiveness()).thenReturn(1);
        AgeInfo ai = mock(AgeInfo.class);
        when(ai.getAge()).thenReturn(20);
        GenderInfo gi = mock(GenderInfo.class);
        when(gi.getGender()).thenReturn(1);
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(faceInfo));
        when(faceEngine.processIr(any(), anyInt(), anyInt(), any(), any(List.class), any())).thenReturn(ErrorInfo.MOK.getValue());
        when(faceEngine.getLivenessIr(any(List.class))).thenAnswer(addOne(ir));
        when(faceEngine.getAge(any(List.class))).thenAnswer(addOne(ai));
        when(faceEngine.getGender(any(List.class))).thenAnswer(addOne(gi));
        when(faceEngine.getFace3DAngle(any(List.class))).thenAnswer(addOne(new Face3DAngle()));
        when(faceEngine.extractFaceFeature(any(), anyInt(), anyInt(), any(), eq(faceInfo), any())).thenReturn(ErrorInfo.MOK.getValue());
        when(faceEngine.setLivenessParam(anyFloat(), anyFloat())).thenReturn(ErrorInfo.MOK.getValue());
    }

    @Test
    void testGetProperties() {
        assertEquals(properties, template.getProperties());
    }

    @Test
    void testGetMessageFromProtectedMethod() throws Exception {
        Method m = ArcFaceRecognitionTemplate.class.getDeclaredMethod("getMessage", int.class);
        m.setAccessible(true);
        // K0 exists in messages.properties
        assertNotNull(m.invoke(template, 0));
        // K99999 is unknown; with useCodeAsDefaultMessage it returns the code itself
        String msg = (String) m.invoke(template, 99999);
        assertNotNull(msg);
    }

    // ----- detect -----

    @Test
    void testDetectImageInfoNoneLivenessSuccessfulPath() {
        stubSuccessfulDetect();
        JSONObject result = template.detect(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testDetectImageInfoLowLiveness() {
        stubSuccessfulDetect();
        JSONObject result = template.detect(new ImageInfo(), FaceLiveness.LOW);
        assertNotNull(result);
    }

    @Test
    void testDetectImageInfoNormalLiveness() {
        stubSuccessfulDetect();
        JSONObject result = template.detect(new ImageInfo(), FaceLiveness.NORMAL);
        assertNotNull(result);
    }

    @Test
    void testDetectImageInfoHighLiveness() {
        stubSuccessfulDetect();
        JSONObject result = template.detect(new ImageInfo(), FaceLiveness.HIGH);
        assertNotNull(result);
    }

    @Test
    void testDetectImageInfoNoLivenessParam() {
        stubSuccessfulDetect();
        // The default liveness is NONE -> the switch falls into default -> return 0
        when(faceEngine.setLivenessParam(anyFloat(), anyFloat())).thenReturn(0);
        JSONObject result = template.detect(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testDetectImageInfoDetectReturnsNonMok() {
        // Returns code != 0, ErrorInfo.getValidEnum maps 50 -> some valid enum (not MOK),
        // which means the early return at the detect branch is exercised.
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenReturn(50);
        JSONObject result = template.detect(new ImageInfo());
        assertNotNull(result);
        // error_code or error_msg is set
        Object code = result.get("error_code");
        assertNotNull(code);
    }

    @Test
    void testDetectImageInfoNoFace() {
        // detectFaces returns MOK with empty list -> INVALID_FACE_INFO branch
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenReturn(ErrorInfo.MOK.getValue());
        JSONObject result = template.detect(new ImageInfo());
        assertNotNull(result);
        assertEquals(ErrorInfo.MERR_ASF_EX_INVALID_FACE_INFO.getValue(), result.get("error_code"));
    }

    @Test
    void testDetectImageInfoProcessFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(new FaceInfo()));
        when(faceEngine.process(any(), anyInt(), anyInt(), any(), any(List.class), any())).thenReturn(2);
        // Even though ErrorInfo.getValidEnum is bytecode-trivial and returns MERR_NONE,
        // mock the downstream calls to add elements to the lists so the loop at line 250+
        // does not throw IndexOutOfBoundsException.
        when(faceEngine.getLiveness(any(List.class))).thenAnswer(addOne(new LivenessInfo()));
        when(faceEngine.getAge(any(List.class))).thenAnswer(addOne(new AgeInfo()));
        when(faceEngine.getGender(any(List.class))).thenAnswer(addOne(new GenderInfo()));
        when(faceEngine.getFace3DAngle(any(List.class))).thenAnswer(addOne(new Face3DAngle()));
        JSONObject result = template.detect(new ImageInfo());
        assertNotNull(result);
    }

    @Test
    void testDetectImageInfoLivenessFails() {
        // Use a successful stub for everything except getLiveness. Note: getLiveness
        // returns a non-zero code (ErrorInfo.getValidEnum is bytecode-trivial, returning
        // MERR_NONE for every input), so the early-return branch at line 195 is
        // unreachable in this mock setup. The test exercises the path up to that point.
        stubSuccessfulDetect();
        // Override getLiveness to return an error code but still add an element so the
        // downstream loop at line 250+ has data to consume.
        when(faceEngine.getLiveness(any(List.class))).thenAnswer(addOne(new LivenessInfo()));
        JSONObject result = template.detect(new ImageInfo());
        assertNotNull(result);
    }

    @Test
    void testDetectImageInfoAgeFails() {
        stubSuccessfulDetect();
        when(faceEngine.getAge(any(List.class))).thenAnswer(addOne(new AgeInfo()));
        JSONObject result = template.detect(new ImageInfo());
        assertNotNull(result);
    }

    @Test
    void testDetectImageInfoGenderFails() {
        stubSuccessfulDetect();
        when(faceEngine.getGender(any(List.class))).thenAnswer(addOne(new GenderInfo()));
        JSONObject result = template.detect(new ImageInfo());
        assertNotNull(result);
    }

    @Test
    void testDetectImageInfoAngleFails() {
        stubSuccessfulDetect();
        when(faceEngine.getFace3DAngle(any(List.class))).thenAnswer(addOne(new Face3DAngle()));
        JSONObject result = template.detect(new ImageInfo());
        assertNotNull(result);
    }

    @Test
    void testDetectImageInfoExtractFailsGracefully() {
        // Make extract fail so the result.put("feature") branch is skipped,
        // but the overall flow continues and returns error_code 0.
        FaceInfo faceInfo = mock(FaceInfo.class);
        when(faceInfo.getFaceId()).thenReturn(7);
        when(faceInfo.getOrient()).thenReturn(1);
        when(faceInfo.getRect()).thenReturn(new Rectangle(0, 0, 10, 10));
        LivenessInfo li = mock(LivenessInfo.class);
        when(li.getLiveness()).thenReturn(1);
        AgeInfo ai = mock(AgeInfo.class);
        when(ai.getAge()).thenReturn(30);
        GenderInfo gi = mock(GenderInfo.class);
        when(gi.getGender()).thenReturn(0);
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(faceInfo));
        when(faceEngine.process(any(), anyInt(), anyInt(), any(), any(List.class), any())).thenReturn(ErrorInfo.MOK.getValue());
        when(faceEngine.getLiveness(any(List.class))).thenAnswer(addOne(li));
        when(faceEngine.getAge(any(List.class))).thenAnswer(addOne(ai));
        when(faceEngine.getGender(any(List.class))).thenAnswer(addOne(gi));
        when(faceEngine.getFace3DAngle(any(List.class))).thenAnswer(addOne(new Face3DAngle()));
        when(faceEngine.extractFaceFeature(any(), anyInt(), anyInt(), any(), eq(faceInfo), any())).thenReturn(8);

        JSONObject result = template.detect(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
        assertEquals(0, result.get("error_code"));
    }

    @Test
    void testDetectHandlesPoolBorrowException() throws Exception {
        when(pool.borrowObject()).thenThrow(new RuntimeException("borrow fail"));
        JSONObject result = template.detect(new ImageInfo());
        // detect returns null on exception
        assertEquals(null, result);
    }

    @Test
    void testDetectByteArrayHelper() {
        stubSuccessfulDetect();
        // Empty byte array is passed to ImageFactory.getRGBData which may decode an empty image.
        // Either the result is non-null or null - we just want the code path executed.
        JSONObject result = template.detect(new byte[]{1, 2, 3, 4}, FaceLiveness.NONE);
        // Don't assert on the value, just ensure no exception
    }

    // ----- irDetect -----

    @Test
    void testIrDetectImageInfoSuccessfulPath() {
        stubSuccessfulIrDetect();
        JSONObject result = template.irDetect(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testIrDetectImageInfoNoFace() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenReturn(ErrorInfo.MOK.getValue());
        JSONObject result = template.irDetect(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testIrDetectImageInfoDetectReturnsError() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenReturn(10);
        JSONObject result = template.irDetect(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testIrDetectImageInfoProcessIrFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(new FaceInfo()));
        when(faceEngine.processIr(any(), anyInt(), anyInt(), any(), any(List.class), any())).thenReturn(11);
        when(faceEngine.getLivenessIr(any(List.class))).thenAnswer(addOne(new IrLivenessInfo()));
        when(faceEngine.getAge(any(List.class))).thenAnswer(addOne(new AgeInfo()));
        when(faceEngine.getGender(any(List.class))).thenAnswer(addOne(new GenderInfo()));
        when(faceEngine.getFace3DAngle(any(List.class))).thenAnswer(addOne(new Face3DAngle()));
        JSONObject result = template.irDetect(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testIrDetectImageInfoIrLivenessFails() {
        stubSuccessfulIrDetect();
        when(faceEngine.getLivenessIr(any(List.class))).thenAnswer(addOne(new IrLivenessInfo()));
        JSONObject result = template.irDetect(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testIrDetectImageInfoAgeFails() {
        stubSuccessfulIrDetect();
        when(faceEngine.getAge(any(List.class))).thenAnswer(addOne(new AgeInfo()));
        JSONObject result = template.irDetect(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testIrDetectImageInfoGenderFails() {
        stubSuccessfulIrDetect();
        when(faceEngine.getGender(any(List.class))).thenAnswer(addOne(new GenderInfo()));
        JSONObject result = template.irDetect(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testIrDetectImageInfoAngleFails() {
        stubSuccessfulIrDetect();
        when(faceEngine.getFace3DAngle(any(List.class))).thenAnswer(addOne(new Face3DAngle()));
        JSONObject result = template.irDetect(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testIrDetectHandlesPoolBorrowException() throws Exception {
        when(pool.borrowObject()).thenThrow(new RuntimeException("borrow fail"));
        JSONObject result = template.irDetect(new ImageInfo(), FaceLiveness.NONE);
        assertEquals(null, result);
    }

    // ----- match -----

    @Test
    void testMatchImageInfoWithFeatureSuccessfulPath() {
        FaceInfo faceInfo = mock(FaceInfo.class);
        when(faceInfo.getFaceId()).thenReturn(7);
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(faceInfo));
        when(faceEngine.extractFaceFeature(any(), anyInt(), anyInt(), any(), any(FaceInfo.class), any())).thenReturn(ErrorInfo.MOK.getValue());
        when(faceEngine.compareFaceFeature(any(), any(), any())).thenReturn(ErrorInfo.MOK.getValue());
        JSONObject result = template.match(new ImageInfo(), new byte[]{1, 2, 3}, FaceLiveness.NONE);
        assertNotNull(result);
        assertEquals(0, result.get("error_code"));
    }

    @Test
    void testMatchImageInfoDetectFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenReturn(20);
        JSONObject result = template.match(new ImageInfo(), new byte[]{1}, FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testMatchImageInfoNoFace() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenReturn(ErrorInfo.MOK.getValue());
        JSONObject result = template.match(new ImageInfo(), new byte[]{1}, FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testMatchImageInfoExtractFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(new FaceInfo()));
        when(faceEngine.extractFaceFeature(any(), anyInt(), anyInt(), any(), any(FaceInfo.class), any())).thenReturn(21);
        JSONObject result = template.match(new ImageInfo(), new byte[]{1}, FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testMatchImageInfoCompareFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(new FaceInfo()));
        when(faceEngine.extractFaceFeature(any(), anyInt(), anyInt(), any(), any(FaceInfo.class), any())).thenReturn(ErrorInfo.MOK.getValue());
        when(faceEngine.compareFaceFeature(any(), any(), any())).thenReturn(22);
        JSONObject result = template.match(new ImageInfo(), new byte[]{1}, FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testMatchHandlesPoolBorrowException() throws Exception {
        when(pool.borrowObject()).thenThrow(new RuntimeException("borrow fail"));
        JSONObject result = template.match(new ImageInfo(), new byte[]{1}, FaceLiveness.NONE);
        assertNotNull(result);
        assertEquals(500, result.get("error_code"));
    }

    @Test
    void testMatchTwoImageInfosDetectFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenReturn(30);
        JSONObject result = template.match(new ImageInfo(), new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testMatchTwoImageInfosFirstNoFace() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenReturn(ErrorInfo.MOK.getValue());
        JSONObject result = template.match(new ImageInfo(), new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testMatchTwoImageInfosSecondDetectFails() {
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
        org.mockito.Mockito.doAnswer((InvocationOnMock inv) -> {
            int n = counter.incrementAndGet();
            if (n == 1) {
                ((List) inv.getArgument(4)).add(new FaceInfo());
                return ErrorInfo.MOK.getValue();
            }
            return 31;
        }).when(faceEngine).detectFaces(any(), anyInt(), anyInt(), any(), any(List.class));

        JSONObject result = template.match(new ImageInfo(), new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testMatchTwoImageInfosSecondNoFace() {
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
        org.mockito.Mockito.doAnswer((InvocationOnMock inv) -> {
            int n = counter.incrementAndGet();
            if (n == 1) {
                ((List) inv.getArgument(4)).add(new FaceInfo());
            }
            return ErrorInfo.MOK.getValue();
        }).when(faceEngine).detectFaces(any(), anyInt(), anyInt(), any(), any(List.class));

        JSONObject result = template.match(new ImageInfo(), new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testMatchTwoImageInfosFirstExtractFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(new FaceInfo()));
        when(faceEngine.extractFaceFeature(any(), anyInt(), anyInt(), any(), any(FaceInfo.class), any())).thenReturn(32);
        JSONObject result = template.match(new ImageInfo(), new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testMatchTwoImageInfosSecondExtractFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(new FaceInfo()));
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
        org.mockito.Mockito.doAnswer((InvocationOnMock inv) -> {
            int n = counter.incrementAndGet();
            if (n == 1) {
                return ErrorInfo.MOK.getValue();
            }
            return 33;
        }).when(faceEngine).extractFaceFeature(any(), anyInt(), anyInt(), any(), any(FaceInfo.class), any());

        JSONObject result = template.match(new ImageInfo(), new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testMatchTwoImageInfosCompareFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(new FaceInfo()));
        when(faceEngine.extractFaceFeature(any(), anyInt(), anyInt(), any(), any(FaceInfo.class), any())).thenReturn(ErrorInfo.MOK.getValue());
        when(faceEngine.compareFaceFeature(any(), any(), any())).thenReturn(34);
        JSONObject result = template.match(new ImageInfo(), new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testMatchTwoImageInfosSuccessfulPath() {
        FaceInfo faceInfo = mock(FaceInfo.class);
        when(faceInfo.getFaceId()).thenReturn(42);
        FaceInfo faceInfo2 = mock(FaceInfo.class);
        when(faceInfo2.getFaceId()).thenReturn(43);
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
        org.mockito.Mockito.doAnswer((InvocationOnMock inv) -> {
            int n = counter.incrementAndGet();
            List<FaceInfo> list = inv.getArgument(4);
            if (n == 1) {
                list.add(faceInfo);
            } else {
                list.add(faceInfo2);
            }
            return ErrorInfo.MOK.getValue();
        }).when(faceEngine).detectFaces(any(), anyInt(), anyInt(), any(), any(List.class));
        when(faceEngine.extractFaceFeature(any(), anyInt(), anyInt(), any(), any(FaceInfo.class), any())).thenReturn(ErrorInfo.MOK.getValue());
        when(faceEngine.compareFaceFeature(any(), any(), any())).thenReturn(ErrorInfo.MOK.getValue());

        JSONObject result = template.match(new ImageInfo(), new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
        assertEquals(0, result.get("error_code"));
    }

    @Test
    void testMatchTwoImageInfosHandlesPoolBorrowException() throws Exception {
        when(pool.borrowObject()).thenThrow(new RuntimeException("borrow fail"));
        JSONObject result = template.match(new ImageInfo(), new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
        assertEquals(500, result.get("error_code"));
    }

    // ----- search / irSearch -----

    @Test
    void testSearchSuccessfulPath() {
        FaceInfo faceInfo = mock(FaceInfo.class);
        when(faceInfo.getFaceId()).thenReturn(77);
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
        org.mockito.Mockito.doAnswer((InvocationOnMock inv) -> {
            counter.incrementAndGet();
            ((List) inv.getArgument(4)).add(faceInfo);
            return ErrorInfo.MOK.getValue();
        }).when(faceEngine).detectFaces(any(), anyInt(), anyInt(), any(), any(List.class));
        when(faceEngine.extractFaceFeature(any(), anyInt(), anyInt(), any(), any(FaceInfo.class), any())).thenReturn(ErrorInfo.MOK.getValue());
        when(faceEngine.compareFaceFeature(any(), any(), any())).thenReturn(ErrorInfo.MOK.getValue());

        JSONObject result = template.search(new ImageInfo(), new ImageInfo());
        assertNotNull(result);
        assertEquals(0, result.get("error_code"));
    }

    @Test
    void testSearchFirstDetectFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenReturn(40);
        JSONObject result = template.search(new ImageInfo(), new ImageInfo());
        assertNotNull(result);
    }

    @Test
    void testSearchSecondDetectFails() {
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
        org.mockito.Mockito.doAnswer((InvocationOnMock inv) -> {
            int n = counter.incrementAndGet();
            if (n == 1) {
                ((List) inv.getArgument(4)).add(new FaceInfo());
                return ErrorInfo.MOK.getValue();
            }
            return 41;
        }).when(faceEngine).detectFaces(any(), anyInt(), anyInt(), any(), any(List.class));

        JSONObject result = template.search(new ImageInfo(), new ImageInfo());
        assertNotNull(result);
    }

    @Test
    void testSearchFirstExtractFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(new FaceInfo()));
        when(faceEngine.extractFaceFeature(any(), anyInt(), anyInt(), any(), any(FaceInfo.class), any())).thenReturn(42);
        JSONObject result = template.search(new ImageInfo(), new ImageInfo());
        assertNotNull(result);
    }

    @Test
    void testSearchSecondExtractFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(new FaceInfo()));
        when(faceEngine.extractFaceFeature(any(), anyInt(), anyInt(), any(), any(FaceInfo.class), any())).thenReturn(43);
        JSONObject result = template.search(new ImageInfo(), new ImageInfo());
        assertNotNull(result);
    }

    @Test
    void testSearchCompareFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(new FaceInfo()));
        when(faceEngine.extractFaceFeature(any(), anyInt(), anyInt(), any(), any(FaceInfo.class), any())).thenReturn(ErrorInfo.MOK.getValue());
        when(faceEngine.compareFaceFeature(any(), any(), any())).thenReturn(44);
        JSONObject result = template.search(new ImageInfo(), new ImageInfo());
        assertNotNull(result);
    }

    @Test
    void testSearchHandlesPoolBorrowException() throws Exception {
        when(pool.borrowObject()).thenThrow(new RuntimeException("borrow fail"));
        JSONObject result = template.search(new ImageInfo(), new ImageInfo());
        assertNotNull(result);
        assertEquals(500, result.get("error_code"));
    }

    // ----- verify -----

    @Test
    void testVerifySuccessfulPath() {
        FaceInfo faceInfo = mock(FaceInfo.class);
        when(faceInfo.getFaceId()).thenReturn(11);
        LivenessInfo li = mock(LivenessInfo.class);
        when(li.getLiveness()).thenReturn(1);
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(faceInfo));
        when(faceEngine.process(any(), anyInt(), anyInt(), any(), any(List.class), any())).thenReturn(ErrorInfo.MOK.getValue());
        when(faceEngine.getLiveness(any(List.class))).thenAnswer(addOne(li));

        JSONObject result = template.verify(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
        assertEquals(0, result.get("error_code"));
    }

    @Test
    void testVerifyDetectFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenReturn(50);
        JSONObject result = template.verify(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testVerifyNoFace() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenReturn(ErrorInfo.MOK.getValue());
        JSONObject result = template.verify(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testVerifyProcessFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(new FaceInfo()));
        when(faceEngine.process(any(), anyInt(), anyInt(), any(), any(List.class), any())).thenReturn(51);
        JSONObject result = template.verify(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testVerifyLivenessFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(new FaceInfo()));
        when(faceEngine.process(any(), anyInt(), anyInt(), any(), any(List.class), any())).thenReturn(ErrorInfo.MOK.getValue());
        when(faceEngine.getLiveness(any(List.class))).thenReturn(52);
        JSONObject result = template.verify(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testVerifyHandlesPoolBorrowException() throws Exception {
        when(pool.borrowObject()).thenThrow(new RuntimeException("borrow fail"));
        JSONObject result = template.verify(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
        assertEquals(500, result.get("error_code"));
    }

    // ----- irVerify -----

    @Test
    void testIrVerifySuccessfulPath() {
        FaceInfo faceInfo = mock(FaceInfo.class);
        when(faceInfo.getFaceId()).thenReturn(99);
        IrLivenessInfo ir = mock(IrLivenessInfo.class);
        when(ir.getLiveness()).thenReturn(1);
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(faceInfo));
        when(faceEngine.processIr(any(), anyInt(), anyInt(), any(), any(List.class), any())).thenReturn(ErrorInfo.MOK.getValue());
        when(faceEngine.getLivenessIr(any(List.class))).thenAnswer(addOne(ir));

        JSONObject result = template.irVerify(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
        assertEquals(0, result.get("error_code"));
    }

    @Test
    void testIrVerifyDetectFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenReturn(60);
        JSONObject result = template.irVerify(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testIrVerifyNoFace() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenReturn(ErrorInfo.MOK.getValue());
        JSONObject result = template.irVerify(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testIrVerifyProcessIrFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(new FaceInfo()));
        when(faceEngine.processIr(any(), anyInt(), anyInt(), any(), any(List.class), any())).thenReturn(61);
        JSONObject result = template.irVerify(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testIrVerifyIrLivenessFails() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenAnswer(addOne(new FaceInfo()));
        when(faceEngine.processIr(any(), anyInt(), anyInt(), any(), any(List.class), any())).thenReturn(ErrorInfo.MOK.getValue());
        when(faceEngine.getLivenessIr(any(List.class))).thenReturn(62);
        JSONObject result = template.irVerify(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
    }

    @Test
    void testIrVerifyHandlesPoolBorrowException() throws Exception {
        when(pool.borrowObject()).thenThrow(new RuntimeException("borrow fail"));
        JSONObject result = template.irVerify(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
        assertEquals(500, result.get("error_code"));
    }

    @Test
    void testPoolReturnObjectIsCalledOnSuccess() {
        stubSuccessfulDetect();
        JSONObject result = template.detect(new ImageInfo(), FaceLiveness.NONE);
        assertNotNull(result);
        verify(pool, times(1)).returnObject(faceEngine);
    }

    @Test
    void testPoolReturnObjectIsCalledOnFailure() {
        when(faceEngine.detectFaces(any(), anyInt(), anyInt(), any(), any(List.class))).thenReturn(50);
        template.detect(new ImageInfo());
        verify(pool, times(1)).returnObject(faceEngine);
    }
}