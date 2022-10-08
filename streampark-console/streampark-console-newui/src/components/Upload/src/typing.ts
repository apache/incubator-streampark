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
import { UploadApiResult } from '/@/api/sys/model/uploadModel';

export enum UploadResultStatus {
  SUCCESS = 'success',
  ERROR = 'error',
  UPLOADING = 'uploading',
}

export interface FileItem {
  thumbUrl?: string;
  name: string;
  size: string | number;
  type?: string;
  percent: number;
  file: File;
  status?: UploadResultStatus;
  responseData?: UploadApiResult;
  uuid: string;
}

export interface PreviewFileItem {
  url: string;
  name: string;
  type: string;
}

export interface FileBasicColumn {
  /**
   * Renderer of the table cell. The return value should be a VNode, or an object for colSpan/rowSpan config
   * @type Function | ScopedSlot
   */
  customRender?: Function;
  /**
   * Title of this column
   * @type any (string | slot)
   */
  title: string;

  /**
   * Width of this column
   * @type string | number
   */
  width?: number;
  /**
   * Display field of the data record, could be set like a.b.c
   * @type string
   */
  dataIndex: string;
  /**
   * specify how content is aligned
   * @default 'left'
   * @type string
   */
  align?: 'left' | 'right' | 'center';
}
