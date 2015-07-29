CREATE DATABASE simple
  WITH OWNER = postgres
       ENCODING = 'UTF8'
       TABLESPACE = pg_default
       LC_COLLATE = 'C'
       LC_CTYPE = 'C'
       CONNECTION LIMIT = -1;

COMMENT ON DATABASE simple
  IS 'a simple database for testing Compile and Rewrite implementation';

CREATE TABLE "tableS"
(
  col1 integer NOT NULL,
  col2 integer NOT NULL,
  CONSTRAINT pkey PRIMARY KEY (col1, col2)
)
WITH (
  OIDS=FALSE
);