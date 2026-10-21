-- Solo se guarda el hash SHA-256 del token; el token en claro nunca llega a la base.
CREATE TABLE refresh_tokens (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id      BIGINT      NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    hash_token      CHAR(64)    NOT NULL UNIQUE,
    expira_en       TIMESTAMPTZ NOT NULL,
    revocado_en     TIMESTAMPTZ,
    reemplazado_por BIGINT REFERENCES refresh_tokens (id)
);

CREATE INDEX idx_refresh_tokens_usuario ON refresh_tokens (usuario_id);
