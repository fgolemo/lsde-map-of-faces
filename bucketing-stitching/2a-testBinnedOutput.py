import sys

filePath = "/media/florian/Data/lsde-data/binning-output/output-z"
zoom = str(20)

out = {}

with open(filePath + zoom) as f:
    for line in f:
        lineSplit = line.split("\t")

        imgData = lineSplit[1][1:-2].split(",")
        lineBuf = []
        for img in imgData:
            if img == "0":
                lineBuf.append(".")
            else:
                lineBuf.append("0")
        out[lineSplit[0]] = lineBuf

sorted(out, key=out.get)

def asint(s):
    try:
        return int(s), ''
    except ValueError:
        return sys.maxint, s

for key, val in [(k, out[k]) for k in sorted(out, key=asint)]:
    # print key, val
    print "".join(val)