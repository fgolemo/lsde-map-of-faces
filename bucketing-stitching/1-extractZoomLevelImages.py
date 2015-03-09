import os
import sys
import shutil
import cStringIO
import Image
from scipy import sparse
from bisect import bisect_left

# if len(sys.argv) < 2 or sys.argv[1] != int(sys.argv[1]) or int(sys.argv[1]) > 19 or int(sys.argv[1]) < 0:
# sys.exit("need zoom level parameter and it needs to be integer in range 0-19")
# zoomLevel = sys.argv[1]


zoomLevel = 6

outpath = "C:/Users/Florian/Downloads/faces2-out/" + str(zoomLevel) + "/"

if not os.path.exists(outpath):
    os.makedirs(outpath)

picsPerTilePerAxis = 2
picsXaxis = 2 ** zoomLevel * picsPerTilePerAxis
picsYaxis = 2 ** zoomLevel * picsPerTilePerAxis / 2

gridSpace = 180.0 / picsYaxis

coordsX = [(x * gridSpace) - 180 for x in range(picsXaxis)] + [180]
coordsY = [(y * gridSpace) - 90 for y in range(picsYaxis)] + [90]

done = sparse.lil_matrix((picsYaxis, picsXaxis))

# print coordsX
# print coordsY


def whichCell(coordX, coordY):
    if coordX >= 0:
        lo = int(round(len(coordsX) / 2) - 1)
        hi = len(coordsX)
    else:
        lo = 0
        hi = int(round(len(coordsX) / 2) + 1)
    x = bisect_left(coordsX, coordX, lo, hi) - 1
    # x = bisect_left(coordsX, coordX) - 1

    if coordY >= 0:
        lo = int(round(len(coordsY) / 2) - 1)
        hi = len(coordsY)
    else:
        lo = 0
        hi = int(round(len(coordsY) / 2) + 1)
    y = bisect_left(coordsY, coordY, lo, hi) - 1
    # y = bisect_left(coordsY, coordY) - 1

    return (x, y)


print "total images possible: {total}".format(total=picsYaxis * picsXaxis)

with open("reducer-out.txt") as f:
    i = 1
    for line in f:
        split1 = line.find(" ")
        split2 = line.find(" ", split1 + 1)
        lat = float(line[:split1])
        long = float(line[split1 + 1:split2])
        img = line[split2 + 1:-1]
        x, y = whichCell(long, lat)
        if (done[y, x] == 0):  # then we don't have that cell filled yet
            done[y, x] = 1
            filename = str(y * picsXaxis + x)
            print "writing image slot {slot}, image numer {no}/{total}".format(slot=filename, no=i,
                                                                               total=picsYaxis * picsXaxis)
            im = Image.open(cStringIO.StringIO(img.decode("base64")))
            im.save(outpath + filename + ".jpg")
            i += 1

print "done, checking for missing images and copying/linking the background pic"

for i in range(picsYaxis * picsXaxis):
    if not os.path.isfile(outpath + str(i) + ".jpg"):
        shutil.copy(outpath + "../background.jpg", outpath + str(i) + ".jpg")
