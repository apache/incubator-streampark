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


export function baseUrl() {
  if (process.env.VUE_APP_ENV) {
    return `${location.protocol}//${location.host}`
  }
  let url = ''
  switch (process.env.NODE_ENV) {
    case 'production':
      url = process.env['VUE_APP_BASE_API']
      break
    // In the development and testing phase, the front and back ends are separated, and the request URI of the back end is configured here.
    case 'development':
      url = process.env['VUE_APP_BASE_API']
      break
  }
  return url
}
