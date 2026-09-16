# Système de Gestion des Demandes de Stage – ONEP

Application web de gestion des candidatures de stage : dépôt en ligne, suivi candidat, validation admin, statistiques et notifications par e-mail.

## Fonctionnalités

**Candidat :**
- Inscription et connexion sécurisée
- Dépôt d'une candidature par cycle annuel (brouillon puis soumission)
- Téléversement du CV au format PDF (10 Mo maximum)
- Suivi des candidatures et consultation des décisions
- Réinitialisation du mot de passe par e-mail

**Administrateur :**
- Tableau de bord avec recherche et filtres par statut
- Instruction des dossiers : mise en revue, acceptation, rejet, suppression
- Consultation et téléchargement des CV
- Statistiques : répartition par statut, universités, filières, activité mensuelle
- Envoi automatique d'e-mails (réception, acceptation, rejet)

## Stack technique

- Spring Boot 4.1.0 – Java 26
- Thymeleaf – Spring Security – Spring Data JPA – Bean Validation
- MySQL / MariaDB – JavaMail (SMTP Gmail)
- Maven Wrapper inclus (`mvnw` / `mvnw.cmd`)

## Prérequis

- JDK 26
- MySQL 8.0+ ou MariaDB 10.6+ sur `localhost:3306`
- Compte Gmail avec mot de passe d'application (pour les e-mails)

## Installation rapide (Windows)

```powershell
# 1. Créer la base et les tables (sûr, sans DROP)
mysql -u root -p < "database\schema-install.sql"

# 2. Configurer l'environnement
$env:DB_USER="internship_app"
$env:DB_PASSWORD="OneP!2026"
$env:MAIL_USERNAME="votre-gmail@gmail.com"
$env:MAIL_PASSWORD="votre-mot-de-passe-application"
$env:APP_PUBLIC_BASE_URL="http://localhost:8080"
$env:APP_SECURITY_REQUIRE_HTTPS="false"
$env:SESSION_COOKIE_SECURE="false"
$env:JPA_DDL_AUTO="validate"

# 3. Lancer l'application
.\mvnw.cmd spring-boot:run
```

Sous Linux ou macOS, utiliser `./mvnw` et `export VARIABLE="valeur"`.

Alternative : compiler puis exécuter le jar :

```powershell
.\mvnw.cmd clean package
java -jar target\internship-0.0.1-SNAPSHOT.jar
```

Ouvrir ensuite http://localhost:8080/

## Configuration

Fichier de référence : `src/main/resources/application.properties`. Modèle des variables : `.env.example`.

| Variable | Défaut | Description |
|---|---|---|
| `DB_USER` | `internship_app` | Utilisateur MySQL |
| `DB_PASSWORD` | `OneP!2026` | Mot de passe MySQL (à changer) |
| `MAIL_USERNAME` | adresse Gmail | Expéditeur des e-mails |
| `MAIL_PASSWORD` | mot de passe d'application | Mot de passe d'application Gmail (à changer) |
| `APP_PUBLIC_BASE_URL` | `http://localhost:8080` | URL publique (liens de réinitialisation) |
| `APP_SECURITY_REQUIRE_HTTPS` | `false` | Mettre `true` en production |
| `SESSION_COOKIE_SECURE` | `false` | Mettre `true` en production (HTTPS) |
| `JPA_DDL_AUTO` | `validate` | `validate` en production |

Base de données : `internship_db`. Schéma créé par `database/schema-install.sql` (sûr, sans DROP). Les anciens scripts sont archivés dans `database/archive/`.

Les CV sont stockés dans `./uploads/` (PDF uniquement).

## Comptes d'accès

| URL | Usage |
|---|---|
| `/` | Accueil public |
| `/home/register` | Inscription candidat |
| `/home/login` | Connexion |
| `/applicant/apply` | Déposer une candidature |
| `/applicant/dashboard` | Suivi candidat |
| `/admin/dashboard` | Administration |
| `/admin/stats` | Statistiques |

Compte administrateur initial (créé si la table `users` est vide) : identifiant `admin`, mot de passe `admin123`. À changer dès la première connexion.

## Déploiement en production

1. Serveur avec JDK 26 et MySQL.
2. Créer la base : `mysql -u root -p < schema-install.sql`, puis créer l'utilisateur MySQL avec un mot de passe fort.
3. Copier `target/internship-0.0.1-SNAPSHOT.jar` et créer un dossier `./uploads/` inscriptible à côté du jar.
4. Définir les variables de production (`DB_*`, `MAIL_*`, `APP_PUBLIC_BASE_URL=https://votre-domaine.com`, `APP_SECURITY_REQUIRE_HTTPS=true`, `SESSION_COOKIE_SECURE=true`, `JPA_DDL_AUTO=validate`).
5. Lancer : `java -jar internship-0.0.1-SNAPSHOT.jar`, derrière un reverse-proxy TLS (Nginx ou Apache).
6. Recompiler après toute modification : `./mvnw clean package`.

## Documentation détaillée

- `GUIDE_EXECUTION_DEPLOIEMENT_FR.md` : guide complet en français
- `RUN_AND_DEPLOYMENT_GUIDE_EN.md` : guide complet en anglais

## Remarques de sécurité

- Changer les mots de passe par défaut (`DB_PASSWORD`, `MAIL_PASSWORD`, `admin123`) avant toute mise en production.
- Ne pas commiter les secrets réels.
- Sauvegarder régulièrement la base MySQL et le dossier `uploads/`.
