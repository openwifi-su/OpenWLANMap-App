#!/bin/sh

echo "Doing javalint..."
if ! [ -e ".ci/google_checks.xml" ]; then
  if ! wget https://raw.githubusercontent.com/checkstyle/checkstyle/master/src/main/resources/google_checks.xml -O .ci/google_checks.xml
  then
    rm .ci/google_checks.xml
    echo "failed downloading google_checks.xml.."
    exit 1
  fi
fi

if ! [ -e ".ci/checkstyle.jar" ]; then
  if ! wget http://downloads.sourceforge.net/project/checkstyle/checkstyle/8.10.1/checkstyle-8.10.1-all.jar -O .ci/checkstyle.jar
  then
    rm .ci/checkstyle.jar
    echo "failed downloading checkstyle.jar.."
    exit 1
  fi
fi
bool=false
# explicitly set IFS to contain only a line feed
IFS='
'
for line in $(java -jar .ci/checkstyle.jar -c .ci/google_checks.xml .); do
  if ! grep -q "$line" ".ci/java_accepted" && echo "$line" | grep -vqE "./app/build/*"; then
    echo "$line"
    bool=true
  fi
done
if $bool; then
  exit 1
else
  echo "No javalint errors found."
fi

exit 0
