VERSION=`./gradlew version | grep Version | cut -f 2 -d " "`
PASSPHRASE=`cat ~/.gnupg/passphrase.txt`
GPGPARAMS="--passphrase $PASSPHRASE --batch --yes --no-tty"
./gradlew assemble generatePom

gpg $GPGPARAMS -ab build/pom.xml
gpg $GPGPARAMS -ab build/libs/kolasu-${VERSION}.jar
gpg $GPGPARAMS -ab build/libs/kolasu-${VERSION}-javadoc.jar
gpg $GPGPARAMS -ab build/libs/kolasu-${VERSION}-sources.jar
cd build/libs
jar -cvf bundle-kolasu.jar ../pom.xml ../pom.xml.asc kolasu-${VERSION}.jar kolasu-${VERSION}.jar.asc kolasu-${VERSION}-javadoc.jar kolasu-${VERSION}-javadoc.jar.asc kolasu-${VERSION}-sources.jar kolasu-${VERSION}-sources.jar.asc
cd ../..

mkdir -p release
mv build/libs/bundle-kolasu.jar release
