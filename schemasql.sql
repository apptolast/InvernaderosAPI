create table actuator_states
(
    id             smallserial
        primary key,
    name           varchar(20)                            not null
        constraint uq_actuator_states_name
            unique,
    description    text,
    is_operational boolean                  default false not null,
    display_order  smallint                 default 0     not null,
    color          varchar(7),
    created_at     timestamp with time zone default now() not null
);

comment on table actuator_states is 'Estados posibles para actuadores';

comment on column actuator_states.is_operational is 'TRUE si el actuador esta funcionando en este estado';

comment on column actuator_states.display_order is 'Orden para mostrar en UI';

comment on column actuator_states.color is 'Color hexadecimal para UI (ej: #00FF00)';

alter table actuator_states
    owner to admin;

create index idx_actuator_states_name
    on actuator_states (name);

create index idx_actuator_states_operational
    on actuator_states (is_operational);

create table flyway_schema_history
(
    installed_rank integer                 not null
        constraint flyway_schema_history_pk
            primary key,
    version        varchar(50),
    description    varchar(200)            not null,
    type           varchar(20)             not null,
    script         varchar(1000)           not null,
    checksum       integer,
    installed_by   varchar(100)            not null,
    installed_on   timestamp default now() not null,
    execution_time integer                 not null,
    success        boolean                 not null
);

alter table flyway_schema_history
    owner to admin;

create index flyway_schema_history_s_idx
    on flyway_schema_history (success);

create table mqtt_acl
(
    id         uuid                     default gen_random_uuid() not null
        primary key,
    username   varchar(100)                                       not null,
    permission varchar(20)                                        not null
        constraint mqtt_acl_permission_check
            check ((permission)::text = ANY
                   (ARRAY [('allow'::character varying)::text, ('deny'::character varying)::text])),
    action     varchar(20)                                        not null
        constraint mqtt_acl_action_check
            check ((action)::text = ANY
                   (ARRAY [('publish'::character varying)::text, ('subscribe'::character varying)::text, ('pubsub'::character varying)::text])),
    topic      varchar(255)                                       not null,
    qos        integer
        constraint mqtt_acl_qos_check
            check (qos = ANY (ARRAY [0, 1, 2])),
    created_at timestamp with time zone default now()
);

alter table mqtt_acl
    owner to admin;

create index idx_mqtt_acl_topic
    on mqtt_acl (topic);

create index idx_mqtt_acl_username
    on mqtt_acl (username);

grant select on mqtt_acl to public;

create table tenants
(
    id         uuid                     default gen_random_uuid() not null
        primary key,
    name       varchar(100)                                       not null
        unique,
    email      varchar(255)                                       not null,
    is_active  boolean                  default true,
    created_at timestamp with time zone default now(),
    updated_at timestamp with time zone default now(),
    province   varchar(100),
    country    varchar(50)              default 'España'::character varying,
    phone      varchar(50),
    location   jsonb
);

comment on column tenants.location is 'Coordenadas geográficas: {lat: number, lon: number}';

alter table tenants
    owner to admin;

create table greenhouses
(
    id         uuid                     default gen_random_uuid() not null
        primary key,
    name       varchar(100)                                       not null,
    tenant_id  uuid                                               not null
        constraint fk_greenhouse_tenant
            references tenants
            on delete cascade
        references tenants
            on delete cascade,
    location   jsonb,
    area_m2    numeric(10, 2),
    timezone   varchar(50)              default 'Europe/Madrid'::character varying,
    is_active  boolean                  default true,
    created_at timestamp with time zone default now(),
    updated_at timestamp with time zone default now(),
    unique (tenant_id, name)
);

alter table greenhouses
    owner to admin;

create index idx_greenhouses_active
    on greenhouses (is_active);

create index idx_greenhouses_location_gin
    on greenhouses using gin (location jsonb_path_ops);

create index idx_greenhouses_tenant
    on greenhouses (tenant_id);

create index idx_greenhouses_tenant_active
    on greenhouses (tenant_id, is_active)
    where (is_active = true);

grant select on greenhouses to public;

