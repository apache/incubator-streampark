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
import { Tag } from 'ant-design-vue';
import { defineComponent } from 'vue';
import './LoginSlogan.less';
import Icon from '/@/components/Icon';
import { version } from '../../../../package.json';
export default defineComponent({
  name: 'LoginSlogan',
  setup() {
    return () => {
      return (
        <div className="!text-left w-550px m-auto">
          <div className="mb-5 system_info pt-0">
            <div className="project_title fw-bold text-white mb-3">
              <div
                className="animated-gradient-text_background animated-gradient-text_background-1"
                style={{
                  '--content': '"Apache"',
                  '--start-color': '#00DFD8',
                  '--end-color': '#FFF',
                }}
              >
                <span className="animated-gradient-text_foreground animated-gradient-text_foreground-1">
                  Apache
                </span>
              </div>
              <div className="flex  items-start">
                <div
                  className="animated-gradient-text_background animated-gradient-text_background-2"
                  style={{
                    '--content': '"StreamPark"',
                    '--start-color': '#FFF',
                    '--end-color': '#00DFD8',
                  }}
                >
                  <span className="animated-gradient-text_foreground animated-gradient-text_foreground-2">
                    StreamPark
                  </span>
                </div>
                <span className="badge bg-secondary-home text-xl tag">Incubating</span>
              </div>
            </div>
            <p className=" text-light-200 leading-40px" style={{ fontSize: '18px' }}>
              <div>Make stream processing easier!</div>
              <div>easy-to-use streaming application development framework</div>
              <div>and operation platform</div>
            </p>
          </div>
          <div className="flex items-center mt-10">
            <a
              className="btn streampark-btn btn !flex items-center"
              href="https://github.com/apache/incubator-streampark"
              target="_blank"
            >
              <Icon icon="ant-design:github-filled"></Icon>
              <div>&nbsp; GitHub</div>
            </a>
            <a
              className="btn streampark-btn btn-green !flex items-center ml-10px"
              href="https://streampark.apache.org"
              target="_blank"
            >
              <Icon icon="carbon:document"></Icon>
              <div>&nbsp;Document</div>
            </a>
          </div>

          <div className="mt-20px shields z-3 flex items-center">
            <Tag color="#477de9">Version: v{version}</Tag>
            <img
              src="https://img.shields.io/github/stars/apache/incubator-streampark.svg?sanitize=true"
              className="wow fadeInUp"
            ></img>
            <img
              src="https://img.shields.io/github/forks/apache/incubator-streampark.svg?sanitize=true"
              className="wow fadeInUp"
            ></img>
          </div>
        </div>
      );
    };
  },
});
