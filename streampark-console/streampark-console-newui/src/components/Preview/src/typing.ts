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
export interface Options {
  show?: boolean;
  imageList: string[];
  index?: number;
  scaleStep?: number;
  defaultWidth?: number;
  maskClosable?: boolean;
  rememberState?: boolean;
  onImgLoad?: ({ index: number, url: string, dom: HTMLImageElement }) => void;
  onImgError?: ({ index: number, url: string, dom: HTMLImageElement }) => void;
}

export interface Props {
  show: boolean;
  instance: Props;
  imageList: string[];
  index: number;
  scaleStep: number;
  defaultWidth: number;
  maskClosable: boolean;
  rememberState: boolean;
}

export interface PreviewActions {
  resume: () => void;
  close: () => void;
  prev: () => void;
  next: () => void;
  setScale: (scale: number) => void;
  setRotate: (rotate: number) => void;
}

export interface ImageProps {
  alt?: string;
  fallback?: string;
  src: string;
  width: string | number;
  height?: string | number;
  placeholder?: string | boolean;
  preview?:
    | boolean
    | {
        visible?: boolean;
        onVisibleChange?: (visible: boolean, prevVisible: boolean) => void;
        getContainer: string | HTMLElement | (() => HTMLElement);
      };
}

export type ImageItem = string | ImageProps;
