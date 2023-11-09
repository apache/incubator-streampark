export const REDIRECT_NAME = 'Redirect';

export const PARENT_LAYOUT_NAME = 'ParentLayout';

export const PAGE_NOT_FOUND_NAME = 'PageNotFound';

export const EXCEPTION_COMPONENT = () => import('/@/views/base/exception/Exception.vue');

/**
 * @description: default layout
 */
export const LAYOUT = () => import('/@/layouts/default/index.vue');

/**
 * @description: parent-layout
 */
export const getParentLayout = (_name?: string) => {
  return () =>
    new Promise((resolve) => {
      resolve({
        name: PARENT_LAYOUT_NAME,
      });
    });
};
const projectPath = '/flink/project';
const settingPath = '/setting';
const variablePath = '/flink/variable';

const applicationPath = '/flink/app';
export const menuMap = {
  [`${projectPath}/add`]: projectPath,
  [`${projectPath}/edit`]: projectPath,
  [`${applicationPath}/add`]: applicationPath,
  [`${applicationPath}/detail`]: applicationPath,
  [`${applicationPath}/edit_flink`]: applicationPath,
  [`${applicationPath}/edit_streampark`]: applicationPath,
  [`${applicationPath}/edit_streampark`]: applicationPath,
  [`${settingPath}/add_cluster`]: `${settingPath}/flinkCluster`,
  [`${settingPath}/edit_cluster`]: `${settingPath}/flinkCluster`,
  [`${variablePath}/depend_apps`]: variablePath,
};
