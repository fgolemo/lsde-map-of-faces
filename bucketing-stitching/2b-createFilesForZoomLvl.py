import os
import sys
import subprocess

filePath = "/media/florian/Data/lsde-data/binning-output/output-z"
outPath = "/home/florian/Downloads/lsde-out/imgs/"
linesPath = "/home/florian/Downloads/lsde-out/img-lines/"
zoom = "12"
fillerImg = "/home/florian/Downloads/lsde-out/filler.jpg"

picsPerZoomLevel = {  # only y axis, i.e. full image for zoom level 0 is 50x100, 4 is 100x200
                      0: 50,
                      4: 100,
                      8: 200,
                      12: 500,
                      16: 1000
                      }

out = {}

with open(filePath + zoom) as f:
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


tilesY = picsPerZoomLevel[int(zoom)]
tilesX = tilesY * 2 + 2

i = 1
line = 1
lineFiles = []
os.makedirs(outPath + zoom)
for key, vals in [(k, out[k]) for k in sorted(out, key=asint, reverse=True)]:
    print "line: ", i
    lineFileNames = []
    for img in vals:
        if img == "0":
            os.symlink(fillerImg, outPath + zoom + "/" + str(i) + ".jpg")
        else:
            with open(outPath + zoom + "/" + str(i) + ".jpg", "wb") as fout:
                # print img
                fout.write(img.decode("base64"))
        lineFileNames.append(outPath + zoom + "/" + str(i) + ".jpg")
        i += 1

    if int(zoom) > 8:
        lineFile = "{linesPath}{zoom}-{line}.jpg".format(linesPath=linesPath, zoom=zoom, line=line)
        try:
            cmd = "montage {files} -tile {tilesX}x1 -geometry 50x50 {lineFile}".format(files=" ".join(lineFileNames),
                                                                                     tilesX=tilesX, lineFile=lineFile)
            montageOut = subprocess.check_output(cmd, shell=True, stderr=subprocess.STDOUT)
            for cellFile in lineFileNames:
                os.remove(cellFile)
        except subprocess.CalledProcessError:
            print cmd
            quit("montage didn't work")

        lineFiles.append(lineFile)
    line += 1

try:
    xWidth = 50 * tilesX
    outFile = outPath + "out" + zoom + ".jpg"
    cmd = "montage {files} -tile 1x{tilesY} -geometry {xWidth}x50 {outFile}".format(files=" ".join(lineFiles),
                                                                             tilesY=tilesY, outFile=outFile, xWidth=xWidth)
    montageOut = subprocess.check_output(cmd, shell=True, stderr=subprocess.STDOUT)
    for cellFile in lineFileNames:
        os.remove(cellFile)
except subprocess.CalledProcessError:
    print cmd
    quit("final montage didn't work")