create table mqtt_users
(
    id                uuid                     default gen_random_uuid() not null
        primary key,
    username          varchar(100)                                       not null
        unique,
    password_hash     varchar(255)                                       not null,
    salt              varchar(255)                                       not null,
    device_type       varchar(50)
        constraint mqtt_users_device_type_check
            check ((device_type)::text = ANY
                   (ARRAY [('SENSOR'::character varying)::text, ('ACTUATOR'::character varying)::text, ('GATEWAY'::character varying)::text, ('API'::character varying)::text])),
    greenhouse_id     uuid
        references greenhouses
            on delete cascade,
    is_active         boolean                  default true,
    created_at        timestamp with time zone default now(),
    updated_at        timestamp with time zone default now(),
    last_connected_at timestamp with time zone,
    tenant_id         uuid
        constraint fk_mqtt_users_tenant
            references tenants
            on delete cascade
);

alter table mqtt_users
    owner to admin;

create index idx_mqtt_users_active
    on mqtt_users (is_active);

create index idx_mqtt_users_device_type
    on mqtt_users (device_type)
    where (is_active = true);

create index idx_mqtt_users_greenhouse
    on mqtt_users (greenhouse_id);

create index idx_mqtt_users_last_connected
    on mqtt_users (last_connected_at desc)
    where (is_active = true);

create index idx_mqtt_users_tenant_active
    on mqtt_users (tenant_id, is_active)
    where (is_active = true);

create index idx_mqtt_users_username
    on mqtt_users (username);

grant select on mqtt_users to public;

create table sectors
(
    id            uuid default gen_random_uuid() not null
        primary key,
    greenhouse_id uuid                           not null
        references greenhouses
            on delete cascade,
    variety       varchar(100)
);

comment on table sectors is 'Subdivisiones logicas de un invernadero para agrupar dispositivos';

alter table sectors
    owner to admin;

create index idx_sectors_greenhouse
    on sectors (greenhouse_id);

create index idx_tenants_active
    on tenants (is_active)
    where (is_active = true);

create index idx_tenants_email_lower
    on tenants (lower(email::text));

grant select on tenants to public;

create table units
(
    id          smallserial
        primary key,
    symbol      varchar(10)                            not null
        constraint uq_units_symbol
            unique,
    name        varchar(50)                            not null,
    description text,
    is_active   boolean                  default true  not null,
    created_at  timestamp with time zone default now() not null
);

comment on table units is 'Catalogo de unidades de medida para sensores y actuadores';

comment on column units.symbol is 'Simbolo de la unidad (ej: °C, %, hPa)';

comment on column units.name is 'Nombre descriptivo de la unidad';

alter table units
    owner to admin;

create index idx_units_active
    on units (is_active)
    where (is_active = true);

create index idx_units_symbol
    on units (symbol);

create table users
(
    id                          uuid                     default gen_random_uuid() not null
        primary key,
    tenant_id                   uuid                                               not null
        references tenants
            on delete cascade,
    username                    varchar(50)                                        not null
        unique,
    email                       varchar(255)                                       not null
        unique,
    password_hash               varchar(255)                                       not null,
    role                        varchar(20)                                        not null
        constraint users_role_check
            check ((role)::text = ANY
                   (ARRAY [('ADMIN'::character varying)::text, ('OPERATOR'::character varying)::text, ('VIEWER'::character varying)::text])),
    is_active                   boolean                  default true,
    last_login                  timestamp with time zone,
    created_at                  timestamp with time zone default now(),
    updated_at                  timestamp with time zone default now(),
    reset_password_token        varchar(255),
    reset_password_token_expiry timestamp with time zone
);

comment on column users.reset_password_token is 'Token for password reset flow';

comment on column users.reset_password_token_expiry is 'Expiration time for the password reset token';

alter table users
    owner to admin;

create index idx_users_email_lower
    on users (lower(email::text));

create index idx_users_last_login
    on users (last_login desc)
    where (is_active = true);

create index idx_users_tenant
    on users (tenant_id);

create index idx_users_tenant_active
    on users (tenant_id, is_active)
    where (is_active = true);

