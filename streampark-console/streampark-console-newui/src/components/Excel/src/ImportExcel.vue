<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<template>
  <div>
    <input
      ref="inputRef"
      type="file"
      v-show="false"
      accept=".xlsx, .xls"
      @change="handleInputClick"
    />
    <div @click="handleUpload">
      <slot></slot>
    </div>
  </div>
</template>
<script lang="ts">
  import { defineComponent, ref, unref } from 'vue';
  import * as XLSX from 'xlsx';
  import { dateUtil } from '/@/utils/dateUtil';

  import type { ExcelData } from './typing';
  export default defineComponent({
    name: 'ImportExcel',
    props: {
      // Datetime format. If not provided or a null value is provided, the original Date object will be returned
      dateFormat: {
        type: String,
      },
      // Time zone adjustment. Experimental feature, only to solve the problem of reading datetime values skewed. Currently only offset corrections for the +08:00 time zone are available
      // https://github.com/SheetJS/sheetjs/issues/1470#issuecomment-501108554
      timeZone: {
        type: Number,
        default: 8,
      },
    },
    emits: ['success', 'error'],
    setup(props, { emit }) {
      const inputRef = ref<HTMLInputElement | null>(null);
      const loadingRef = ref<Boolean>(false);

      /**
       * @description: first line as header
       */
      function getHeaderRow(sheet: XLSX.WorkSheet) {
        if (!sheet || !sheet['!ref']) return [];
        const headers: string[] = [];
        // A3:B7=>{s:{c:0, r:2}, e:{c:1, r:6}}
        const range = XLSX.utils.decode_range(sheet['!ref']);

        const R = range.s.r;
        /* start in the first row */
        for (let C = range.s.c; C <= range.e.c; ++C) {
          /* walk every column in the range */
          const cell = sheet[XLSX.utils.encode_cell({ c: C, r: R })];
          /* find the cell in the first row */
          let hdr = 'UNKNOWN ' + C; // <-- replace with your desired default
          if (cell && cell.t) hdr = XLSX.utils.format_cell(cell);
          headers.push(hdr);
        }
        return headers;
      }

      /**
       * @description: get excel data
       */
      function getExcelData(workbook: XLSX.WorkBook) {
        const excelData: ExcelData[] = [];
        const { dateFormat, timeZone } = props;
        for (const sheetName of workbook.SheetNames) {
          const worksheet = workbook.Sheets[sheetName];
          const header: string[] = getHeaderRow(worksheet);
          let results = XLSX.utils.sheet_to_json(worksheet, {
            raw: true,
            dateNF: dateFormat, //Not worked
          }) as object[];
          results = results.map((row: object) => {
            for (let field in row) {
              if (row[field] instanceof Date) {
                if (timeZone === 8) {
                  row[field].setSeconds(row[field].getSeconds() + 43);
                }
                if (dateFormat) {
                  row[field] = dateUtil(row[field]).format(dateFormat);
                }
              }
            }
            return row;
          });

          excelData.push({
            header,
            results,
            meta: {
              sheetName,
            },
          });
        }
        return excelData;
      }

      /**
       * @description: read excel data
       */
      function readerData(rawFile: File) {
        loadingRef.value = true;
        return new Promise((resolve, reject) => {
          const reader = new FileReader();
          reader.onload = async (e) => {
            try {
              const data = e.target && e.target.result;
              const workbook = XLSX.read(data, { type: 'array', cellDates: true });
              // console.log(workbook);
              /* DO SOMETHING WITH workbook HERE */
              const excelData = getExcelData(workbook);
              emit('success', excelData);
              resolve('');
            } catch (error) {
              reject(error);
              emit('error');
            } finally {
              loadingRef.value = false;
            }
          };
          reader.readAsArrayBuffer(rawFile);
        });
      }

      async function upload(rawFile: File) {
        const inputRefDom = unref(inputRef);
        if (inputRefDom) {
          // fix can't select the same excel
          inputRefDom.value = '';
        }
        await readerData(rawFile);
      }

      /**
       * @description: Trigger select file manager
       */
      function handleInputClick(e: Event) {
        const files = e && (e.target as HTMLInputElement).files;
        const rawFile = files && files[0]; // only setting files[0]
        if (!rawFile) return;
        upload(rawFile);
      }

      /**
       * @description: Click the upload button
       */
      function handleUpload() {
        const inputRefDom = unref(inputRef);
        inputRefDom && inputRefDom.click();
      }

      return { handleUpload, handleInputClick, inputRef };
    },
  });
</script>
