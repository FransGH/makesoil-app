VERSION="1.4.2"

REGEX="s/version=\"[0-9]\{1,2\}\.[0-9]\{1,2\}\.[0-9]\{1,2\}\"/version=\"$VERSION\"/g"
sed -i '' -e $REGEX ./config_dev.xml
sed -i '' -e $REGEX ./config_staging.xml
sed -i '' -e $REGEX ./config_production.xml

# remove existing intent data from android.json to prevent merging
# the build process with add the correct one
FILE="platforms/android/android.json"
MANIFEST="platforms/android/app/src/main/AndroidManifest.xml"
jq 'del(.config_munge.files."AndroidManifest.xml".parents."/manifest/application/activity/intent-filter")' $FILE > tmp.json && mv tmp.json $FILE
sed -i '' -e '/data android:host/d' $MANIFEST
jq 'del(.config_munge.files."AndroidManifest.xml".parents."/manifest/application")' $FILE > tmp.json && mv tmp.json $FILE
sed -i '' -e '/org\.makesoil\.app\.targetdomain/d' $MANIFEST

BUILD=$1
echo "Copying resources for $BUILD"
rm -Rf platforms/android/app/src/main/res.bak
mv platforms/android/app/src/main/res platforms/android/app/src/main/res.bak
cp -R platforms/android/app/src/main/res.$BUILD platforms/android/app/src/main/res
