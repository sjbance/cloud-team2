// Routing for EMP

var express = require('express'), router = express.Router();

router.use('/emp', require('./emp'));

// EMP pages
router.get('/emp', function(req, res) {
	res.render('emp/home');
});
router.get('/emp/mortgage', function(req, res) {
	res.render('emp/mortgage');
});

module.exports = router;