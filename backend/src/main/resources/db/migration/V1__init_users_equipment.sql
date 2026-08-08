-- V1: Users and Equipment
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL,
    nom_prenom      VARCHAR(255) NOT NULL,
    actif           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE equipment (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(100) NOT NULL UNIQUE,
    designation     VARCHAR(255) NOT NULL,
    famille         VARCHAR(100),
    zone            VARCHAR(100),
    constructeur    VARCHAR(150),
    mise_en_service DATE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_equipment_code ON equipment (code);
CREATE INDEX idx_equipment_famille ON equipment (famille);
CREATE INDEX idx_users_email ON users (email);
