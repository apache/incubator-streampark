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
  tableTitle: 'Yarn Queue List',
  createQueue: 'Add Yarn Queue',
  success: 'Success',
  descriptionMessage: 'exceeds maximum length limit of 512 characters',
  modifyYarnQueue: 'Edit Yarn Queue',
  deleteYarnQueue: 'Delete Yarn Queue',
  deleteConfirm: 'Are you sure to delete this queue ?',
  yarnQueueLabelExpression: "Queue{'@'}Label",
  placeholder: {
    yarnQueueLabelExpression: "Please input queue label like {'{queue} or {queue}@{lab1,...}'}",
    description: 'Please input the description of the queue.',
  },
  selectionHint:
    "Quick-set 'yarn.application.name' and 'yarn.application.node-label'. Please contact admins to add queue by the 'Settings' -> 'Yarn Queue' page if no available queues.",
  checkResult: {
    emptyHint: 'Queue label can not be empty.',
    invalidFormatHint: 'Invalid queue label format',
    existedHint: 'The queue label existed in the current team',
  },
  noteInfo:
    'Queue label. eg. "queue1" represents queue name, "queue1{\'@\'}label1,label2" represents that queue name is "queue1" and label(s) of the queue is "label1" & "label2"',
};
