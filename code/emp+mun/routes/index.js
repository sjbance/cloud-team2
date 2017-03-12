// Routing for 3 company pages (life insurance, mortgage, employer)

var express = require('express'), router = express.Router();

router.use('/mun', require('./mun'));
router.use('/emp', require('./emp'));

// EMP pages
router.get('/emp', function(req, res) {
	res.render('emp/home');
});
router.get('/emp/mortgage', function(req, res) {
	res.render('emp/mortgage');
});

module.exports = router;