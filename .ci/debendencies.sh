#!/bin/sh

set -e

#check installed debendenciece
echo "Checking software depencies..."

winenv="$(uname -s)"

if [ -f /etc/debian_version ]; then
  #check shell file linter
  which shellcheck > /dev/null 2>&1 || ( echo "please install shellcheck"; exit 1 )
  #check xml file linter
  which xmllint > /dev/null 2>&1 || ( echo "please install libxml2-utils"; exit 1 )
  #check java1.8 installation
  if which java > /dev/null 2>&1 ; then
    eval "$(java -version > /dev/null 2>&1 | grep -E -q 'version \"1\.8.*\"')" || ( echo "please install java1.8"; exit 1)
  else
    echo "please install java1.8"; exit 1
  fi
elif [ -f /etc/arch-release ]; then
  if ! command -v yaourt > /dev/null 2>&1 ; then
    echo "Please install yaourt first."
    exit 1
  fi

  #check shell file linter
  which shellcheck > /dev/null 2>&1 || ( echo "please install shellcheck"; exit 1 )
  #check xml file linter
  which xmllint > /dev/null 2>&1 || ( echo "please install libxml2"; exit 1 )
  #check java1.8 installation
  if which java > /dev/null 2>&1 ; then
    eval "$(java -version > /dev/null 2>&1 | grep -E -q 'version \"1\.8.*\"')" || ( echo "please install java1.8"; exit 1 )
  else
    echo "please install java1.8"; exit 1
  fi
elif [ -z "${winenv##*CYGWIN*}" ] || [ -z "${winenv##*MINGW*}" ]; then
  #check shell file linter
  which shellcheck > /dev/null 2>&1 || ( echo "please install shellcheck"; exit 1 )
  #check xml file linter
  which xmllint > /dev/null 2>&1 || ( echo "please install libxml2-utils"; exit 1 )
  #check java1.8 installation
  if which java > /dev/null 2>&1 ; then
  eval "$(java -version > /dev/null 2>&1 | grep -E -q 'version \"1\.8.*\"')" || ( echo "please install java1.8"; exit 1)
  else
    echo "please install java1.8"; exit 1
  fi
else
  echo "Only compatible with Debian or Archlinux."
  exit 1
fi
echo "All software depencies installed."
exit 0
