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
  title: 'Cloud Accounts',
  accountName: 'Account Name',
  provider: 'Provider',
  region: 'Region',
  accessKey: 'Access Key',
  secretKey: 'Secret Key',
  connectivity: 'Connectivity',
  status: 'Status',
  untested: 'Untested',
  connected: 'Connected',
  failed: 'Failed',
  enabled: 'Enabled',
  disabled: 'Disabled',
  create: 'Create Cloud Account',
  edit: 'Edit Cloud Account',
  rotateHint: 'Leave both credential fields empty to keep the existing credentials.',
  secretHint: 'The secret key is write-only and is never returned by the server.',
  test: 'Test connection',
  testSuccess: 'Connection succeeded',
  testFailed: 'Connection failed',
  requestId: 'Request ID',
  disable: 'Disable',
  disableConfirm: 'Disable this cloud account?',
  deleteConfirm:
    'Delete this cloud account? Accounts referenced by environments cannot be deleted.',
  grant: 'Team authorization',
  grantTitle: 'Authorize Teams',
  grantHint: 'Only authorized teams can use this account to create managed Flink environments.',
  noTeams: 'No teams are available.',
  lastCheckTime: 'Last Check',
  lastError: 'Last Error',
  endpointManaged: 'The provider endpoint is managed by StreamPark.',
};
