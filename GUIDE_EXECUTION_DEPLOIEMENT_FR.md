# Système de Gestion des Demandes de Stage – ONEP
## Guide d'installation, d'exécution et de déploiement (FR)

**Projet :** `com.onep:internship:0.0.1-SNAPSHOT`
**Stack :** Spring Boot 4.1.0 – Java 26 – Thymeleaf – Spring Security – Spring Data JPA – MySQL – JavaMail (Gmail SMTP)
**Port par défaut :** 8080 – **URL locale :** http://localhost:8080/

---

### 1. Présentation

Application web de gestion des candidatures de stage :
- **Candidat (APPLICANT) :** inscription, connexion, dépôt d'une candidature par cycle annuel (brouillon / soumission), téléversement CV PDF, suivi, mot de passe oublié par e-mail.
- **Admin (ADMIN) :** tableau de bord, recherche, filtres par statut (SOUMIS, EN REVUE, ACCEPTÉ, REJETÉ), statistiques, acceptation / rejet / suppression, consultation des CV, envoi d'e-mails automatiques.
- Cycle annuel : une seule candidature active par candidat (CNI + e-mail) par année civile.
- Uploads CV : PDF uniquement, 10 Mo max, stockage local `./uploads`.

### 2. Prérequis

| Élément | Version requise |
|---|---|
| JDK | 26 (ex. Temurin 26.0.1+) – vérifier avec `java -version` |
| Maven | Inclus via `mvnw` / `mvnw.cmd` (Maven 3.9.16) – pas d'installation requise |
| MySQL / MariaDB | 8.0+ / 10.6+ en écoute sur `localhost:3306` |
| SMTP Gmail | Compte Gmail + mot de passe d'application (pour les e-mails) |
| OS | Windows 10/11, Linux ou macOS |

### 3. Contenu livré

```
internship application system/
├── pom.xml
├── mvnw / mvnw.cmd
├── src/main/resources/application.properties
├── src/main/java/com/onep/internship/  (controllers, config, model, repository, service)
├── src/main/resources/templates/       (Thymeleaf : index, login, register, applicant/, admin/)
├── src/main/resources/static/          (css, js, logo, hero-home)
├── database/
│   ├── schema-install.sql                (install client sûre, sans DROP)
│   └── archive/                        (anciens scripts dev, ne pas utiliser)
├── uploads/                            (stockage CV – contient .gitkeep)
└── target/internship-0.0.1-SNAPSHOT.jar (jar pré-compilé ~59 Mo)
```

### 4. Configuration (variables d'environnement)

L'application lit `src/main/resources/application.properties` avec surcharges par variables d'environnement :

| Variable | Défaut dans le code | Usage |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/internship_db` | URL MySQL |
| `DB_USER` | `internship_app` | Utilisateur MySQL |
| `DB_PASSWORD` | vide | Mot de passe MySQL (**à définir**) |
| `MAIL_HOST` / `MAIL_PORT` | `smtp.gmail.com` / `587` | Serveur SMTP |
| `MAIL_USERNAME` | vide | Expéditeur Gmail (**à définir**) |
| `MAIL_PASSWORD` | vide | Mot de passe d'application Gmail (**à définir**) |
| `APP_PUBLIC_BASE_URL` | `http://localhost:8080` | URL publique (liens reset password) |
| `APP_SECURITY_REQUIRE_HTTPS` | `false` | Mettre `true` en production |
| `SESSION_COOKIE_SECURE` | `false` | Mettre `true` en production (HTTPS) |
| `JPA_DDL_AUTO` | `validate` | `validate` en prod, `update` en dev si besoin |

> Fichier modèle : `.env.example`. Copiez-le et adaptez les valeurs. Ne commitez jamais les vrais secrets.

### 5. Installation locale – pas à pas (Windows PowerShell)

```powershell
# 0. Vérifier Java 26
java -version

# 1. Créer la base et les tables (sûr, sans DROP)
mysql -u root -p < "database\schema-install.sql"
# Les anciens scripts sont archivés dans database\archive\ (ne pas utiliser).

# 2. Configurer l'environnement (session courante)
$env:DB_USER="internship_app"
$env:DB_PASSWORD="OneP!2026"
$env:MAIL_USERNAME="votre-gmail@gmail.com"
$env:MAIL_PASSWORD="votre-mot-de-passe-application"
$env:APP_PUBLIC_BASE_URL="http://localhost:8080"
$env:APP_SECURITY_REQUIRE_HTTPS="false"
$env:SESSION_COOKIE_SECURE="false"
$env:JPA_DDL_AUTO="validate"

# 3a. Lancer en mode développement
.\mvnw.cmd spring-boot:run

# 3b. OU compiler puis lancer le jar
.\mvnw.cmd clean package
java -jar target\internship-0.0.1-SNAPSHOT.jar
```

Sous Linux/macOS, remplacez `.\mvnw.cmd` par `./mvnw` et `$env:X="y"` par `export X="y"`.

