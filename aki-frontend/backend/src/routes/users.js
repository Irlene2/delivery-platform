const router = require('express').Router();
router.post('/register', (req, res) => {
  const { name, email, phone } = req.body || {};
  if (!name || (!email && !phone)) return res.status(400).json({ error: 'Informe o nome e pelo menos um contato: e-mail ou telefone.' });
  res.status(501).json({ error: 'Cadastro ainda não implementado; nenhum dado foi salvo.', received: { name, email: email || null, phone: phone || null } });
});
router.post('/login', (_req, res) => res.status(501).json({ error: 'Autenticação ainda não implementada.' }));
module.exports = router;
