const router = require('express').Router();
// Endpoints documentados como esqueleto; não persistem pedidos.
router.get('/', (_req, res) => res.json({ items: [], note: 'Pedidos ainda não persistidos: banco não conectado.' }));
router.post('/', (req, res) => {
  const { items, customer, paymentMethod } = req.body || {};
  if (!Array.isArray(items) || items.length === 0) return res.status(400).json({ error: 'Informe ao menos um item.' });
  return res.status(501).json({ error: 'Criação de pedido ainda não implementada.', received: { itemsCount: items.length, hasCustomer: Boolean(customer), paymentMethod: paymentMethod || null } });
});
router.get('/:id', (req, res) => res.status(404).json({ error: 'Pedido não encontrado.', requestedId: req.params.id }));
module.exports = router;
