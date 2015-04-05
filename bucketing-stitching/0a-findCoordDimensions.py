filePath = "/media/florian/Data/lsde-data/redFileSplit2-combined-base64-50px2"
zoom = str(20)
aMin = 999
aMax = -999
bMin = 999
bMax = -999

num_lines = sum(1 for line in open(filePath))

i=0
with open(filePath) as f:
    for fileLine in f:
        split1 = fileLine.find(" ")
        split2 = fileLine.find(" ", split1 + 1)
        a = float(fileLine[:split1])
        b = float(fileLine[split1 + 1:split2])
        if a < aMin:
            aMin = a
        if a > aMax:
            aMax = a
        if b < bMin:
            bMin = b
        if b > bMax:
            bMax = b
    i += 1
    if i % 10000 == 0:
        print "{cur}/{total}".format(cur=i, total=num_lines)

print aMin, aMax, bMin, bMax