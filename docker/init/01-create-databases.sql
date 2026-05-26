SELECT 'CREATE DATABASE credit_conveyor'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'credit_conveyor')\gexec