### 6. Accès à l'application

| URL | Rôle |
|---|---|
| http://localhost:8080/ | Accueil public |
| http://localhost:8080/home/register | Inscription candidat |
| http://localhost:8080/home/login | Connexion |
| http://localhost:8080/applicant/apply | Déposer une candidature |
| http://localhost:8080/applicant/dashboard | Suivi candidat |
| http://localhost:8080/admin/dashboard | Admin |
| http://localhost:8080/admin/stats | Statistiques |

**Compte admin initial (créé automatiquement si table `users` vide) :**
- Identifiant : `admin` – Mot de passe : `admin123`
- ⚠️ Changez ce mot de passe immédiatement après la première connexion.

### 7. Déploiement en production

1. **Serveur :** JDK 26 + MySQL/MariaDB + ~200 Mo disque (+ espace `uploads/`).
2. **Base de données :**
   ```bash
   mysql -u root -p < schema-install.sql
   # Puis créez l'utilisateur applicatif :
   # CREATE USER 'internship_app'@'localhost' IDENTIFIED BY '<fort>';
   # GRANT SELECT,INSERT,UPDATE,DELETE ON internship_db.* TO 'internship_app'@'localhost';
   ```
3. **Déposer le jar :** copiez `target/internship-0.0.1-SNAPSHOT.jar` sur le serveur + créez `./uploads/` inscriptible à côté du jar.
4. **Variables de production :**
   ```bash
   export DB_USER="internship_app"
   export DB_PASSWORD="<mot-de-passe-fort>"
   export MAIL_USERNAME="<gmail-entreprise>"
   export MAIL_PASSWORD="<mot-de-passe-application-gmail>"
   export APP_PUBLIC_BASE_URL="https://votre-domaine.com"
   export APP_SECURITY_REQUIRE_HTTPS="true"
   export SESSION_COOKIE_SECURE="true"
   export JPA_DDL_AUTO="validate"
   java -jar internship-0.0.1-SNAPSHOT.jar
   ```
5. **HTTPS :** exposez via Nginx / Apache en reverse-proxy TLS vers `localhost:8080`. L'application exige `SESSION_COOKIE_SECURE=true` quand `REQUIRE_HTTPS=true`.
6. **Service système (exemple Linux systemd) :**
   ```ini
   [Unit]
   Description=ONEP Internship Portal
   After=mysql.service
   [Service]
   User=app
   WorkingDirectory=/opt/internship
   Environment="DB_USER=internship_app" "DB_PASSWORD=***" "MAIL_USERNAME=***" "MAIL_PASSWORD=***" "APP_PUBLIC_BASE_URL=https://votre-domaine.com" "APP_SECURITY_REQUIRE_HTTPS=true" "SESSION_COOKIE_SECURE=true" "JPA_DDL_AUTO=validate"
   ExecStart=/usr/bin/java -jar /opt/internship/internship-0.0.1-SNAPSHOT.jar
   Restart=always
   [Install]
   WantedBy=multi-user.target
   ```
7. **Recompiler si besoin :** `./mvnw clean package` → nouveau jar dans `target/`.

### 8. Dépannage

| Symptôme | Cause / Solution |
|---|---|
| `Access denied for user internship_app` | Créez l'utilisateur MySQL (voir §7.2) ou corrigez `DB_USER/DB_PASSWORD` |
| `Unknown database internship_db` | Exécutez `schema-install.sql` |
| `Hibernate validation failed` | Schéma désynchronisé → ré-exécutez `schema-install.sql` sur une base neuve |
| `IllegalStateException: require-https … cookie.secure` | `SESSION_COOKIE_SECURE` doit être `true` si `APP_SECURITY_REQUIRE_HTTPS=true` |
| E-mails non reçus | Vérifiez `MAIL_USERNAME/PASSWORD` (mot de passe d'application Gmail), port 587 sortant ouvert. Les échecs sont loggés en warning, l'appli continue |
| `429 Too Many Requests` | Rate-limiter (5 tentatives login/min/IP) – attendez 60 s |
| CV rejeté | PDF uniquement, ≤ 10 Mo, commençant par `%PDF` |

### 9. Actions recommandées avant remise client

- [ ] Définir `DB_PASSWORD`, `MAIL_PASSWORD`, `APP_ADMIN_PASSWORD`.
- [ ] Secrets uniquement en variables d'env, jamais en dur.
- [ ] Passer `APP_PUBLIC_BASE_URL` en HTTPS public, activer `REQUIRE_HTTPS` + `SESSION_COOKIE_SECURE`.
- [ ] Sauvegarder régulièrement `./uploads/` + dump MySQL.
- [ ] Recompiler le jar après tout changement (`mvnw clean package`) – le jar livré date du 23/07/2026.

*Document généré le 16/09/2026 – Spring Boot 4.1.0 / Java 26.*
