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

const options = [
  {
    opt: '-m',
    key: 'jobmanager',
    name: 'jobmanager',
    placeholder: '-m,--jobmanager <arg>',
    description: 'JobManager address (yarn-cluster)',
    group: 'run',
    type: 'input',
    defaultValue: '',
    validator: (__rule, value, callback) => {
      if (!value || value.length === 0) {
        callback(new Error('JobManager is require or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  {
    opt: '-z',
    key: 'zookeeperNamespace',
    name: 'zookeeperNamespace',
    placeholder: '-z,--zookeeperNamespace <arg>',
    description: 'Namespace to create the Zookeeper sub-paths for high availability mode',
    group: 'no-support',
    type: 'input',
    defaultValue: '',
    validator: (__rule, value, callback) => {
      if (!value || value.length === 0) {
        callback(new Error('zookeeperNamespace is require or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  {
    opt: '-d',
    key: 'detached',
    name: 'detached',
    placeholder: '-d,--detached',
    description: 'If present, runs the job in detached mode',
    group: 'no-support',
    type: 'switch',
  },
  {
    opt: '-sae',
    key: 'shutdownOnAttachedExit',
    name: 'shutdownOnAttachedExit',
    placeholder: '-sae,--shutdownOnAttachedExit',
    description:
      'If the task submitted in non-standalone mode, when the client is interrupted, the job task executed by the cluster will also shutdown',
    group: 'no-support',
    type: 'switch',
    defaultValue: false,
  },
  // --------------------total-memory--------------------
  {
    key: 'jobmanager_memory_flink_size',
    name: 'jobmanager.memory.flink.size',
    placeholder: 'Total Flink Memory size for the JobManage',
    description: 'JobManager Flink total memory size',
    unit: 'mb',
    group: 'total-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    defaultValue: 1024,
    validator: (__rule, value, callback) => {
      if (!value) {
        callback(new Error('flink.size is require or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  {
    key: 'taskmanager_memory_flink_size',
    name: 'taskmanager.memory.flink.size',
    placeholder: 'Total Flink Memory size for the TaskExecutors',
    description: 'TaskExecutor Flink total memory size',
    unit: 'mb',
    group: 'total-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    defaultValue: 1024,
    validator: (_rule, value, callback) => {
      if (!value) {
        callback(new Error('flink.size is required or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  // --------------------process-memory--------------------
  {
    key: 'jobmanager_memory_process_size',
    name: 'jobmanager.memory.process.size',
    placeholder: 'Total Process Memory size for the JobManager',
    description: 'The total memory size of the JobManager process',
    unit: 'mb',
    group: 'process-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    defaultValue: 1024,
    validator: (_rule, value, callback) => {
      if (!value) {
        callback(new Error('process.size is required or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  {
    key: 'taskmanager_memory_process_size',
    name: 'taskmanager.memory.process.size',
    placeholder: 'Total Process Memory size for the TaskExecutors',
    description: 'The total memory size of the TaskExecutor process',
    unit: 'mb',
    group: 'process-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    defaultValue: 1024,
    validator: (_rule, value, callback) => {
      if (!value) {
        callback(new Error('process.size is required or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  // ------------------------------------------------------- jobmanager-memory -------------------------------------------------------
  {
    key: 'jobmanager_memory_heap_size',
    name: 'jobmanager.memory.heap.size',
    placeholder: 'JVM Heap Memory size for JobManager',
    description: 'JobManager is JVM heap memory, the recommended size is 128mb',
    unit: 'mb',
    group: 'jobmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    defaultValue: 128,
    validator: (_rule, value, callback) => {
      if (!value) {
        callback(new Error('heap.size is require or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  {
    key: 'jobmanager_memory_off_heap_size',
    name: 'jobmanager.memory.off-heap.size',
    placeholder: 'Off-heap Memory size for JobManager',
    description: 'JobManager is off-heap memory (direct or local)',
    unit: 'mb',
    group: 'jobmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    defaultValue: 128,
    validator: (_rule, value, callback) => {
      if (!value) {
        callback(new Error('off-heap.size is required or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  {
    key: 'jobmanager_memory_jvm_metaspace_size',
    name: 'jobmanager.memory.jvm-metaspace.size',
    placeholder: 'JVM Metaspace Size for the JobManager',
    description: 'JVM Metaspace Size for the JobManager',
    unit: 'mb',
    group: 'jobmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    defaultValue: 256,
    validator: (_rule, value, callback) => {
      if (!value) {
        callback(new Error('jvm-metaspace.size is require or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  {
    key: 'jobmanager_memory_jvm_overhead_fraction',
    name: 'jobmanager.memory.jvm-overhead.fraction',
    placeholder: 'Fraction of Total Process Memory to be reserved for JVM Overhead',
    description:
      'JobManager is other JVM overhead (such as stack space, garbage collection space) accounts for the total memory of the process',
    unit: null,
    group: 'jobmanager-memory',
    type: 'number',
    min: 0.1,
    max: 1,
    step: 0.1,
    defaultValue: 0.1,
    validator: (_rule, value, callback) => {
      if (!value) {
        callback(new Error('jvm-overhead.fraction is require or you can delete this option.'));
      } else {
        callback();
      }
    },
  },
  {
    key: 'jobmanager_memory_jvm_overhead_max',
    name: 'jobmanager.memory.jvm-overhead.max',
    placeholder: 'Max JVM Overhead size for the JobManager',
    description:
      'Maximum memory for JobManager other JVM overhead (such as stack space, garbage collection space)',
    unit: 'mb',
    group: 'jobmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    defaultValue: 1024,
    validator: (_rule, value, callback) => {
      if (!value) {
        callback(new Error('jvm-overhead.max is required or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  {
    key: 'jobmanager_memory_jvm_overhead_min',
    name: 'jobmanager.memory.jvm.overhead.min',
    placeholder: 'Min JVM Overhead size for the JobManager',
    description:
      'Minimum memory for JobManager other JVM overhead (such as stack space, garbage collection space)',
    unit: 'mb',
    group: 'jobmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    defaultValue: 192,
    validator: (_rule, value, callback) => {
      if (!value) {
        callback(new Error('jvm-overhead.min is required or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  // ------------------------------------------------------- taskmanager-memory -------------------------------------------------------
  {
    key: 'taskmanager_memory_framework_heap_size',
    name: 'taskmanager.memory.framework.heap.size',
    placeholder: 'Framework Heap Memory size for TaskExecutors',
    description:
      'Framework heap memory - JVM heap memory for the Flink framework (not recommended for adjustment, advanced configuration)',
    unit: 'mb',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    defaultValue: null,
    validator: (_rule, value, callback) => {
      if (!value) {
        callback(new Error('framework.heap.size is required or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  {
    key: 'taskmanager_memory_framework_off_heap_size',
    name: 'taskmanager.memory.framework.off-heap.size',
    placeholder: 'Framework Off-Heap Memory size for TaskExecutors',
    description:
      'Framework off-heap memory - off-heap memory for the Flink framework (not recommended for adjustment, advanced configuration)',
    unit: 'mb',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    defaultValue: null,
    validator: (_rule, value, callback) => {
      if (!value) {
        callback(new Error('framework.off-heap.size is required or you can delete this option'));
      } else {
        callback();
      }
    },
  },

  {
    key: 'taskmanager_memory_task_heap_size',
    name: 'taskmanager.memory.task.heap.size',
    placeholder: 'Task Heap Memory size for TaskExecutors',
    description:
      'Task heap memory - JVM heap memory for operators and user code of Flink applications',
    unit: 'mb',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    defaultValue: 128,
    validator: (_rule, value, callback) => {
      if (!value) {
        callback(new Error('task.heap.size is required or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  {
    key: 'taskmanager_memory_task_off_heap_size',
    name: 'taskmanager.memory.task.off-heap.size',
    placeholder: 'Task Off-Heap Memory size for TaskExecutors',
    description:
      'Task off-heap memory - off-heap memory for Flink operators and user code (not recommended for adjustment, advanced configuration)',
    unit: 'mb',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    defaultValue: null,
    validator: (_rule, value, callback) => {
      if (!value) {
        callback(new Error('task.off-heap.size is required or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  {
    key: 'taskmanager_memory_managed_size',
    name: 'taskmanager.memory.managed.size',
    placeholder: 'Managed Memory size for TaskExecutors',
    description:
      'Managed memory - size of memory managed by Flink for (sort|cache intermediate results|StateBackend)',
    unit: 'mb',
    group: 'taskmanager-memory',
    type: 'number',
    min: 0,
    max: 102400,
    step: 1,
    defaultValue: 128,
    validator: (_rule, value, callback) => {
      if (value === undefined || value === null) {
        callback(new Error('managed.size is required or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  {
    key: 'taskmanager_memory_managed_fraction',
    name: 'taskmanager.memory.managed.fraction',
    placeholder: 'Min JVM Overhead size for the TaskExecutors',
    description:
      'Managed memory - percentage of memory managed by Flink for (sort|cache intermediate results|StateBackend)',
    unit: null,
    group: 'taskmanager-memory',
    type: 'number',
    min: 0.1,
    max: 1,
    step: 0.1,
    defaultValue: 0.4,
    validator: (_rule, value, callback) => {
      if (value === undefined || value === null) {
        callback(new Error('managed.fraction is required or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  {
    key: 'taskmanager_memory_jvm_metaspace_size',
    name: 'taskmanager.memory.jvm-metaspace.size',
    placeholder: 'JVM Metaspace Size for the TaskExecutors',
    description: 'JVM Metaspace Size for the TaskExecutors',
    unit: 'mb',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    defaultValue: 256,
    validator: (_rule, value, callback) => {
      if (!value) {
        callback(new Error('jvm-metaspace.size is required or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  {
    key: 'taskmanager_memory_jvm_overhead_fraction',
    name: 'taskmanager.memory.jvm-overhead.fraction',
    placeholder: 'Fraction of Total Process Memory to be reserved for JVM Overhead',
    description:
      'Other JVM overheads of TaskExecutor (such as stack space, garbage collection space) account for the total memory of the process',
    unit: null,
    group: 'taskmanager-memory',
    type: 'number',
    min: 0.1,
    max: 1,
    step: 0.1,
    defaultValue: 0.1,
    validator: (_rule, value, callback) => {
      if (!value) {
        callback(new Error('jvm-overhead.fraction is required or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  {
    key: 'taskmanager_memory_jvm_overhead_max',
    name: 'taskmanager.memory.jvm-overhead.max',
    placeholder: 'Max JVM Overhead size for the TaskExecutors',
    description:
      'Maximum memory for TaskExecutor is other JVM overhead (such as stack space, garbage collection space)',
    unit: 'mb',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    defaultValue: 1024,
    validator: (_rule, value, callback) => {
      if (!value) {
        callback(new Error('jvm-overhead.max is required or you can delete this option'));
      } else {
        callback();
      }
    },
  },
  {
    key: 'taskmanager_memory_jvm_overhead_min',
    name: 'taskmanager.memory.jvm-overhead.min',
    placeholder: 'Min JVM Overhead size for the TaskExecutors',
    description:
      'Minimum memory for TaskExecutor is other JVM overhead (such as stack space, garbage collection space)',
    unit: 'mb',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    defaultValue: 192,
    validator: (_rule, value, callback) => {
      if (!value) {
        callback(new Error('jvm-overhead.min is required or you can delete this option'));
      } else {
        callback();
      }
    },
  },
];

const optionsKeyMapping = new Map<string, Recordable>();
const optionsValueMapping = new Map<string, string>();
options.forEach((item) => {
  optionsKeyMapping.set(item.key, item);
  optionsValueMapping.set(item.name, item.key);
});

export default options;

export { optionsKeyMapping, optionsValueMapping };
