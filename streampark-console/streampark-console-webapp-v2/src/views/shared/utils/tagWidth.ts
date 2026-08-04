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

/** Match naive-ui small tag metrics so state columns align without clipping i18n text. */
export function computeStateTagWidth(maxTitle?: string): number {
    if (!maxTitle) return 0
    const dom = document.createElement('span')
    dom.style.display = 'inline-block'
    dom.style.fontSize = '12px'
    dom.style.fontWeight = '600'
    dom.style.padding = '0 7px'
    dom.style.border = '1px solid transparent'
    dom.style.boxSizing = 'border-box'
    dom.style.whiteSpace = 'nowrap'
    dom.textContent = maxTitle
    document.body.appendChild(dom)
    const width = Math.ceil(dom.getBoundingClientRect().width)
    document.body.removeChild(dom)
    return width
}
