#!/usr/bin/env bash

cd $1

a=`ls -mv $2/*.jpg | tr "," " " | tr "\n" " " | tr -s " "`
# echo $a
#tilesY=$(echo '2^'$2 | bc)
#tilesX=$(echo '2*2^'$2 | bc)
tilesY=500
tilesX=1002
echo  $tilesX $tilesY
montage $a -tile ${tilesX}x${tilesY} -geometry 50x50 out$2.jpg
