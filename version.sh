VERSION="1.2.8"

REGEX="s/version=\"[0-9]\{1,2\}\.[0-9]\{1,2\}\.[0-9]\{1,2\}\"/version=\"$VERSION\"/g"
sed -i '' -e $REGEX ./config_dev.xml
sed -i '' -e $REGEX ./config_staging.xml
sed -i '' -e $REGEX ./config_production.xml

BUILD=$1
echo "Copying resources for $BUILD"
rm -Rf platforms/android/app/src/main/res.bak
mv platforms/android/app/src/main/res platforms/android/app/src/main/res.bak
cp -R platforms/android/app/src/main/res.$BUILD platforms/android/app/src/main/res
