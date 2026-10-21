CREATE TABLE postulaciones (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id   BIGINT       NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    empresa_id   BIGINT       NOT NULL REFERENCES empresas (id),
    puesto       VARCHAR(200) NOT NULL,
    fuente       VARCHAR(100),
    url_oferta   VARCHAR(1000),
    salario_min  INTEGER CHECK (salario_min >= 0),
    salario_max  INTEGER CHECK (salario_max >= 0),
    moneda       CHAR(3),
    postulado_en TIMESTAMPTZ  NOT NULL DEFAULT now(),
    creado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (salario_min IS NULL OR salario_max IS NULL OR salario_min <= salario_max)
);

CREATE INDEX idx_postulaciones_usuario ON postulaciones (usuario_id, postulado_en DESC);

-- El estado de una postulación no es una columna: cada cambio es una fila aquí
-- y el estado actual se deduce del evento más reciente (ver la vista de V4).
CREATE TABLE eventos (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    postulacion_id BIGINT      NOT NULL REFERENCES postulaciones (id) ON DELETE CASCADE,
    tipo           VARCHAR(20) NOT NULL
        CHECK (tipo IN ('postulada', 'respuesta', 'entrevista', 'oferta', 'rechazo', 'seguimiento', 'cerrada')),
    nota           TEXT,
    ocurrido_en    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_eventos_postulacion ON eventos (postulacion_id, ocurrido_en DESC, id DESC);
