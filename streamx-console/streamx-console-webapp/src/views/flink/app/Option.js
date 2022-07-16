/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
export default [
    {
        opt: '-m',
        key: 'jobmanager',
        name: 'jobmanager',
        placeholder: '-m,--jobmanager <arg>',
        description: 'JobManager 地址(yarn-cluster)',
        group: 'run',
        type: 'input',
        defaultValue: '',
        validator: (rule, value, callback) => {
            if (!value || value.length === 0) {
                callback(new Error('JobManager is require or you can delete this option'))
            } else {
                callback()
            }
        }
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
        validator: (rule, value, callback) => {
            if (!value || value.length === 0) {
                callback(new Error('zookeeperNamespace is require or you can delete this option'))
            } else {
                callback()
            }
        }
    },
    {
        opt: '-d',
        key: 'detached',
        name: 'detached',
        placeholder: '-d,--detached',
        description: 'If present, runs the job in detached mode',
        group: 'no-support',
        type: 'switch',
        validator: (rule, value, callback) => {
            callback()
        }
    },
    {
        opt: '-sae',
        key: 'shutdownOnAttachedExit',
        name: 'shutdownOnAttachedExit',
        placeholder: '-sae,--shutdownOnAttachedExit',
        description: '如果非独立模式提交的任务,当客户端中断,集群执行的job任务也会shutdown',
        group: 'no-support',
        type: 'switch',
        defaultValue: false,
        validator: (rule, value, callback) => {
            callback()
        }
    },
    // --------------------total-memory--------------------
    {
        key: 'jobmanager_memory_flink_size',
        name: 'jobmanager.memory.flink.size',
        placeholder: 'Total Flink Memory size for the JobManage',
        description: 'JobManager Flink总内存大小',
        unit: 'mb',
        group: 'total-memory',
        type: 'number',
        min: 1,
        max: 102400,
        step: 1,
        defaultValue: 1024,
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('flink.size is require or you can delete this option'))
            } else {
                callback()
            }
        }
    },
    {
        key: 'taskmanager_memory_flink_size',
        name: 'taskmanager.memory.flink.size',
        placeholder: 'Total Flink Memory size for the TaskExecutors',
        description: 'TaskExecutor Flink总内存大小',
        unit: 'mb',
        group: 'total-memory',
        type: 'number',
        min: 1,
        max: 102400,
        step: 1,
        defaultValue: 1024,
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('flink.size is required or you can delete this option'))
            } else {
                callback()
            }
        }
    },
    // --------------------process-memory--------------------
    {
        key: 'jobmanager_memory_process_size',
        name: 'jobmanager.memory.process.size',
        placeholder: 'Total Process Memory size for the JobManager',
        description: 'JobManager 进程总内存大小',
        unit: 'mb',
        group: 'process-memory',
        type: 'number',
        min: 1,
        max: 102400,
        step: 1,
        defaultValue: 1024,
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('process.size is required or you can delete this option'))
            } else {
                callback()
            }
        }
    },
    {
        key: 'taskmanager_memory_process_size',
        name: 'taskmanager.memory.process.size',
        placeholder: 'Total Process Memory size for the TaskExecutors',
        description: 'TaskExecutor 进程总内存大小',
        unit: 'mb',
        group: 'process-memory',
        type: 'number',
        min: 1,
        max: 102400,
        step: 1,
        defaultValue: 1024,
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('process.size is required or you can delete this option'))
            } else {
                callback()
            }
        }
    },
    // ------------------------------------------------------- jobmanager-memory -------------------------------------------------------
    {
        key: 'jobmanager_memory_heap_size',
        name: 'jobmanager.memory.heap.size',
        placeholder: 'JVM Heap Memory size for JobManager',
        description: 'JobManager 的 JVM 堆内存,推荐大小128mb',
        unit: 'mb',
        group: 'jobmanager-memory',
        type: 'number',
        min: 1,
        max: 102400,
        step: 1,
        defaultValue: 128,
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('heap.size is require or you can delete this option'))
            } else {
                callback()
            }
        }
    },
    {
        key: 'jobmanager_memory_off_heap_size',
        name: 'jobmanager.memory.off-heap.size',
        placeholder: 'Off-heap Memory size for JobManager',
        description: 'JobManager 的堆外内存(直接内存或本地内存)',
        unit: 'mb',
        group: 'jobmanager-memory',
        type: 'number',
        min: 1,
        max: 102400,
        step: 1,
        defaultValue: 128,
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('off-heap.size is required or you can delete this option'))
            } else {
                callback()
            }
        }
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
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('jvm-metaspace.size is require or you can delete this option'))
            } else {
                callback()
            }
        }
    },
    {
        key: 'jobmanager_memory_jvm_overhead_fraction',
        name: 'jobmanager.memory.jvm-overhead.fraction',
        placeholder: 'Fraction of Total Process Memory to be reserved for JVM Overhead',
        description: 'JobManager其他JVM开销(如栈空间,垃圾回收空间)于进程总内存占比',
        unit: null,
        group: 'jobmanager-memory',
        type: 'number',
        min: 0.1,
        max: 1,
        step: 0.1,
        defaultValue: 0.1,
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('jvm-overhead.fraction is require or you can delete this option.'))
            } else {
                callback()
            }
        }
    },
    {
        key: 'jobmanager_memory_jvm_overhead_max',
        name: 'jobmanager.memory.jvm-overhead.max',
        placeholder: 'Max JVM Overhead size for the JobManager',
        description: 'JobManager其他JVM开销(如栈空间,垃圾回收空间)的最大内存',
        unit: 'mb',
        group: 'jobmanager-memory',
        type: 'number',
        min: 1,
        max: 102400,
        step: 1,
        defaultValue: 1024,
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('jvm-overhead.max is required or you can delete this option'))
            } else {
                callback()
            }
        }
    },
    {
        key: 'jobmanager_memory_jvm_overhead_min',
        name: 'jobmanager.memory.jvm.overhead.min',
        placeholder: 'Min JVM Overhead size for the JobManager',
        description: 'JobManager其他JVM开销(如栈空间,垃圾回收空间)的最小内存',
        unit: 'mb',
        group: 'jobmanager-memory',
        type: 'number',
        min: 1,
        max: 102400,
        step: 1,
        defaultValue: 192,
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('jvm-overhead.min is required or you can delete this option'))
            } else {
                callback()
            }
        }
    },
    // ------------------------------------------------------- taskmanager-memory -------------------------------------------------------
    {
        key: 'taskmanager_memory_framework_heap_size',
        name: 'taskmanager.memory.framework.heap.size',
        placeholder: 'Framework Heap Memory size for TaskExecutors',
        description: '框架堆内存-用于Flink框架的JVM堆内存 (不建议调整,进阶配置)',
        unit: 'mb',
        group: 'taskmanager-memory',
        type: 'number',
        min: 1,
        max: 102400,
        step: 1,
        defaultValue: null,
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('framework.heap.size is required or you can delete this option'))
            } else {
                callback()
            }
        }
    },
    {
        key: 'taskmanager_memory_framework_off_heap_size',
        name: 'taskmanager.memory.framework.off-heap.size',
        placeholder: 'Framework Off-Heap Memory size for TaskExecutors',
        description: '框架堆外内存-用于Flink框架的堆外内存 (不建议调整,进阶配置)',
        unit: 'mb',
        group: 'taskmanager-memory',
        type: 'number',
        min: 1,
        max: 102400,
        step: 1,
        defaultValue: null,
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('framework.off-heap.size is required or you can delete this option'))
            } else {
                callback()
            }
        }
    },

    {
        key: 'taskmanager_memory_task_heap_size',
        name: 'taskmanager.memory.task.heap.size',
        placeholder: 'Task Heap Memory size for TaskExecutors',
        description: '任务堆内存-用于Flink应用的算子及用户代码的JVM堆内存',
        unit: 'mb',
        group: 'taskmanager-memory',
        type: 'number',
        min: 1,
        max: 102400,
        step: 1,
        defaultValue: 128,
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('task.heap.size is required or you can delete this option'))
            } else {
                callback()
            }
        }
    },
    {
        key: 'taskmanager_memory_task_off_heap_size',
        name: 'taskmanager.memory.task.off-heap.size',
        placeholder: 'Task Off-Heap Memory size for TaskExecutors',
        description: '任务堆外内存-用于Flink算子及用户代码的堆外内存(不建议调整,进阶配置)',
        unit: 'mb',
        group: 'taskmanager-memory',
        type: 'number',
        min: 1,
        max: 102400,
        step: 1,
        defaultValue: null,
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('task.off-heap.size is required or you can delete this option'))
            } else {
                callback()
            }
        }
    },
    {
        key: 'taskmanager_memory_managed_size',
        name: 'taskmanager.memory.managed.size',
        placeholder: 'Managed Memory size for TaskExecutors',
        description: '托管内存-由Flink管理用于(排序|缓存中间结果|StateBackend)的内存大小',
        unit: 'mb',
        group: 'taskmanager-memory',
        type: 'number',
        min: 0,
        max: 102400,
        step: 1,
        defaultValue: 128,
        validator: (rule, value, callback) => {
          if (value === undefined || value === null) {
                callback(new Error('managed.size is required or you can delete this option'))
            } else {
                callback()
            }
        }
    },
    {
        key: 'taskmanager_memory_managed_fraction',
        name: 'taskmanager.memory.managed.fraction',
        placeholder: 'Min JVM Overhead size for the TaskExecutors',
        description: '托管内存-由Flink管理用于(排序|缓存中间结果|StateBackend)的内存占比',
        unit: null,
        group: 'taskmanager-memory',
        type: 'number',
        min: 0.1,
        max: 1,
        step: 0.1,
        defaultValue: 0.4,
        validator: (rule, value, callback) => {
          if (value === undefined || value === null) {
                callback(new Error('managed.fraction is required or you can delete this option'))
            } else {
                callback()
            }
        }
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
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('jvm-metaspace.size is required or you can delete this option'))
            } else {
                callback()
            }
        }
    },
    {
        key: 'taskmanager_memory_jvm_overhead_fraction',
        name: 'taskmanager.memory.jvm-overhead.fraction',
        placeholder: 'Fraction of Total Process Memory to be reserved for JVM Overhead',
        description: 'TaskExecutor的其他JVM开销(如栈空间,垃圾回收空间)于进程总内存占比',
        unit: null,
        group: 'taskmanager-memory',
        type: 'number',
        min: 0.1,
        max: 1,
        step: 0.1,
        defaultValue: 0.1,
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('jvm-overhead.fraction is required or you can delete this option'))
            } else {
                callback()
            }
        }
    },
    {
        key: 'taskmanager_memory_jvm_overhead_max',
        name: 'taskmanager.memory.jvm-overhead.max',
        placeholder: 'Max JVM Overhead size for the TaskExecutors',
        description: 'TaskExecutor的其他JVM开销(如栈空间,垃圾回收空间)的最大内存',
        unit: 'mb',
        group: 'taskmanager-memory',
        type: 'number',
        min: 1,
        max: 102400,
        step: 1,
        defaultValue: 1024,
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('jvm-overhead.max is required or you can delete this option'))
            } else {
                callback()
            }
        }
    },
    {
        key: 'taskmanager_memory_jvm_overhead_min',
        name: 'taskmanager.memory.jvm-overhead.min',
        placeholder: 'Min JVM Overhead size for the TaskExecutors',
        description: 'TaskExecutor的其他JVM开销(如栈空间,垃圾回收空间)的最小内存',
        unit: 'mb',
        group: 'taskmanager-memory',
        type: 'number',
        min: 1,
        max: 102400,
        step: 1,
        defaultValue: 192,
        validator: (rule, value, callback) => {
            if (!value) {
                callback(new Error('jvm-overhead.min is required or you can delete this option'))
            } else {
                callback()
            }
        }
    }

]
