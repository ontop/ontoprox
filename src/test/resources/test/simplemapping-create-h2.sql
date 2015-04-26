
CREATE TABLE table1 (
    uri character varying(100) NOT NULL,
    val character varying(100),
);


CREATE TABLE table2 (
    id int NOT NULL,
    attr1 char(100) NOT NULL,
    attr2 int NOT NULL,
);


INSERT INTO table1 VALUES ('uri1', 'value1');

ALTER TABLE table1
    ADD CONSTRAINT table1_pkey PRIMARY KEY (uri);

ALTER TABLE table2 ADD CONSTRAINT table2_pkey PRIMARY KEY (id);

