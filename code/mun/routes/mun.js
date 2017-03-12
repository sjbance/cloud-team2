// Interactions with the life insurance web service/DB

var jwt = require("jwt-simple");
var router = require("express").Router();
var request = require("request");
var AWS = require("aws-sdk");
AWS.config.update({
	region : "us-west-2",
	endpoint : "https://dynamodb.us-west-2.amazonaws.com"
});
require("dotenv").load();
var docClient = new AWS.DynamoDB.DocumentClient();
var currStep;

const tableName = "MUN";
const insPath = process.env.INS_PATH;
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

// Create a service code listing 
router.post("/service", function(req, res) {
	var params = {
		TableName : tableName,
		Item : {
			"id" : parseInt(req.body.houseId),
			"serviceCode" : req.body.serviceCode
		},
		ConditionExpression : "attribute_not_exists(id)",
	};
	docClient.put(params, function(err, data) {
		if (err) {
			sendErr(res, err);
		} else {
			sendSuccess(res, req.body);
		}
	});

});

// Get a service code
router.get("/service", function(req, res) {
	var params = {
		TableName : tableName,
		Key : {
			"id" : parseInt(req.query.houseId)
		},
	};
	getItem(res, params, function(data){
		sendSuccess(res, data);
	});

});

// List all services
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

// Delete a service code
router.delete("/service", function(req, res) {
	var params = {
		TableName : tableName,
		Key : {
			"id" : parseInt(req.body.houseId),
		},
		ConditionExpression: "attribute_exists(id)"
	};
	
	docClient.delete(params, function(err, data){
		if (err) {
			sendErr(res, err);
		} else {
			sendSuccess(res, data);
		}
	});
});

// Send info to insurance web service
router.post("/insurance", function(req, res){
	currStep = "Request from RE - sending information to INS service";
	sendLog(currStep, true, req.body);
	var params = {
			TableName : tableName,
			Key : {
				"id" : parseInt(req.body.houseId)
			},
		};	
	getItem(res, params, function(data){
		params = {
				"mortId" : req.body.mortId,
				"serviceCode" : data.serviceCode				
		}
		request({url: insPath, method:"POST", json:params}, function(err, response, body){
			if (err || response.statusCode != 200) {
				console.log(err);
				sendErr(res, "Error recieved from INS");
			} else {
				sendSuccess(res);
			}
		});	
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
			sendErr(res, err);			
		}
	}
	else{
		sendErr(res, "User not authorized");
	}
}

//Send log to logging service
function sendLog(message, start, params){
	var data = {
			"message" : message,
			"source" : "MUN"
	};
	if(params){
		data["params"] = params;
		data["message"] += " | params:";
	}
	var logPath = (start && logPathStart) || logPathEnd;
	request({url: logPath, method:"POST", json:data});
}

module.exports = router;