// Interactions with MUN web service/DB

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
var unknownMortgageCode = "ZZZ";

const tableName = "INS";
const mbrPath = process.env.MBR_PATH;
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

router.post("/", function(req, res){
	console.log("!");
});
// Receive info from RE
router.post("/insurance_quote", function(req, res){
	console.log("INSURANCE QUOTE!!!");
	currStep = "POST from RE - saving mortgage data";
	sendLog(currStep, true, req.body);

	console.log(req.body.appraisedValue);
	av = req.body.appraisedValue.split('.');
	appraisedValue = parseInt(av[0]) * 100 + parseInt(av[1]);
	console.log(appraisedValue);
	var params = {
			TableName : tableName,
			Item : {
				"mortId" : parseInt(req.body.mortId),
				"houseId" : req.body.houseId,
				"appraisedValue" : appraisedValue,
				"token" : req.body.token
			}

		};
	console.log("params");

	docClient.put(params, function(err, data) {
		if (err) {
			console.log("docClient error");
			sendErr(res, err);
		} else {
			sendSuccess(res, req.body);
		}
	});
	console.log("docClient");

});

// Receive info from MUN, send data to MBR
router.post("/services", function(req, res){
	currStep = "POST from MUN - sending to MBR";
	sendLog(currStep, true, req.body);


	serviceCode = req.body.serviceCode;
	mortId = parseInt(req.body.mortId);
	var mortgage;

	var params = {
		TableName : tableName,
		Key : {
			"mortId": mortId
		}
	};


	docClient.get(params, function(err, data) {
		if (!data || !data.Item){
			console.log("no data from docClient");
			senderr(res, "no such mortId");
		}
		else if (err) {
			console.log("error from docClient");
			sendErr(res, err);
		} else {
			console.log("mortgage set");
			mortgage = data.Item;
			console.log("mortgage:");
			for (k in mortgage){
				console.log(k);
				console.log(mortgage[k]);
			}



			var insuredValue = (mortgage.appraisedValue * .65).toString();

			insuredValue = insuredValue.slice(0, insuredValue.length - 2) + '.' + insuredValue.slice(-2);

			var deductible = (mortgage.appraisedValue * .01).toString();
			deductible = deductible.slice(0, deductible.length - 2) + '.' + deductible.slice(-2);
			console.log("deductible set");
	params = {
		"token" : req.body.token,
		"mortId" : req.body.mortId,
		"insuredValue" : insuredValue,
		"deductible" : deductible,
		"name" : ""
		};
	console.log("sending request to MBR");
	request({url: mbrPath + "/insurer", method:"POST", json:params}, function(err, response, body){
		if (err || response.statusCode != 200) {
			sendErr(res, "Error received from MBR" + " - " + err + " - " + response.statusCode);
		} else {
			sendSuccess(res);
		}
	});
		}
	});


});

// Get item in database
function getItem(res, params, callback){
	docClient.get(params, function(err, data) {
		if (!data || !data.Item){
			// Send default service code for unknown house ID
			callback({"serviceCode": unknownHouseCode});
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
