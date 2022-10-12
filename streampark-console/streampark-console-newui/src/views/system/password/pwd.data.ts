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
import { FormSchema } from '/@/components/Form';

export const formSchema: FormSchema[] = [
  {
    field: 'passwordOld',
    label: 'current password',
    component: 'InputPassword',
    required: true,
  },
  {
    field: 'passwordNew',
    label: 'new password',
    component: 'StrengthMeter',
    componentProps: {
      placeholder: 'new password',
    },
    rules: [
      {
        required: true,
        message: 'Please enter a new password',
      },
    ],
  },
  {
    field: 'confirmPassword',
    label: 'Confirm Password',
    component: 'InputPassword',

    dynamicRules: ({ values }) => {
      return [
        {
          required: true,
          validator: (_, value) => {
            if (!value) {
              return Promise.reject('password can not be blank');
            }
            if (value !== values.passwordNew) {
              return Promise.reject('The two entered passwords do not match!');
            }
            return Promise.resolve();
          },
        },
      ];
    },
  },
];
