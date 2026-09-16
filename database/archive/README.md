# Archive — ne pas utiliser pour une installation client

- `full-dump.sql` : dump dev avec DROP DATABASE / DROP TABLE. Destructeur. Gardé pour référence locale uniquement.
- `migration.sql` : mise à jour d'une ancienne base existante. Inutile pour une installation fraîche.
- `migrations/` : correctifs historiques déjà inclus dans `schema-install.sql`.

Installation client : utilisez uniquement `../schema-install.sql`.
