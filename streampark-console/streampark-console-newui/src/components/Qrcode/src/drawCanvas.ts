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
import { toCanvas } from 'qrcode';
import type { QRCodeRenderersOptions } from 'qrcode';
import { RenderQrCodeParams, ContentType } from './typing';
import { cloneDeep } from 'lodash-es';

export const renderQrCode = ({
  canvas,
  content,
  width = 0,
  options: params = {},
}: RenderQrCodeParams) => {
  const options = cloneDeep(params);
  // Fault tolerance rate, the default for two-dimensional codes with less content adopts high fault tolerance rate, and two-dimensional codes with more content adopt low fault tolerance rate
  options.errorCorrectionLevel = options.errorCorrectionLevel || getErrorCorrectionLevel(content);

  return getOriginWidth(content, options).then((_width: number) => {
    options.scale = width === 0 ? undefined : (width / _width) * 4;
    return toCanvas(canvas, content, options);
  });
};

// Get the size of the original QrCode so that you can scale to get the correct QrCode size
function getOriginWidth(content: ContentType, options: QRCodeRenderersOptions) {
  const _canvas = document.createElement('canvas');
  return toCanvas(_canvas, content, options).then(() => _canvas.width);
}

// For QrCode with little content, increase the fault tolerance
function getErrorCorrectionLevel(content: ContentType) {
  if (content.length > 36) {
    return 'M';
  } else if (content.length > 16) {
    return 'Q';
  } else {
    return 'H';
  }
}
