const plugins = []
plugins.push(['import', {
  'libraryName': 'Antd',
  'libraryDirectory': 'es',
  'style': true // `style: true` 会加载 less 文件
}])

module.exports = {
  presets: [
    '@vue/cli-plugin-babel/preset',
    [
      '@babel/preset-env',
      {
        'useBuiltIns': 'entry',
        'corejs': 3
      }
    ]
  ],
  plugins,
  'env': {
    'production': {
      'plugins': ['transform-remove-console']
    }
  }
}
