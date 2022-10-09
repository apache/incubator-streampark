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
export function checkFileType(file: File, accepts: string[]) {
  const newTypes = accepts.join('|');
  // const reg = /\.(jpg|jpeg|png|gif|txt|doc|docx|xls|xlsx|xml)$/i;
  const reg = new RegExp('\\.(' + newTypes + ')$', 'i');

  return reg.test(file.name);
}

export function checkImgType(file: File) {
  return isImgTypeByName(file.name);
}

export function isImgTypeByName(name: string) {
  return /\.(jpg|jpeg|png|gif)$/i.test(name);
}

export function getBase64WithFile(file: File) {
  return new Promise<{
    result: string;
    file: File;
  }>((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve({ result: reader.result as string, file });
    reader.onerror = (error) => reject(error);
  });
}
