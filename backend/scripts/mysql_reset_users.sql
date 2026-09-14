-- Creates or repairs the two application accounts WITHOUT touching the data.
--
-- Use this instead of mysql_users.sql when the database already exists and only the accounts are
-- wrong or missing - mysql_users.sql opens with DROP DATABASE and would destroy the schema.
-- Safe to run repeatedly.
--
--   mysql -u root -p < scripts/mysql_reset_users.sql
--
-- =====================================================================================
--  REPLACE BOTH PASSWORD PLACEHOLDERS BELOW BEFORE RUNNING. There is no guard: run this
--  unedited and the passwords literally become __SET_ADMIN_PASSWORD__ and
--  __SET_USER_PASSWORD__, and the next login attempt fails with "Access denied".
-- =====================================================================================

CREATE DATABASE IF NOT EXISTS recipes CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- recipesadmin: DDL rights. Liquibase runs as this account - it creates the application tables
-- plus its own DATABASECHANGELOG / DATABASECHANGELOGLOCK, so DML alone is not enough.
--
-- CREATE then ALTER: CREATE IF NOT EXISTS leaves an existing account's password untouched, so the
-- ALTER is what makes this idempotent whether the account exists or not.
CREATE USER IF NOT EXISTS `recipesadmin`@`%` IDENTIFIED WITH mysql_native_password BY '__SET_ADMIN_PASSWORD__';
ALTER USER `recipesadmin`@`%` IDENTIFIED WITH mysql_native_password BY '__SET_ADMIN_PASSWORD__';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, REFERENCES, INDEX, ALTER, EXECUTE, CREATE VIEW, SHOW VIEW,
CREATE ROUTINE, ALTER ROUTINE, EVENT, TRIGGER ON `recipes`.* TO `recipesadmin`@`%`;

-- recipesuser: DML only. This is the account the application should run as once the schema exists.
CREATE USER IF NOT EXISTS `recipesuser`@`%` IDENTIFIED WITH mysql_native_password BY '__SET_USER_PASSWORD__';
ALTER USER `recipesuser`@`%` IDENTIFIED WITH mysql_native_password BY '__SET_USER_PASSWORD__';
GRANT SELECT, INSERT, UPDATE, DELETE, SHOW VIEW ON `recipes`.* TO `recipesuser`@`%`;

FLUSH PRIVILEGES;

-- Verification. Both accounts should be listed with host '%'.
SELECT user, host, plugin FROM mysql.user WHERE user LIKE 'recipes%';

-- An anonymous account shadows `recipesadmin`@`%` for local connections, because MySQL prefers the
-- more specific host: 'localhost' beats '%'. The symptom is "Access denied for user
-- 'recipesadmin'@'localhost'" with a password that is actually correct. This should return nothing.
SELECT user, host FROM mysql.user WHERE user = '';