create index idx_users_tenant_role
    on users (tenant_id, role)
    where (is_active = true);

create index idx_users_username
    on users (username);

create index idx_users_username_lower
    on users (lower(username::text));

grant select on users to public;

create table device_categories
(
    id   smallint    not null
        primary key,
    name varchar(20) not null
        unique
);

alter table device_categories
    owner to admin;

create table device_types
(
    id                 smallserial
        primary key,
    name               varchar(30)                            not null
        constraint uq_device_types_name
            unique,
    description        text,
    default_unit_id    smallint
        references units,
    data_type          varchar(20)              default 'DECIMAL'::character varying
        constraint chk_device_types_data_type
            check ((data_type IS NULL) OR ((data_type)::text = ANY
                                           (ARRAY [('DECIMAL'::character varying)::text, ('INTEGER'::character varying)::text, ('BOOLEAN'::character varying)::text, ('TEXT'::character varying)::text, ('JSON'::character varying)::text]))),
    min_expected_value numeric(10, 2),
    max_expected_value numeric(10, 2),
    control_type       varchar(20)
        constraint chk_device_types_control_type
            check ((control_type IS NULL) OR ((control_type)::text = ANY
                                              (ARRAY [('BINARY'::character varying)::text, ('CONTINUOUS'::character varying)::text, ('MULTI_STATE'::character varying)::text]))),
    is_active          boolean                  default true  not null,
    created_at         timestamp with time zone default now() not null,
    category_id        smallint                               not null
        constraint fk_device_types_category
            references device_categories
);

comment on table device_types is 'Catalogo unificado de tipos de dispositivos (sensores y actuadores)';

comment on column device_types.data_type is 'Tipo de dato que genera el sensor';

comment on column device_types.min_expected_value is 'Valor minimo fisicamente posible';

comment on column device_types.max_expected_value is 'Valor maximo fisicamente posible';

comment on column device_types.control_type is 'BINARY (on/off), CONTINUOUS (0-100%), MULTI_STATE';

alter table device_types
    owner to admin;

create index idx_device_types_active
    on device_types (is_active)
    where (is_active = true);

create index idx_device_types_name
    on device_types (name);

create table devices
(
    id            uuid                     default gen_random_uuid() not null
        constraint devices_new_pkey
            primary key,
    tenant_id     uuid                                               not null
        constraint devices_new_tenant_id_fkey
            references tenants
            on delete cascade,
    greenhouse_id uuid                                               not null
        constraint devices_new_greenhouse_id_fkey
            references greenhouses
            on delete cascade,
    is_active     boolean                  default true              not null,
    created_at    timestamp with time zone default now()             not null,
    updated_at    timestamp with time zone default now()             not null,
    category_id   smallint
        constraint fk_devices_category
            references device_categories,
    type_id       smallint
        constraint fk_devices_type
            references device_types,
    unit_id       smallint
        constraint fk_devices_unit
            references units
);

comment on table devices is 'Dispositivos IoT unificados (sensores + actuadores) con CHECK constraints';

alter table devices
    owner to admin;

create table command_history
(
    id         uuid                     default gen_random_uuid() not null
        primary key,
    device_id  uuid                                               not null
        references devices
            on delete cascade,
    command    varchar(50)                                        not null,
    value      double precision,
    source     varchar(30)
        constraint chk_command_source
            check ((source IS NULL) OR ((source)::text = ANY
                                        (ARRAY [('USER'::character varying)::text, ('SYSTEM'::character varying)::text, ('SCHEDULE'::character varying)::text, ('ALERT'::character varying)::text, ('API'::character varying)::text, ('MQTT'::character varying)::text]))),
    user_id    uuid
                                                                  references users
                                                                      on delete set null,
    success    boolean,
    response   jsonb,
    created_at timestamp with time zone default now()             not null
);

comment on table command_history is 'Historico de comandos enviados a actuadores';

alter table command_history
    owner to admin;

create index idx_command_history_created
    on command_history (created_at desc);

create index idx_command_history_device
    on command_history (device_id);

