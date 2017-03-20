/**
 * Created by daniel on 19/03/17.
 */
var express = require('express');
var https = require('https');
var request = require('request');
var AWS = require('aws-sdk');
var morgan = require('morgan');
var fs = require('fs');

var bodyParser = require('body-parser');
AWS.config.update({
    region : "us-west-2",
    endpoint : "https://dynamodb.us-west-2.amazonaws.com"
});
var docClient = new AWS.DynamoDB.DocumentClient();
require("dotenv").load();
var app = express();

// This portion copied from emp
const tableName = "INS";
const mbrPath = process.env.MBR_PATH;
const logPathStart = process.env.LOG_PATH + "/start";
const logPathEnd = process.env.LOG_PATH + "/end";
const verifyAuthPath = process.env.VERIFY_AUTH_PATH;

morgan.token('params1', function(req, res) {
    return JSON.stringify(req.body)
});
morgan.token('params2', function(req, res) {
    return JSON.stringify(req.query)
});
app.use(morgan('[:date[clf]] | :method :url :status | User-agent: :user-agent | Body: :params1| Query: :params2'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({
    extended : false
}));

var sslOptions = {
    key : fs.readFileSync('ssl/key.pem'),
    cert : fs.readFileSync('ssl/cert.pem'),
    passphrase : '4145'
};



// End of portion


app.post("/ins/insurance_quote", function(req, res){
    sendLog(true, true, "Received insurance quote POST request from RE.", {}, res);
    mortId = parseInt(req.body.mortId);
    token = req.body.token;
    houseId = req.body.houseId;
    appraisedValue = parseInt(req.body.appraisedValue);



    if(!(req.body.serviceCode && req.body.mortId && req.body.token)){
        sendLog(false, false, "Service request missing needed parameter.", {}, res);
        return;
    }

    var params = {
        TableName : tableName,
        Item : {
            "mortId" : mortId,
            "houseId" : houseId,
            "appraisedValue" : appraisedValue,
            "token" : token
        }

    };

    docClient.put(params, function(err, data){
        if (err){
            sendLog(false, false, "Could not put into DB.", {"error": err}, res);
        }
        else{
            sendLog(false, true, "Insurance quote correctly saved.", data, res);
        }
    });




});


app.post("/ins/services", function(req, res){
    console.log('85');
    sendLog(true, true, "Received services data from MUN", {}, res);
    console.log(req.body);
    serviceCode = req.body.serviceCode;
    mortId = parseInt(req.body.mortId);
    token = req.body.token;

    if(!(req.body.serviceCode && req.body.mortId && req.body.token)){
        sendLog(false, false, "Service request missing needed parameter.", {}, res);
        return;
    }

    params = {
        TableName : tableName,
        Key : {
            "mortId": mortId
        }
    };
    console.log('before get');

    docClient.get(params, function(err, data){
        if (err){
            sendLog(false, false, "Could not find an entry with that mortId and token.", {"error": err}, res);
        }
        else if(!data || !data.Item){
            sendLog(false, false, "docClient did not find data.", {"error": err}, res);
        }
        else{
            insuredValue = data.Item.appraisedValue * .80;
            deductible = data.Item.appraisedValue * .10;
            serviceCode = data.Item.serviceCode;
            if (serviceCode = "HS"){
                insuredValue = insuredValue * 1.1;
            }
            params = {
                "mortId" : mortId,
                "insuredValue" : insuredValue,
                "deductible" : deductible,
                "name" :  "Bob Bobson",
                "token" : token
            };
            console.log('before request');
            request.post({url: mbrPath, json: params}, function(err, response){
                if (err){
                    console.log("failed to communicate with MBR");
                    sendLog(false, false, "Communication with MBR returned an error", {"error": err, "response": response}, res)
                }
                else{
                    console.log("Communicated with MBR");
                    sendLog(false, true, "POST to MBR successful", params, res);
                }
            });
        }
    });



});


https.createServer(sslOptions, app).listen(3001);
console.log("Listening on 3001");


function sendLog(is_start, is_successful, message, params, res){


    source = "INS";
    if (is_start){
        logPath = logPathStart;
    }
    else{
        logPath = logPathEnd;
    }
    data = {
        "message": message,
        "params": params,
        "source": "INS"
    };
    request.post({url: logPath, json:data});

    // Portions of this taken from emp

    if (!is_start){
        if (is_successful == false) {
            console.log("not successful");
            console.log(params);
            res.status(500).json({
                success: false,
                error: params["error"],
                data: data
            });
        }
        else {
            res.status(200).json({
                success: true,
                data: data
            })

        }
    }


}