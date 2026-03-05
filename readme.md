# Database initialization

This project uses PostgreSQL as the main database.

Before starting the application, create a clean PostgreSQL container and initialize the database manually using the provided SQL script.
Before running the script, open `docker/script/init.sql` and replace the placeholder password value:

```sql
PASSWORD '?'
