require('dotenv').config();

module.exports = {
  mongodb: {
    connectionString: process.env.MONGODB_URI,
  },
  site: {
    baseUrl: '/',
    cookieKeyName: 'mongo-express',
    cookieSecret: 'cookiesecret',
    host: 'localhost',
    port: 8081,
    requestSizeLimit: '50mb',
    sessionSecret: 'sessionsecret',
    sslEnabled: false,
    sslCert: '',
    sslKey: '',
  },
  useBasicAuth: false,
  basicAuth: {
    username: 'admin',
    password: 'pass',
  },
  options: {
    console: true,
    documentsPerPage: 10,
    editorTheme: 'rubyblue',
    maxPropSize: 100 * 1000,
    noDelete: false,
    noExport: false,
  },
};

