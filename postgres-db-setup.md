# PostgreSQL Database Setup

Use this file to create all databases (auth, user, product) in a single PostgreSQL instance.  
**Note:** This file is for initial setup only and is not part of the product service.

---

## 1. Run PostgreSQL container

```bash
docker run -d \
  --name postgres-db \
  -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:16
```

---

## 2. Connect to PostgreSQL

```bash
docker exec -it postgres-db psql -U postgres
```

---

## 3. Create databases and users

Run the following SQL in the `psql` session:

```sql
-- AUTH DB
CREATE USER auth_user WITH PASSWORD 'auth_password';
CREATE DATABASE auth OWNER auth_user;
GRANT ALL PRIVILEGES ON DATABASE auth TO auth_user;

-- USER DB
CREATE USER user_user WITH PASSWORD 'user_password';
CREATE DATABASE "user" OWNER user_user;
GRANT ALL PRIVILEGES ON DATABASE "user" TO user_user;

-- PRODUCT DB
CREATE USER product_user WITH PASSWORD 'product_password';
CREATE DATABASE product OWNER product_user;
GRANT ALL PRIVILEGES ON DATABASE product TO product_user;
```

---

## 4. Exit psql

```sql
\q
```

---

    later automate the db creation using
    docker-entrypoint-initdb.d/
      └── init.sql
