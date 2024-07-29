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
import { Divider, Table, Tag, type TableColumnType, Empty, Skeleton } from 'ant-design-vue';
import { defineComponent, ref, shallowRef, watchEffect } from 'vue';
import { fetchApiSchema } from '/@/api/system/openapi';
import { baseUrl } from '/@/api';
import { useAppStore } from '/@/store/modules/app';
import { ThemeEnum } from '/@/enums/appEnum';
import { useI18n } from '/@/hooks/web/useI18n';
interface RequestSchemas {
  url: string;
  method?: string;
  params?: Recordable[];
  body?: Recordable[];
  header?: Recordable[];
}

const methodMap = {
  GET: {
    lightColor: '#22c55e',
    darkColor: '#10b981',
  },
  POST: {
    lightColor: '#f59e0b',
    darkColor: '#eab308',
  },
  PUT: {
    lightColor: '#3b82f6',
    darkColor: '#0ea5e9  ',
  },
  DELETE: {
    lightColor: '#ef4444',
    darkColor: '#f43f5e',
  },
  PATCH: {
    lightColor: '#a855f7',
    darkColor: '#8b5cf6',
  },
  DEFAULT: {
    lightColor: '#737373',
    darkColor: '#71717a',
  },
} as const;
export default defineComponent({
  name: 'OpenApi',
  props: {
    name: {
      type: String,
      required: true,
    },
  },
  setup(props) {
    const requestRef = shallowRef<RequestSchemas>({
      url: '',
    });
    const { t } = useI18n();
    const fetchLoading = ref(false);
    const appStore = useAppStore();
    const handleGetRequestSchemas = async () => {
      try {
        fetchLoading.value = true;
        const resp = await fetchApiSchema({
          name: props.name,
        });
        requestRef.value = {
          url: baseUrl() + resp.url,
          method: resp.method,
          params: resp.schema,
          body: resp.body,
          header: resp.header,
        };
      } catch (error) {
        console.error(error);
      } finally {
        fetchLoading.value = false;
      }
    };
    const tableColumns = shallowRef<TableColumnType[]>([
      { title: t('component.openApi.param'), dataIndex: 'name', width: 160 },
      { title: t('component.openApi.defaultValue'), dataIndex: 'defaultValue', width: 130 },
      { title: t('component.openApi.type'), dataIndex: 'type', width: 150 },
      {
        title: t('component.openApi.required'),
        dataIndex: 'required',
        customRender: ({ text }) => (
          <Tag color={text ? 'success' : 'error'}>{text ? t('common.yes') : t('common.no')}</Tag>
        ),
      },
      { title: t('component.openApi.description'), dataIndex: 'description' },
    ]);

    watchEffect(() => {
      if (props.name) {
        handleGetRequestSchemas();
      }
    });

    const renderParams = () => {
      if (!requestRef.value.params) return null;
      return (
        <>
          <Divider orientation="left">Params</Divider>
          {renderTableData(requestRef.value.params)}
        </>
      );
    };

    const renderBody = () => {
      if (!requestRef.value.body) return null;
      return (
        <>
          <Divider orientation="left">Body</Divider>
          {renderTableData(requestRef.value.body)}
        </>
      );
    };
    const renderHeader = () => {
      if (!requestRef.value.header) return null;
      return (
        <>
          <Divider orientation="left">Header</Divider>
          {renderTableData(requestRef.value.header)}
        </>
      );
    };
    const renderTableData = (data: Recordable[]) => {
      if (!data || (Array.isArray(data) && data.length === 0))
        return (
          <>
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              v-slots={{
                description: () => <span class="text-gray-4">{t('component.openApi.empty')}</span>,
              }}
            ></Empty>
          </>
        );
      return (
        <>
          <Table
            size="small"
            bordered
            pagination={false}
            dataSource={data}
            columns={tableColumns.value}
          ></Table>
        </>
      );
    };
    const renderRequestMethod = () => {
      if (!requestRef.value.method) return null;
      const currentMethod =
        methodMap[requestRef.value.method.toLocaleUpperCase()] ?? methodMap.DEFAULT;
      const darkMode = appStore.getDarkMode === ThemeEnum.DARK;
      return (
        <div class="relative flex">
          <label for="method">
            <span class="flex overflow-hidden text-ellipsis whitespace-nowrap">
              <div class="flex flex-1 relative">
                <div
                  id="method"
                  class="flex w-20 rounded-l bg-[#f9fafb] dark:bg-[#1c1c1e] px-4 py-2 font-semibold justify-center transition"
                  style={{
                    color: darkMode ? currentMethod.lightColor : currentMethod.darkColor,
                  }}
                >
                  {requestRef.value.method.toLocaleUpperCase()}
                </div>
              </div>
            </span>
          </label>
        </div>
      );
    };

    return () => (
      <>
        <Skeleton loading={fetchLoading.value} active>
          <div class="flex-none flex-shrink-0 ">
            <div class="min-w-[12rem] flex min-h-9 flex-1 whitespace-nowrap rounded border dark:border-[#303030] border-[#f3f4f6]">
              {renderRequestMethod()}
              <div
                class="flex flex-1 items-center text-[#111827] whitespace-nowrap rounded-r border-l border-[#f3f4f6] bg-[#f9fafb] dark:bg-[#1c1c1e] dark:text-[#fff] dark:border-[#303030] transition "
                style="padding-left:10px"
              >
                {requestRef.value.url}
              </div>
            </div>
          </div>
          {renderHeader()}
          {renderParams()}
          {renderBody()}
        </Skeleton>
      </>
    );
  },
});
