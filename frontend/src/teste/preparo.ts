import '@testing-library/jest-dom/vitest';
import { afterEach, beforeEach } from 'vitest';

/**
 * O `sessionStorage` é limpo entre testes.
 *
 * Sem isto, um teste que guarda sessão deixa o seguinte começar autenticado, e
 * a suíte passa a depender da ordem — que é o defeito que some quando se roda um
 * arquivo só e volta quando se roda tudo.
 */
beforeEach(() => sessionStorage.clear());
afterEach(() => sessionStorage.clear());
