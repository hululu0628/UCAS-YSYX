#!/bin/bash

# usage: addenv env_name path
# replace .bashrc into other shell config file if you are not using bash
function addenv() {
	if [ ! -f .envrc ]; then
		sed -i -e "/^export $1=.*/d" ~/.bashrc
		echo "export $1=`readlink -f $2`" >> ~/.bashrc
	else
		sed -i -e "/^export $1=.*/d" .envrc
		echo "export $1=`readlink -f $2`" >> .envrc
		direnv allow
	fi
	echo "By default this script will add environment variables into ~/.bashrc if there's no .envrc under this directory."
	echo "After that, please run 'source ~/.bashrc' to let these variables take effect."
	echo "If you use shell other than bash, please add these environment variables manually."
}

# usage: init repo branch directory trace [env]
# trace = true|false
function init() {
  if [ -d $3 ]; then
    echo "$3 is already initialized, skipping..."
    if [ $5 ] ; then
      addenv $5 $3
    fi
    return
  fi

  while [ ! -d $3 ]; do
    git clone -b $2 git@github.com:$1.git $3
  done
  log="$1 `cd $3 && git log --oneline --no-abbrev-commit -n1`"$'\n'

  if [ $4 == "true" ] ; then
    rm -rf $3/.git
    git add -A $3
    git commit -am "$1 $2 initialized"$'\n\n'"$log"
  else
    sed -i -e "/^\/$3/d" .gitignore
    echo "/$3" >> .gitignore
    git add -A .gitignore
    git commit --no-verify --allow-empty -am "$1 $2 initialized without tracing"$'\n\n'"$log"
  fi

  if [ $5 ] ; then
    addenv $5 $3
  fi
}

case $1 in
  nemu)
    init NJU-ProjectN/nemu ics2024 nemu true NEMU_HOME
    ;;
  abstract-machine)
    init NJU-ProjectN/abstract-machine ics2024 abstract-machine true AM_HOME
    init NJU-ProjectN/fceux-am ics2021 fceux-am false
    ;;
  am-kernels)
    init NJU-ProjectN/am-kernels ics2021 am-kernels false
    ;;
  nanos-lite)
    init NJU-ProjectN/nanos-lite ics2021 nanos-lite true
    ;;
  navy-apps)
    init NJU-ProjectN/navy-apps ics2024 navy-apps true NAVY_HOME
    ;;
  nvboard)
    init NJU-ProjectN/nvboard master nvboard false NVBOARD_HOME
    ;;
  npc-chisel)
    if [ -d npc/playground ]; then
      echo "chisel repo is already initialized, skipping..."
    else
      rm -rf npc
      init OSCPU/chisel-playground master npc true NPC_HOME
    fi
    ;;
  npc)
    addenv NPC_HOME npc
    ;;
  ysyxSoC)
    init OSCPU/ysyxSoC ysyx6 ysyxSoC false YSYXSOC_HOME
    ;;
  ysyxworkbench)
    addenv YSYX_HOME .
    ;;
  *)
    echo "Invalid input..."
    exit
    ;;
esac
