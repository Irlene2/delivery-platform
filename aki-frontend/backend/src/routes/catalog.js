const router = require('express').Router();
// Estrutura de referência; ainda não lê produtos do banco de dados.
router.get('/', (_req, res) => res.json({ items: [], note: 'Catálogo vazio de demonstração. Conectar ao banco em etapa futura.' }));
router.get('/:id', (req, res) => res.status(404).json({ error: 'Produto não encontrado.', requestedId: req.params.id }));
module.exports = router;
