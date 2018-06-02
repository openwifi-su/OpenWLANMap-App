#!/bin/sh

echo "Doing xmllint..."
bool=false
# explicitly set IFS to contain only a line feed
IFS='
'
filelist="$(find . -type f ! -name "$(printf "*\n*")")"
for line in $filelist; do
  if echo "$line" | grep -E -q ".*\.\/.*\.xml" ; then #??
    if ! grep -q "$line" ".ci/xml_accepted" && echo "$line" | grep -vqE "./app/build/*"; then
      xmllint --noout "$line"
      if [ $? -eq 1 ]; then
        bool=true
      fi
    fi
  fi
done
if $bool; then
  exit 1
else
  echo "No xmllint errors found."
fi

exit 0
