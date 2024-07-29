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

import colors from 'picocolors';
import pkg from '../../../package.json';
import { type PluginOption } from 'vite';

import { getEnvConfig } from '../utils/env';
import { createContentHash } from '../utils/hash';

const GLOBAL_CONFIG_FILE_NAME = '_app.config.js';
const PLUGIN_NAME = 'app-config';

async function createAppConfigPlugin(isBuild: boolean): Promise<PluginOption> {
  let publicPath: string;
  let source: string;
  if (!isBuild) {
    return {
      name: PLUGIN_NAME,
    };
  }

  return {
    name: PLUGIN_NAME,
    async configResolved(_config) {
      let appTitle = _config?.env?.VITE_GLOB_APP_TITLE ?? '';
      appTitle = appTitle.replace(/\s/g, '_').replace(/-/g, '_');
      publicPath = _config.base;
      source = await getConfigSource(appTitle);
    },
    transformIndexHtml(html) {
      publicPath = publicPath.endsWith('/') ? publicPath : `${publicPath}/`;

      const appConfigSrc = `${publicPath || '/'}${GLOBAL_CONFIG_FILE_NAME}?v=${
        pkg.version
      }-${createContentHash(source)}`;

      return {
        html,
        tags: [
          {
            tag: 'script',
            attrs: {
              src: appConfigSrc,
            },
          },
        ],
      };
    },
    generateBundle() {
      try {
        this.emitFile({
          type: 'asset',
          fileName: GLOBAL_CONFIG_FILE_NAME,
          source,
        });

        console.log(colors.cyan(`✨configuration file is build successfully!`));
      } catch (error) {
        console.log(
          colors.red('configuration file configuration file failed to package:\n' + error),
        );
      }
    },
  };
}

/**
 * Get the configuration file variable name
 * @param env
 */
const getVariableName = (title: string) => {
  return `__PRODUCTION__${title || '__APP'}__CONF__`.toUpperCase().replace(/\s/g, '');
};

async function getConfigSource(appTitle: string) {
  const config = await getEnvConfig();
  const variableName = getVariableName(appTitle);
  const windowVariable = `window.${variableName}`;
  // Ensure that the variable will not be modified
  let source = `${windowVariable}=${JSON.stringify(config)};`;
  source += `
    Object.freeze(${windowVariable});
    Object.defineProperty(window, "${variableName}", {
      configurable: false,
      writable: false,
    });
  `.replace(/\s/g, '');
  return source;
}

export { createAppConfigPlugin };
