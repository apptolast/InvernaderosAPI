-- Tabla de auditoria de comandos enviados por usuarios
CREATE TABLE metadata.command_audit_log (
    id           BIGINT       PRIMARY KEY,
    code         VARCHAR(50)  NOT NULL UNIQUE,
    time         TIMESTAMPTZ  NOT NULL,
    setting_code VARCHAR(20)  NOT NULL,
    value        VARCHAR(100) NOT NULL,
    user_id      BIGINT       NOT NULL REFERENCES metadata.users(id),
    tenant_id    BIGINT       NOT NULL REFERENCES metadata.tenants(id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_command_audit_log_user ON metadata.command_audit_log(user_id);
CREATE INDEX idx_command_audit_log_tenant ON metadata.command_audit_log(tenant_id);
CREATE INDEX idx_command_audit_log_time ON metadata.command_audit_log(time DESC);
CREATE INDEX idx_command_audit_log_setting_code ON metadata.command_audit_log(setting_code);
CREATE INDEX idx_command_audit_log_code ON metadata.command_audit_log(code);

CREATE SEQUENCE IF NOT EXISTS metadata.command_audit_log_code_seq START WITH 1;
