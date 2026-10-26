import type { InputHTMLAttributes } from 'react';

/** Um campo no padrão do cartão de entrada (`.form-group` + `.input-container`). */
export function CampoDoCartao({
  id,
  rotulo,
  icone,
  erro,
  dica,
  ...resto
}: Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> & {
  id: string;
  rotulo: string;
  icone: string;
  erro?: string | undefined;
  dica?: string;
}) {
  return (
    <div className="form-group">
      <label htmlFor={id}>{rotulo}</label>
      <div className="input-container">
        <span className="material-icons input-icon">{icone}</span>
        <input {...resto} id={id} className={erro !== undefined ? 'input-error' : undefined} />
      </div>
      <small className="error-message">{erro ?? dica}</small>
    </div>
  );
}
