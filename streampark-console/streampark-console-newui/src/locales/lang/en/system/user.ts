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
  addUser: 'Add User',
  modifyUser: 'Edit User',
  viewUser: 'View User',
  userList: 'User List',
  userInfo: 'User Info',
  table: {
    userName: 'User Name',
    nickName: 'Nick Name',
    userType: 'User Type',
    status: 'Status',
    createTime: 'Create Time',
    description: 'Description',
    email: 'email',
    password: 'Password',
    gender: 'gender',
    recentLogin: 'Recent Login'
  },
  userStatus: {
    locked: 'locked',
    effective: 'Effective'
  },
  userGender: {
    male: 'male',
    female: 'female',
    secret: 'secret'
  },
  operation: {
    userNameIsRequired: 'username is required',
    userNameMinLengthRequirement: 'username length cannot be less than 4 characters',
    userNameMaxLengthRequirement: 'exceeds maximum length limit of 8 characters',
    userNameAlreadyExists: 'Sorry the username already exists',
    nickNameIsRequired: 'nickName is required',
    createUserSuccessfulMessage: 'success',
    modify: 'modify',
    viewUserDetailInfoMessage: 'view detail',
    resetUserPasswordMessage: 'reset password',
    resetUserPasswordProcessMessage: 'reseting',
    resetUserPasswordAlertMessage: 'reset password, are you sure',
    view: 'view',
    passwordIsRequired: 'password is required',
    passwordPlaceholder: 'please enter password',
    passwordMinLengthRequirement: 'Password length cannot be less than 8 characters',
    resetPassword: 'reset password',
    resetPasswordSuccessMessagePrefix: 'reset password successful, user [',
    resetPasswordSuccessMessageSuffix: '] new password is streampark666',
    emailValidMessage: 'please enter a valid email address',
    emailMaxLengthRequirement: 'exceeds maximum length limit of 50 characters',
    emailPlaceholder: 'please enter email',
    userTypePlaceholder: 'Please select a user type',
    statusPlaceholder: 'please select status',
    descriptionPlaceholder: 'Please enter description',
    deleteUser: 'delete user',
    deleteUserProcessMessage: 'deleteing',
    deleteAlertMessage: 'Are you sure delete this user ?',
    deleteUserSuccessful: 'delete successful'
  }
}
