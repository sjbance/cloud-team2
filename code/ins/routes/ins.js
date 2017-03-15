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
// Receive info from RE
router.post("/insurance_quote", function(req, res){
	currStep = "POST from RE - saving mortgage data";
	sendLog(currStep, true, req.body);

	if(!(req.body.appraisedValue && req.body.houseId && req.body.token)){
		sendErr(res, "Missing essential attribute.");
		return;
	}

	// av = req.body.appraisedValue.split('.');
	// appraisedValue = parseInt(av[0]) * 100 + parseInt(av[1]);
	var params = {
			TableName : tableName,
			Item : {
				"mortId" : req.body.mortId,
				"houseId" : req.body.houseId,
				"appraisedValue" : req.body.appraisedValue,
				"token" : req.body.token
			}

		};

	docClient.put(params, function(err, data) {
		if (err) {
			console.log("docClient error");
			sendErr(res, err);
		} else {
			sendSuccess(res, req.body);
		}
	});

});

// Receive info from MUN, send data to MBR
router.post("/services", function(req, res){
	currStep = "POST from MUN - sending to MBR";
	sendLog(currStep, true, req.body);

	if(!(req.body.serviceCode && req.body.mortId && req.body.token)){
		sendErr(res, "Missing essential attribute.");
		return;
	}


	serviceCode = req.body.serviceCode;
	mortId = req.body.mortId;
	var mortgage;

	var params = {
		TableName : tableName,
		Key : {
			"mortId": mortId
		}
	};


	docClient.get(params, function(err, data) {
		if (!data || !data.Item){
			sendErr(res, "no such mortId");
		}
		else if (err) {
			sendErr(res, err);
		}
		else {
			mortgage = data.Item;
			var insuredValue = mortgage.appraisedValue * .65;
			var deductible = mortgage.appraisedValue * .10;
			/*
			var insuredValue = (mortgage.appraisedValue * .65).toString();
			insuredValue = insuredValue.slice(0, insuredValue.length - 2) + '.' + insuredValue.slice(-2);
			var deductible = (mortgage.appraisedValue * .01).toString();
			deductible = deductible.slice(0, deductible.length - 2) + '.' + deductible.slice(-2);
			*/

	params = {
		"token" : req.body.token,
		"mortId" : 1,
		"insuredValue" : insuredValue,
		"deductible" : deductible,
		"name" : "bob"
		};


	request({url: mbrPath + "/insurer", method:"POST", json:params}, function(err, response, body){
		if (err || response.statusCode != 200) {
			sendErr(res, "Error received from MBR" + " - " + err + " - " + response.statusCode + " - " + "params: " + JSON.stringify(params));
		} else {
			sendSuccess(res);
		}
	});

		}
	});

});

// Get item in database
function getItem(res, params, callback) {
    docClient.get(params, function(err, data) {
        if (!data || !data.Item) {
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


//Send log to logging service
function sendLog(message, start, params){
	var data = {
			"message" : message,
			"source" : "INS"
	};
	if(params){
		data["params"] = params;
		data["message"] += " | params:";
	}
	var logPath = (start && logPathStart) || logPathEnd;
	request({url: logPath, method:"POST", json:data});
}

module.exports = router;
