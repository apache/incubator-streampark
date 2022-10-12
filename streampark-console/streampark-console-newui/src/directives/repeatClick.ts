/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Prevent repeated clicks
 * @Example v-repeat-click="()=>{}"
 */
import { on, once } from '/@/utils/domUtils';
import type { Directive, DirectiveBinding } from 'vue';

const repeatDirective: Directive = {
  beforeMount(el: Element, binding: DirectiveBinding<any>) {
    let interval: Nullable<IntervalHandle> = null;
    let startTime = 0;
    const handler = (): void => binding?.value();
    const clear = (): void => {
      if (Date.now() - startTime < 100) {
        handler();
      }
      interval && clearInterval(interval);
      interval = null;
    };

    on(el, 'mousedown', (e: MouseEvent): void => {
      if ((e as any).button !== 0) return;
      startTime = Date.now();
      once(document as any, 'mouseup', clear);
      interval && clearInterval(interval);
      interval = setInterval(handler, 100);
    });
  },
};

export default repeatDirective;
