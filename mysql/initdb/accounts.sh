#!/bin/sh
# The application's two database accounts. The MySQL entrypoint runs every script in
# /docker-entrypoint-initdb.d once, while the data volume is empty, after creating MYSQL_DATABASE -
# so a change here reaches only a fresh volume (docker compose down -v).
#
# Liquibase connects as DB_ADMIN_USERNAME, which may change the schema. The application connects as
# DB_USERNAME, which may only read and write rows: it cannot drop the tables it works on.
# MYSQL_USER cannot express this - the entrypoint grants that account all privileges on the schema.
#
# The values go into SQL string literals, so they may not contain a single quote.
mysql -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL
CREATE USER '$DB_ADMIN_USERNAME'@'%' IDENTIFIED BY '$DB_ADMIN_PASSWORD';
GRANT ALL PRIVILEGES ON \`$MYSQL_DATABASE\`.* TO '$DB_ADMIN_USERNAME'@'%';
CREATE USER '$DB_USERNAME'@'%' IDENTIFIED BY '$DB_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE ON \`$MYSQL_DATABASE\`.* TO '$DB_USERNAME'@'%';
SQL
