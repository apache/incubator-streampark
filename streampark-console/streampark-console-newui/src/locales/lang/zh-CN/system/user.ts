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
export default {
  addUser: '新增用户',
  modifyUser: '编辑用户',
  viewUser: '用户详情',
  userList: '用户列表',
  userInfo: '用户信息',
  table: {
    userName: '用户名',
    nickName: '昵称',
    userType: '用户类型',
    status: '状态',
    createTime: '创建时间',
    description: '描述',
    email: '邮箱',
    password: '密码',
    gender: '性别',
    recentLogin: '最近登录'
  },
  userStatus: {
    locked: '锁定',
    effective: '激活'
  },
  userGender: {
    male: '男',
    female: '女',
    secret: '保密'
  },
  operation: {
    userNameIsRequired: '用户名必填',
    userNameMinLengthRequirement: '用户名的长度不能少于4个字符',
    userNameMaxLengthRequirement: '超过了8个字符的最大长度限制',
    userNameAlreadyExists: '抱歉，该用户名已经存在',
    nickNameIsRequired: '昵称必填',
    createUserSuccessfulMessage: '成功',
    modify: '编辑',
    viewUserDetailInfoMessage: '详情',
    resetUserPasswordMessage: '重置密码',
    resetUserPasswordProcessMessage: '重置中',
    resetUserPasswordAlertMessage: '您确认要重置密码?',
    view: '查看',
    passwordIsRequired: '密码必填',
    passwordPlaceholder: '请输入密码',
    passwordMinLengthRequirement: '密码长度不能少于8个字符',
    resetPassword: '重置密码',
    resetPasswordSuccessMessagePrefix: '重置密码成功, 用户 [',
    resetPasswordSuccessMessageSuffix: '] 新密码为 streampark666',
    emailValidMessage: '请输入一个有效的电子邮件地址',
    emailMaxLengthRequirement: '超过了50个字符的最大长度限制',
    emailPlaceholder: '请输入邮箱',
    userTypePlaceholder: '请选择用户类型',
    statusPlaceholder: '请选择状态',
    descriptionPlaceholder: '请输入描述',
    deleteUser: '删除',
    deleteUserProcessMessage: '删除中',
    deleteAlertMessage: '您确定删除该用户 ?',
    deleteUserSuccessful: '删除成功'
  }
}
