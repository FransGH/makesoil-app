SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
FILE=$SCRIPTPATH/app/src/main/kotlin/notifications/MyFirebaseMessagingService.kt

echo "Updating imports for $FILE..."
# this changes org.makesoil.app or org.staging.makesoil.app to org$1.makesoil.app
# $1 should be empty or \.staging
sed -i '' -e "s/org.\staging\.makesoil\.app/org$1\.makesoil\.app/g" $FILE
sed -i '' -e "s/org\.makesoil\.app/org$1\.makesoil\.app/g" $FILE
