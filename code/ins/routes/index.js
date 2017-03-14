// Routing 

var express = require('express'), router = express.Router();
router.use('/ins', require('./ins'));
module.exports = router;
