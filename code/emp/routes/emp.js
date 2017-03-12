// Interactions with the EMP web service/DB

var jwt = require("jwt-simple");
var express = require("express"), router = express.Router();
var request = require("request");
var AWS = require("aws-sdk");
AWS.config.update({
	region : "us-west-2",
	endpoint : "https://dynamodb.us-west-2.amazonaws.com"
});
require("dotenv").load();
var docClient = new AWS.DynamoDB.DocumentClient();
var currStep;

const tableName = "EMP";	// Employee table
const authTableName = "EMPauth";	// Employee authentication table
const mbrPath = process.env.MBR_PATH + "/employer";
const logPathStart = process.env.LOG_PATH + "/start";
const logPathEnd = process.env.LOG_PATH + "/end";

// Send success JSON
function sendSuccess(res, data){
	sendLog((currStep + " success: true"), false);
	res.status(200).json({
		success: true,
		data: data
	});
}

// Send error JSON
function sendErr(res, err) {
	var msg = currStep + " success: false | error msg: " + err;
	sendLog(msg, false);
	res.status(500).json({
		success: false,
		error: err
	});
}

// Authenticate the user
router.post("/authenticate", function(req, res){
	currStep = "Authenticating user"
	sendLog(currStep, true, req.body)
	var params = {
		TableName : authTableName,
		Key : {
			"id" : parseInt(req.body.id),
		},
	};
	getItem(res, params, function(data){
		if(data.password == req.body.password){
			var token = jwt.encode({
			  id: req.body.id
			}, process.env.SECRET);
			sendSuccess(res, {token: token});
		}
		else{
			sendErr(res, "Incorrect password");
		}
	});
});


// Send info to MBR web service
router.post("/mortgage", getToken, function(req, res){
	currStep = "Sending information to MBR service"
	sendLog(currStep, true, req.body)
	delete req.user.id;
	req.user.mortId = req.body.mortId;
	request({url: mbrPath, method:"POST", json:req.user}, function(err, response, body){
		if (err || response.statusCode != 200) {
			sendErr(res, "Error recieved from MBR");
		} else {
			sendSuccess(res);
		}
	});

});

// Get item in database
function getItem(res, params, callback){
	docClient.get(params, function(err, data) {
		if (!data || !data.Item){
			sendErr(res, "Item not found");
		}
		else if (err) {
			sendErr(res, err);
		} else {
			callback(data.Item);
		}
	});
}

// Create an employee
router.post("/employee", function(req, res) {
	var employee = req.body;
	var empId = Date.now();
	var password = ((req.body && req.body.password) || Math.random().toString(36).substring(2,9));
	var params = {
		TableName : tableName,
		Item : {
			"id" : empId,
			"name" : employee.name,
			"startOfEmployment" : employee.start,
			"salary" : employee.salary
		},
		ConditionExpression : "attribute_not_exists(id)",
	};
	var paramsAuth = {
		TableName : authTableName,
		Item : {
			"id" : empId,
			"password" : password
		},
		ConditionExpression : "attribute_not_exists(id)",
	};
	docClient.put(params, function(err, data) {
		if (err) {
			sendErr(res, err);
		} else {
			// Create employee login info
			docClient.put(paramsAuth, function(err, data) {
				if (err) {
					sendErr(res, err);
				} else {
					sendSuccess(res, employee);
				}
			});
		}
	});

});

// Get an employee
router.get("/employee", function(req, res) {
	var employee = req.query;
	var params = {
		TableName : tableName,
		Key : {
			"id" : parseInt(employee.id)
		},
	};
	getItem(res, params, function(data){
		sendSuccess(res, data);
	});
});

// List all employees
router.get("/list", function(req, res) {
	var params = {
		TableName : tableName,
		KeySchema : [ {
			AttributeName : "id",
			KeyType : "HASH"
		} ],
		AttributeDefinitions : [ {
			AttributeName : "id",
			AttributeType : "N"
		} ],
		ProvisionedThroughput : {
			ReadCapacityUnits : 10,
			WriteCapacityUnits : 10
		}
	};
	docClient.scan(params, function(err, data) {
		if (err) {
			sendErr(res, err);
		} else {
			sendSuccess(res, data);
		}

	});
});

// List all employee login info
router.get("/authlist", function(req, res) {
	var params = {
		TableName : authTableName,
		KeySchema : [ {
			AttributeName : "id",
			KeyType : "HASH"
		} ],
		AttributeDefinitions : [ {
			AttributeName : "id",
			AttributeType : "N"
		} ],
		ProvisionedThroughput : {
			ReadCapacityUnits : 10,
			WriteCapacityUnits : 10
		}
	};
	docClient.scan(params, function(err, data) {
		if (err) {
			sendErr(res, err);
		} else {
			sendSuccess(res, data);
		}

	});
});

// Delete an employee
router.delete("/employee", function(req, res) {
	var employee = req.body;
	var params = {
		TableName : tableName,
		Key : {
			"id" : parseInt(employee.id),
		},
		ConditionExpression: "attribute_exists(id)"
	};
	var authParams = {
			TableName : authTableName,
			Key : {
				"id" : parseInt(employee.id),
			},
			ConditionExpression: "attribute_exists(id)"
		};
	
	docClient.delete(params, function(err, data){
		if (err) {
			sendErr(res, err);
		} else {
			docClient.delete(authParams, function(err, data){
				if (err) {
					sendErr(res, err);
				} else {
					sendSuccess(res, data);
				}
			});
		}
	});

});

// Get token from request body or query
function getToken(req, res, next){
	var token = ((req.body && req.body.token) || (req.query && req.query.token));
	if(token){
		try {
			var decoded = jwt.decode(token, process.env.SECRET);
			var params = {
					TableName : tableName,
					Key : {
						"id" : parseInt(decoded.id)
					},
				};
			getItem(res, params, function(data){
				req.user = data;
				next();
			});
		}
		catch(err){
			sendErr(res, "User not authorized");
		}
	}
	else{
		sendErr(res, "User not authorized");
	}

}

// Send log to logging service
function sendLog(message, start, params){
	var data = {
			"message" : message,
			"source" : "EMP"
	};
	if(params){
		data["params"] = params;
		data["message"] += " | params:";
	}
	var logPath = (start && logPathStart) || logPathEnd;
	request({url: logPath, method:"POST", json:data});
}
module.exports = router;