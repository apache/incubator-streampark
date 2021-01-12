export default [
  {
    key: '-m',
    name: 'jobmanager',
    placeholder: '-m,--jobmanager <arg>',
    description: 'JobManager 地址(yarn-cluster)',
    group: 'run',
    type: 'input',
    value: '',
    validator: (rule, value, callback) => {
      if (!value || value.length === 0) {
        callback(new Error('JobManager is require or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: '-z',
    name: 'zookeeperNamespace',
    placeholder: '-z,--zookeeperNamespace <arg>',
    description: 'Namespace to create the Zookeeper sub-paths for high availability mode',
    group: 'no-support',
    type: 'input',
    value: '',
    validator: (rule, value, callback) => {
      if (!value || value.length === 0) {
        callback(new Error('zookeeperNamespace is require or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: '-d',
    name: 'detached',
    placeholder: '-d,--detached',
    description: 'If present, runs the job in detached mode',
    group: 'no-support',
    type: 'switch',
    value: false,
    validator: (rule, value, callback) => {
      callback()
    }
  },
  {
    key: '-sae',
    name: 'shutdownOnAttachedExit',
    placeholder: '-sae,--shutdownOnAttachedExit',
    description: '如果非独立模式提交的任务,当客户端中断,集群执行的job任务也会shutdown',
    group: 'no-support',
    type: 'switch',
    value: false,
    validator: (rule, value, callback) => {
      callback()
    }
  },
  // ------------------------------------------------------- jobmanager-memory -------------------------------------------------------
  {
    key: 'jobmanager.memory.flink.size',
    placeholder: 'Total Flink Memory size for the JobManage',
    description: 'JobManager总内存大小 (单位: MB)',
    group: 'jobmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 1024,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('jobmanager.memory.flink.size is require or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'jobmanager.memory.heap.size',
    placeholder: 'JVM Heap Memory size for JobManager',
    description: 'JobManager Heap,recommended JVM Heap size is 128.000mb (134217728 bytes)',
    group: 'jobmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 128,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('jobmanager.memory.heap.size is require or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'jobmanager.memory.jvm-metaspace.size',
    placeholder: 'JVM Metaspace Size for the JobManage',
    group: 'jobmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 256,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('jobmanager.memory.jvm-metaspace.size is require or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'jobmanager.memory.jvm-overhead.fraction',
    placeholder: 'Fraction of Total Process Memory to be reserved for JVM Overhead',
    description: 'Fraction of Total Process Memory to be reserved for JVM Overhead',
    group: 'jobmanager-memory',
    type: 'number',
    min: 0.1,
    max: 1,
    step: 0.1,
    value: 0.1,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('yarnnodeLabel is require or you can delete this option.'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'jobmanager.memory.jvm-overhead.max',
    placeholder: 'Max JVM Overhead size for the JobManager',
    group: 'jobmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 1024,
    description: 'Max JVM Overhead size for the JobManager',
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('jobmanager.memory.jvm-overhead.max is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'jobmanager.memory.jvm-overhead.min',
    placeholder: 'Min JVM Overhead size for the JobManager',
    description: 'Min JVM Overhead size for the JobManager',
    group: 'jobmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 192,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('jobmanager.memory.jvm-overhead.min is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'jobmanager.memory.off-heap.size',
    placeholder: 'Off-heap Memory size for JobManager',
    description: 'Off-heap Memory size for JobManager',
    group: 'jobmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 128,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('jobmanager.memory.off-heap.size is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'jobmanager.memory.process.size',
    placeholder: 'Total Process Memory size for the JobManager',
    description: 'Total Process Memory size for the JobManager',
    group: 'jobmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 1024,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('jobmanager.memory.process.size is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  // ------------------------------------------------------- taskmanager-memory -------------------------------------------------------
  {
    key: 'taskmanager.memory.flink.size',
    placeholder: 'Total Flink Memory size for the TaskExecutors',
    description: 'Total Flink Memory size for the TaskExecutors',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 1024,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('taskmanager.memory.flink.size is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'taskmanager.memory.framework.heap.size',
    placeholder: 'Framework Heap Memory size for TaskExecutors',
    description: 'Framework Heap Memory size for TaskExecutors',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 128,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('taskmanager.memory.framework.heap.size is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'taskmanager.memory.framework.off-heap.size',
    placeholder: 'Framework Off-Heap Memory size for TaskExecutors',
    description: 'Framework Off-Heap Memory size for TaskExecutors',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 128,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('taskmanager.memory.framework.off-heap.size is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'taskmanager.memory.jvm-metaspace.size',
    placeholder: 'JVM Metaspace Size for the TaskExecutors',
    description: 'JVM Metaspace Size for the TaskExecutors',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 256,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('taskmanager.memory.jvm-metaspace.size is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'taskmanager.memory.jvm-overhead.fraction',
    placeholder: 'Fraction of Total Process Memory to be reserved for JVM Overhead',
    description: 'Fraction of Total Process Memory to be reserved for JVM Overhead',
    group: 'taskmanager-memory',
    type: 'number',
    min: 0.1,
    max: 1,
    step: 0.1,
    value: 0.1,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('taskmanager.memory.jvm-overhead.fraction is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'taskmanager.memory.jvm-overhead.max',
    placeholder: 'Max JVM Overhead size for the TaskExecutors',
    description: 'Max JVM Overhead size for the TaskExecutors',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 1024,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('taskmanager.memory.jvm-overhead.max is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'taskmanager.memory.jvm-overhead.min',
    placeholder: 'Min JVM Overhead size for the TaskExecutors',
    description: 'Min JVM Overhead size for the TaskExecutors',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 192,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('taskmanager.memory.jvm-overhead.min is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'taskmanager.memory.managed.fraction',
    placeholder: 'Min JVM Overhead size for the TaskExecutors',
    description: 'Min JVM Overhead size for the TaskExecutors',
    group: 'taskmanager-memory',
    type: 'number',
    min: 0.1,
    max: 1,
    step: 0.1,
    value: 0.4,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('taskmanager.memory.managed.fraction is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'taskmanager.memory.managed.size',
    placeholder: 'Managed Memory size for TaskExecutors',
    description: 'Managed Memory size for TaskExecutors',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 128,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('taskmanager.memory.managed.size is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'taskmanager.memory.network.fraction',
    placeholder: 'Fraction of Total Flink Memory to be used as Network Memory',
    description: 'Fraction of Total Flink Memory to be used as Network Memory',
    group: 'taskmanager-memory',
    type: 'number',
    min: 0.1,
    max: 1,
    step: 0.1,
    value: 0.1,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('taskmanager.memory.network.fraction is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'taskmanager.memory.network.max',
    placeholder: 'Max Network Memory size for TaskExecutors',
    description: 'Max Network Memory size for TaskExecutors',
    group: 'taskmanager-memory',
    type: 'number',
    min: 64,
    max: 102400,
    step: 1,
    value: 1024,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('taskmanager.memory.network.max is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'taskmanager.memory.network.min',
    placeholder: 'Min Network Memory size for TaskExecutors',
    description: 'Min Network Memory size for TaskExecutors',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 64,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('taskmanager.memory.network.min is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'taskmanager.memory.process.size',
    placeholder: 'Total Process Memory size for the TaskExecutors',
    description: 'Total Process Memory size for the TaskExecutors',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 1024,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('taskmanager.memory.process.size is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'taskmanager.memory.task.heap.size',
    placeholder: 'Task Heap Memory size for TaskExecutors',
    description: 'Task Heap Memory size for TaskExecutors',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 128,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('taskmanager.memory.task.heap.size is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: 'taskmanager.memory.task.off-heap.size',
    placeholder: 'Task Off-Heap Memory size for TaskExecutors',
    description: 'Task Off-Heap Memory size for TaskExecutors',
    group: 'taskmanager-memory',
    type: 'number',
    min: 1,
    max: 102400,
    step: 1,
    value: 0,
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('taskmanager.memory.task.off-heap.size is required or you can delete this option'))
      } else {
        callback()
      }
    }
  }

]