create index idx_command_history_device_time
    on command_history (device_id asc, created_at desc);

create index idx_devices_new_active
    on devices (is_active)
    where (is_active = true);

create index idx_devices_new_greenhouse
    on devices (greenhouse_id);

create index idx_devices_new_tenant
    on devices (tenant_id);

create table alert_types
(
    id          smallint    not null
        primary key,
    name        varchar(30) not null
        unique,
    description text
);

alter table alert_types
    owner to admin;

create table periods
(
    id   smallint    not null
        primary key,
    name varchar(10) not null
        unique
);

alter table periods
    owner to admin;

create table alert_severities
(
    id                         smallint    not null
        primary key,
    name                       varchar(20) not null
        unique,
    level                      smallint    not null,
    description                text,
    color                      varchar(7),
    requires_action            boolean                  default false,
    notification_delay_minutes integer                  default 0,
    created_at                 timestamp with time zone default now()
);

alter table alert_severities
    owner to admin;

create table alerts
(
    id                  uuid                     default gen_random_uuid() not null
        primary key,
    greenhouse_id       uuid                                               not null
        references greenhouses
            on delete cascade,
    message             text                                               not null,
    is_resolved         boolean                  default false,
    resolved_at         timestamp with time zone,
    created_at          timestamp with time zone default now(),
    tenant_id           uuid                                               not null
        constraint fk_alerts_tenant
            references tenants
            on delete cascade,
    updated_at          timestamp with time zone default now(),
    resolved_by_user_id uuid
        constraint fk_alerts_resolved_by_user
            references users
            on delete set null,
    alert_type_id       smallint
        constraint fk_alerts_type
            references alert_types,
    severity_id         smallint
        constraint fk_alerts_severity
            references alert_severities,
    constraint chk_resolved_consistency
        check (((is_resolved = false) AND (resolved_at IS NULL)) OR ((is_resolved = true) AND (resolved_at IS NOT NULL)))
);

comment on table alerts is 'Sistema de alertas multi-tenant para monitoreo de invernaderos';

comment on column alerts.tenant_id is 'ID del tenant (denormalizado para queries eficientes)';

comment on column alerts.resolved_by_user_id is 'UUID del usuario que resolvió (preferir sobre resolved_by)';

comment on column alerts.alert_type_id is 'Normalized alert type (references alert_types.id). Replaces alert_type VARCHAR.';

comment on column alerts.severity_id is 'Normalized severity (references alert_severities.id). Replaces severity VARCHAR.';

alter table alerts
    owner to admin;

create index idx_alerts_alert_type_id
    on alerts (alert_type_id);

create index idx_alerts_created_at
    on alerts (created_at desc);

create index idx_alerts_greenhouse
    on alerts (greenhouse_id);

create index idx_alerts_greenhouse_severity_status
    on alerts (greenhouse_id asc, severity_id asc, is_resolved asc, created_at desc);

create index idx_alerts_resolved
    on alerts (is_resolved);

create index idx_alerts_severity_id
    on alerts (severity_id)
    where (is_resolved = false);

create index idx_alerts_tenant
    on alerts (tenant_id);

create index idx_alerts_tenant_unresolved
    on alerts (tenant_id asc, is_resolved asc, created_at desc)
    where (is_resolved = false);

create index idx_alerts_unresolved
    on alerts (is_resolved asc, created_at desc)
    where (is_resolved = false);

grant select on alerts to public;

create table settings
(
    id            uuid                     default gen_random_uuid() not null
        primary key,
    greenhouse_id uuid                                               not null
        references greenhouses
            on delete cascade,
    tenant_id     uuid                                               not null
        references tenants
            on delete cascade,
    parameter_id  smallint                                           not null
        references device_types,
    period_id     smallint                                           not null
        references periods,
    min_value     numeric(10, 2),
    max_value     numeric(10, 2),
    is_active     boolean                  default true,
    created_at    timestamp with time zone default now(),
    updated_at    timestamp with time zone default now(),
    unique (greenhouse_id, parameter_id, period_id)
);

alter table settings
    owner to admin;

