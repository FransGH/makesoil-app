VERSION="1.2.7"

REGEX="s/version=\"[0-9]\{1,2\}\.[0-9]\{1,2\}\.[0-9]\{1,2\}\"/version=\"$VERSION\"/g"
sed -i '' -e $REGEX ./config_dev.xml
sed -i '' -e $REGEX ./config_staging.xml
sed -i '' -e $REGEX ./config_production.xml
