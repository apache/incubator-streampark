/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.console.base.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RestResponseBodyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRoundTripTypedData() {
        RestResponse response = RestResponse.success("payload");
        RestResponseBody<String> body = RestResponseBody.from(response);

        Assertions.assertEquals(RestResponse.STATUS_SUCCESS, body.getStatus());
        Assertions.assertEquals("payload", body.getData());

        RestResponse restored = body.toRestResponse();
        Assertions.assertEquals("payload", restored.getDataAs(String.class));
    }

    @Test
    void shouldSerializeExtensionFieldsAtTopLevel() throws Exception {
        RestResponseBody<Boolean> body = RestResponseBody.success(false)
            .message("syntax error")
            .extra("type", 4)
            .extra("start", 1)
            .extra("end", 2);

        String json = objectMapper.writeValueAsString(body);
        Assertions.assertTrue(json.contains("\"type\":4"));
        Assertions.assertTrue(json.contains("\"start\":1"));
        Assertions.assertTrue(json.contains("\"end\":2"));
    }

    @Test
    void shouldCopyLegacyExtraFieldsFromRestResponse() {
        RestResponse response = RestResponse.success(false)
            .message("err")
            .put("type", 4)
            .put("start", 1)
            .put("end", 2);
        RestResponseBody<Boolean> body = RestResponseBody.from(response);
        Assertions.assertEquals(4, body.getExtensions().get("type"));
        Assertions.assertEquals(1, body.getExtensions().get("start"));
    }
}
