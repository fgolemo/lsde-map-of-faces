import os
import sys

filePath = "/media/florian/Data/lsde-data/binning-output/output-z"
outPath = "/home/florian/Downloads/lsde-out/imgs/"
zoom = "12"
fillerImg = "/home/florian/Downloads/lsde-out/filler.jpg"

out = {}

with open(filePath+zoom) as f:
    for line in f:
        lineSplit = line.split("\t")
        # print len(lineSplit[1][1:-2].split(","))
        # quit()
        out[lineSplit[0]] = lineSplit[1][1:-2].split(",")

def asint(s):
    try:
        return int(s), ''
    except ValueError:
        return sys.maxint, s


i = 1
os.makedirs(outPath+zoom)
for key, vals in [(k, out[k]) for k in sorted(out, key=asint, reverse=True)]:
    print "line: ", i
    for img in vals:
        if img == "0":
            os.symlink(fillerImg, outPath + zoom + "/" + str(i) + ".jpg")
        else:
            with open(outPath + zoom + "/" + str(i) + ".jpg", "wb") as fout:
                # print img
                fout.write(img.decode("base64"))
        i += 1

