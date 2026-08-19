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

import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ArcFaceMessageSourceTest {

    @Test
    void testGetAccessorReturnsNonNull() {
        MessageSourceAccessor accessor = ArcFaceMessageSource.getAccessor();
        assertNotNull(accessor);
    }

    @Test
    void testDefaultConstructorSetsBasename() {
        ArcFaceMessageSource source = new ArcFaceMessageSource();
        assertNotNull(source);
    }

    @Test
    void testGetMessageFromAccessor() {
        MessageSourceAccessor accessor = ArcFaceMessageSource.getAccessor();
        // K0 is the "OK" key bundled with messages.properties
        String msg = accessor.getMessage("K0", Locale.ROOT);
        assertNotNull(msg);
    }

    @Test
    void testGetAccessorCreatesNewInstance() {
        MessageSourceAccessor a = ArcFaceMessageSource.getAccessor();
        MessageSourceAccessor b = ArcFaceMessageSource.getAccessor();
        assertNotNull(a);
        assertNotNull(b);
    }

    @Test
    void testChineseMessage() {
        MessageSourceAccessor accessor = ArcFaceMessageSource.getAccessor();
        String msg = accessor.getMessage("K0", Locale.SIMPLIFIED_CHINESE);
        assertNotNull(msg);
        assertEquals("\u6b63\u786e", msg);
    }

    @Test
    void testCode0Message() {
        MessageSourceAccessor accessor = ArcFaceMessageSource.getAccessor();
        String english = accessor.getMessage("K0", Locale.ENGLISH);
        assertNotNull(english);
        assertEquals("\u6b63\u786e", english);
    }
}