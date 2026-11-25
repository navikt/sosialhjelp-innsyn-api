CREATE TABLE klage_metadata
(
    klage_id uuid primary key,
    digisos_id uuid not null,
    vedtak_filreferanse_id uuid not null,
    opprettet timestamp not null,
    sendt timestamp,
    status varchar(50) not null
);


CREATE TABLE klage
(
    id uuid PRIMARY KEY,
    begrunnelse text,
    CONSTRAINT fk_klage_klage_metadata FOREIGN KEY (id) REFERENCES klage_metadata (klage_id) ON DELETE CASCADE
);