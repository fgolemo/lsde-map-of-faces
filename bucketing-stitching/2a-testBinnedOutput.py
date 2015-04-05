
filePath = "/media/florian/Data/lsde-data/binning-output/output-z"
zoom = str(4)

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

for key, val in out.iteritems():
    print "".join(val)