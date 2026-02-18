create table if not exists refresh_tokens (
    id bigserial primary key,
    token_hash varchar(128) not null unique,
    user_id bigint not null,
    expires_at timestamp with time zone not null,
    revoked boolean not null default false,
    created_at timestamp with time zone not null,
    revoked_at timestamp with time zone,
    constraint fk_refresh_tokens_user
        foreign key (user_id)
            references users (id)
            on delete cascade
);

create index if not exists idx_refresh_tokens_user_revoked
    on refresh_tokens (user_id, revoked);
