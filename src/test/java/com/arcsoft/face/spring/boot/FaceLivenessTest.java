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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class FaceLivenessTest {

    @Test
    void testEnumCount() {
        FaceLiveness[] values = FaceLiveness.values();
        assertEquals(4, values.length);
    }

    @Test
    void testEnumValues() {
        assertNotNull(FaceLiveness.NONE);
        assertNotNull(FaceLiveness.LOW);
        assertNotNull(FaceLiveness.NORMAL);
        assertNotNull(FaceLiveness.HIGH);
    }

    @Test
    void testValueOf() {
        assertSame(FaceLiveness.NONE, FaceLiveness.valueOf("NONE"));
        assertSame(FaceLiveness.LOW, FaceLiveness.valueOf("LOW"));
        assertSame(FaceLiveness.NORMAL, FaceLiveness.valueOf("NORMAL"));
        assertSame(FaceLiveness.HIGH, FaceLiveness.valueOf("HIGH"));
    }

    @Test
    void testOrdinal() {
        assertEquals(0, FaceLiveness.NONE.ordinal());
        assertEquals(1, FaceLiveness.LOW.ordinal());
        assertEquals(2, FaceLiveness.NORMAL.ordinal());
        assertEquals(3, FaceLiveness.HIGH.ordinal());
    }
}