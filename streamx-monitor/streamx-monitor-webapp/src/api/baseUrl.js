let baseUrl = ''
switch (process.env.NODE_ENV) {
  case 'development':
    baseUrl = '/api'
    break
  case 'production':
    baseUrl = 'http://10.2.39.89:10001'
    break
}
export default baseUrl
