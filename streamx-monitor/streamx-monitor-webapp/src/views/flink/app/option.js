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
    key: '-n',
    name: 'allowNonRestoredState',
    placeholder: '-n,--allowNonRestoredState <arg>',
    description: 'Allow to skip savepoint state that cannot be restored',
    group: 'run',
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
  // ------------------------------------------------------- yarn-cluster -------------------------------------------------------
  {
    key: '-yjm',
    name: 'yarnjobManagerMemory',
    placeholder: '-yjm,--yarnjobManagerMemory <arg>',
    description: 'JobManager内存大小 (单位: MB)',
    group: 'yarn-cluster',
    type: 'number',
    min: 1024,
    value: '',
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('JobManager is require or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: '-ytm',
    name: 'yarntaskManagerMemory',
    placeholder: '-ytm,--yarntaskManagerMemory <arg>',
    description: 'TaskManager内存大小 (单位: MB)',
    group: 'yarn-cluster',
    type: 'number',
    min: 1024,
    value: '',
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('TaskManager is require or you can delete this option'))
      } else {
        callback()
      }
    }
  },

  {
    key: '-yat',
    name: 'yarnapplicationType',
    placeholder: '-yat,--yarnapplicationType <arg>',
    group: 'yarn-cluster',
    type: 'input',
    value: '',
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('yarnapplicationType is require or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: '-ynl',
    name: 'yarnnodeLabel',
    placeholder: '-ynl,--yarnnodeLabel <arg>',
    description: 'Specify YARN node label for the YARN application',
    group: 'yarn-cluster',
    type: 'input',
    value: '',
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('yarnnodeLabel is require or you can delete this option.'))
      } else {
        callback()
      }
    }
  },
  {
    key: '-yqu',
    name: 'yarnqueue',
    placeholder: '-yqu,--yarnqueue <arg> ',
    group: 'yarn-cluster',
    type: 'input',
    description: '指定应用的运行队列(on YARN)',
    value: '',
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('yarnqueue is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: '-yz',
    name: 'yarnzookeeperNamespace',
    placeholder: '-yz,--yarnzookeeperNamespace <arg>',
    description: 'Namespace to create the Zookeeper sub-paths for high availability mode',
    group: 'yarn-cluster',
    type: 'input',
    value: '',
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('yarnzookeeperNamespace is required or you can delete this option'))
      } else {
        callback()
      }
    }
  },
  {
    key: '-yq',
    name: 'yarnquery',
    placeholder: '-yq,--yarnquery',
    description: '显示YARN上可用的资源(memory, cores)',
    group: 'yarn-cluster',
    type: 'switch',
    value: '',
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('yarnquery is required or you can delete this option'))
      } else {
        callback()
      }
    }
  }
]
