#!/bin/bash
# ============================================================
#  docker-entrypoint.sh
# ============================================================
set -e

WAR="/usr/local/tomcat/webapps/ROOT.war"
ROOT="/usr/local/tomcat/webapps/ROOT"
CONFIG="$ROOT/WEB-INF/classes/config.properties"

# Extraire le WAR manuellement avant le démarrage de Tomcat
echo "[startup] Extraction du WAR..."
mkdir -p "$ROOT"
cd "$ROOT"
unzip -q "$WAR"
rm "$WAR"

echo "[startup] Injection des variables d'environnement dans config.properties..."

# --- MongoDB ---
[ -n "$MONGO_URI" ]             && sed -i "s|mongo.uri=.*|mongo.uri=$MONGO_URI|" "$CONFIG"
[ -n "$MONGO_DATABASE" ]        && sed -i "s|mongo.database=.*|mongo.database=$MONGO_DATABASE|" "$CONFIG"

# --- JWT ---
[ -n "$JWT_SECRET" ]            && sed -i "s|jwt.secret=.*|jwt.secret=$JWT_SECRET|" "$CONFIG"
[ -n "$JWT_EXPIRATION" ]        && sed -i "s|jwt.expiration=.*|jwt.expiration=$JWT_EXPIRATION|" "$CONFIG"

# --- MailerSend ---
[ -n "$MAILERSEND_API_KEY" ]    && sed -i "s|mailersend.api.key=.*|mailersend.api.key=$MAILERSEND_API_KEY|" "$CONFIG"
[ -n "$MAIL_FROM" ]             && sed -i "s|mail.from=.*|mail.from=$MAIL_FROM|" "$CONFIG"
[ -n "$MAIL_FROM_NAME" ]        && sed -i "s|mail.from.name=.*|mail.from.name=$MAIL_FROM_NAME|" "$CONFIG"

# --- Cloudinary ---
[ -n "$CLOUDINARY_CLOUD_NAME" ] && sed -i "s|cloudinary.cloud_name=.*|cloudinary.cloud_name=$CLOUDINARY_CLOUD_NAME|" "$CONFIG"
[ -n "$CLOUDINARY_API_KEY" ]    && sed -i "s|cloudinary.api_key=.*|cloudinary.api_key=$CLOUDINARY_API_KEY|" "$CONFIG"
[ -n "$CLOUDINARY_API_SECRET" ] && sed -i "s|cloudinary.api_secret=.*|cloudinary.api_secret=$CLOUDINARY_API_SECRET|" "$CONFIG"

# --- CORS ---
[ -n "$CORS_ORIGIN" ]           && sed -i "s|app.cors.origin=.*|app.cors.origin=$CORS_ORIGIN|" "$CONFIG"

# --- Admin ---
[ -n "$ADMIN_EMAIL" ]           && sed -i "s|admin.email=.*|admin.email=$ADMIN_EMAIL|" "$CONFIG"
[ -n "$ADMIN_PASSWORD" ]        && sed -i "s|admin.password=.*|admin.password=$ADMIN_PASSWORD|" "$CONFIG"

echo "[startup] config.properties prêt. Démarrage de Tomcat..."
exec catalina.sh run