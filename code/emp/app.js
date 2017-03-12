var express = require('express'), https = require('https'), path = require('path'), app = express(), fs = require('fs'), router = express.Router();
var morgan = require('morgan');
var bodyParser = require('body-parser');
var dotenv = require('dotenv');
var routes = require('./routes');
dotenv.load();

// Log timestamp, user agent, query parameters, and additional http action info (method, url, status)
morgan.token('params1', function(req, res) {
	return JSON.stringify(req.body)
})
morgan.token('params2', function(req, res) {
	return JSON.stringify(req.query)
})
app.use(morgan('[:date[clf]] | :method :url :status | User-agent: :user-agent | Body: :params1| Query: :params2'));

// Set SSL key and certificate
var sslOptions = {
	key : fs.readFileSync('ssl/key.pem'),
	cert : fs.readFileSync('ssl/cert.pem'),
	passphrase : '4145'
}

// Configure app
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({
	extended : false
}));
app.set('views', __dirname + '/views');
app.set('view engine', 'ejs');
app.set('jwtSecret', process.env.SECRET);
app.use(express.static(path.join(__dirname, 'public')));
console.log(path.join(__dirname, 'public'));
process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0"; // avoid error with self-signed certificate

app.use('/', routes);

// Start HTTPS server
var server = https.createServer(sslOptions, app).listen(process.env.PORT || 3000, function() {
	console.log("Server running on port " + server.address().port);
});
