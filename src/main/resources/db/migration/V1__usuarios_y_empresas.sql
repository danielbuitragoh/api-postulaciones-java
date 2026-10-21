CREATE TABLE usuarios (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    hash_password VARCHAR(255) NOT NULL,
    rol           VARCHAR(20)  NOT NULL DEFAULT 'USUARIO' CHECK (rol IN ('USUARIO', 'ADMIN')),
    creado_en     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Cada usuario tiene sus propias empresas: dos personas pueden seguir a la misma empresa
-- sin compartir ni sector ni web. El nombre es único por usuario sin distinguir mayúsculas.
CREATE TABLE empresas (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id BIGINT       NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    nombre     VARCHAR(200) NOT NULL,
    sitio_web  VARCHAR(500),
    sector     VARCHAR(100),
    creado_en  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- La unicidad vive en la base y no en un SELECT previo: dos peticiones simultáneas
-- pasarían las dos ese SELECT.
CREATE UNIQUE INDEX empresas_por_usuario ON empresas (usuario_id, lower(nombre));
