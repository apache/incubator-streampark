let baseUrl = ''
switch (process.env.NODE_ENV) {
  case 'development':
    baseUrl = 'http://localhost:10001/api'
    break
  case 'production':
    baseUrl = 'http://10.2.39.89:10001/api'
    break
}
export default baseUrl
