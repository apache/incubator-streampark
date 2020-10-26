let baseUrl = ''
switch (process.env.NODE_ENV) {
  case 'development':
    baseUrl = '/api'
    break
  case 'production':
    baseUrl = 'http://localhost:10001'
    break
}
export default baseUrl
