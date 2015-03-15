
a=`ls -mv $1/*.jpg | tr "," " " | tr "\n" " " | tr -s " "`
# echo $a
tilesY=$(echo '2^'$1 | bc)
tilesX=$(echo '2*2^'$1 | bc)
echo  $tilesX $tilesY 
montage $a -tile ${tilesX}x${tilesY} -geometry 100x100 out$1.jpg
