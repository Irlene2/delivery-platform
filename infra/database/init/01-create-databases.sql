-- Um banco lógico por serviço. Em produção, cada um com credencial própria;
-- em desenvolvimento, uma instância com bancos separados.
-- Nenhum serviço acessa o banco de outro.
--
-- Cinco bancos PostgreSQL e dois MongoDB — ADR-021. O payment_db saiu em 01/11/2026,
-- com o payment-service: não há pagamento pelo aplicativo (ADR-021, emenda).
-- Sem PostGIS: geoprocessamento saiu do MVP com a ADR-020, e extensão criada
-- "por precaução" é superfície que alguém acaba usando.

CREATE DATABASE identity_db;
CREATE DATABASE merchant_db;
CREATE DATABASE settlement_db;
CREATE DATABASE order_db;
CREATE DATABASE delivery_db;
