import sys

filePath = "/media/florian/Data/lsde-data/binning-output/output-z8"

out = {}

with open(filePath) as f:
    for line in f:
        lineSplit = line.split("\t")

        imgData = lineSplit[1][1:-2].split(",")
        lineBuf = []
        for img in imgData:
            if img == "0":  # we don't have an img
                lineBuf.append(".")
            else:  # we DO have an img
                lineBuf.append("0")
        out[lineSplit[0]] = lineBuf

sorted(out, key=out.get)


def asint(s):
    try:
        return int(s), ''
    except ValueError:
        return sys.maxint, s


for key, val in [(k, out[k]) for k in sorted(out, key=asint, reverse=True)]:
    # print key, val
    print "".join(val)