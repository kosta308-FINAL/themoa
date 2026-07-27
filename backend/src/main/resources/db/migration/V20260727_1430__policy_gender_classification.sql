create table if not exists policy_gender_classification (
    policy_id int not null,
    audience varchar(30) not null,
    exclusive bit not null,
    confidence double not null,
    applicant_scope varchar(30) not null,
    evidence varchar(500) not null,
    classification_version varchar(50) not null,
    source_hash varchar(64) not null,
    source_updated_at datetime(6) null,
    classified_at datetime(6) not null,
    primary key (policy_id),
    constraint fk_policy_gender_classification_policy
        foreign key (policy_id) references policy (id)
);

create index idx_policy_gender_classification_audience
    on policy_gender_classification (audience);

create index idx_policy_gender_classification_version_hash
    on policy_gender_classification (classification_version, source_hash);

create index idx_policy_gender_classification_updated_at
    on policy_gender_classification (classified_at);
