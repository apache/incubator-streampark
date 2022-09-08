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

const getters = {
  // app
  device: state => state.app.device,
  theme: state => state.app.theme,
  color: state => state.app.color,

  // user
  permissions: state => state.user.permissions,
  roles: state => state.user.roles,
  token: state => state.user.token,
  avatar: state => state.user.avatar,
  nickname: state => state.user.name,
  userName: state => state.user.userName,
  welcome: state => state.user.welcome,
  userInfo: state => state.user.info,
  routers: state => state.user.routers,
  multiTab: state => state.app.multiTab,
  applicationId: state => state.application.appId,
  clusterId: state => state.cluster.clusterId,
  projectId: state => state.project.projectId
}

export default getters
