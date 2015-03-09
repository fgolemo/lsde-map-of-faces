#@ TESTS

# with open("face9_0.jpg", "rb") as f:
#     data = f.read()
#     print "".join(data.encode("base64").split("\n"))
#
# with open("out.txt", "w") as f:
#     f.write("40.71867 126.56554 " + "".join(data.encode("base64").split("\n")))
#
# with open("out.txt") as f:
#     for line in f:
#         split1 = line.find(" ")
#         split2 = line.find(" ", split1 + 1)
#         lat = line[:split1]
#         long = line[split1 + 1:split2]
#         img = line[split2 + 1:]
#
#         with open("out-test.jpg", "wb") as fout:
#             fout.write(img.decode("base64"))

from os import listdir
from os.path import isfile, join
import re

imgpath = "C:/Users/Florian/Downloads/faces2"
outpath = "C:/Users/Florian/Downloads/faces2-out"
onlyfiles = [ f for f in listdir(imgpath) if isfile(join(imgpath,f)) ]
i = 0

# from random import uniform
# x, y = uniform(-180,180), uniform(-90, 90)

with open("reducer-out.txt", "wb") as fout:
    with open("coords.txt") as f:
        for line in f:
            split1 = line.find(" ")
            lat = line[:split1]
            long = line[split1 + 1:-1]
            with open(imgpath + "/" + onlyfiles[i], "rb") as fimg:
                img = "".join(fimg.read().encode("base64").split("\n"))
            lineOut = "{lat} {long} {img}\n".format(lat=lat, long=long, img=img)
            fout.write(lineOut)
            i += 1

with open("reducer-out.txt") as f:
    i = 0
    for line in f:
        split1 = line.find(" ")
        split2 = line.find(" ", split1 + 1)
        lat = line[:split1]
        long = line[split1 + 1:split2]
        img = line[split2 + 1:-1]
        with open(outpath + "/" + str(i) + ".jpg", "wb") as fout:
            fout.write(img.decode("base64"))
        i += 1