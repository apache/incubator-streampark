/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.streamxhub.streamx.console.core.enums.AlertType;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author weijinglun
 * @date 2022.01.14
 */
public class AlertTypeTest {
    @Test
    void decodeTest() {
        List<AlertType> notifyTypes = AlertType.decode(5);
        System.out.println(notifyTypes);
    }

    @Test
    void encodeTest() {
        int level = AlertType.encode(Arrays.asList(AlertType.dingTalk, AlertType.email));
        System.out.println(level);
    }
}
