const router = require('express').Router();
router.get('/', (_req, res) => res.json({ status: 'ok', timestamp: new Date().toISOString(), database: 'not_connected' }));
module.exports = router;
