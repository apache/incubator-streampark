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
import path from 'path';
import fs from 'fs-extra';

const uploadUrl = 'http://localhost:3300/static/upload';
const filePath = path.join(__dirname, '../static/upload/');

fs.ensureDir(filePath);
export default class UserService {
  async upload(ctx, files, isMultiple) {
    let fileReader, fileResource, writeStream;

    const fileFunc = function (file) {
      fileReader = fs.createReadStream(file.path);
      fileResource = filePath + `/${file.name}`;
      console.log(fileResource);

      writeStream = fs.createWriteStream(fileResource);
      fileReader.pipe(writeStream);
    };

    const returnFunc = function (flag) {
      if (flag) {
        let url = '';
        for (let i = 0; i < files.length; i++) {
          url += uploadUrl + `/${files[i].name},`;
        }
        url = url.replace(/,$/gi, '');
        ctx.body = {
          url: url,
          code: 0,
          message: 'upload Success!',
        };
      } else {
        ctx.body = {
          url: uploadUrl + `/${files.name}`,
          code: 0,
          message: 'upload Success!',
        };
      }
    };
    console.log(isMultiple, files.length);

    if (isMultiple) {
      for (let i = 0; i < files.length; i++) {
        const f1 = files[i];
        fileFunc(f1);
      }
    } else {
      fileFunc(files);
    }
    fs.ensureDir(filePath);
    returnFunc(isMultiple);
  }
}
