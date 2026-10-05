-- Separate database and owner account for Keycloak (idempotent; safe to re-run).
-- Keycloak manages its own schema inside this database only; it has no access to
-- the application's database.
--
--   :kc_user / :kc_password   Keycloak's database account
--   :kc_db                    Keycloak's database

SELECT format('CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION',
              :'kc_user', :'kc_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'kc_user') \gexec

SELECT format('ALTER ROLE %I PASSWORD %L', :'kc_user', :'kc_password') \gexec

SELECT format('CREATE DATABASE %I OWNER %I', :'kc_db', :'kc_user')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'kc_db') \gexec

SELECT format('REVOKE ALL ON DATABASE %I FROM PUBLIC', :'kc_db') \gexec
