import os, sys

basePath = '/media/florian/Data/lsde-data/'
baseFile = 'redFileSplit2-ab'
outFileSuffix = '-base64'
tmpSuffix = ".tmp"

if len(sys.argv) == 2:
    baseFile = sys.argv[1]

onlyTakeCertainSize = True
certainSize = 50

j = 0

print "counting lines in file"
num_lines = sum(1 for line in open(basePath + baseFile))

print "done, starting the conversion"

fileName = basePath + baseFile + outFileSuffix
if onlyTakeCertainSize:
    fileName += "-" + str(certainSize) + "px"
    num_lines /= 2

with open(fileName + tmpSuffix, "wb") as fout:
    with open(basePath + baseFile) as f:
        for line in f:

            # split the line
            lineSplit = line.split(";")

            # extract the code section and remove spaces
            tmpCode = lineSplit[2].split("\t")
            code = tmpCode[1].replace(" ", "")

            if onlyTakeCertainSize and int(float(tmpCode[0])) != certainSize:
                continue

            # split into 2 hex digits
            test = [code[i:i+2] for i in range(0, len(code), 2)]

            # iterate over code and decode hex
            out = ""
            for char in test:
                if len(char) == 2:
                    out += char.decode('hex')

            outLine = [lineSplit[0], lineSplit[1], out]
            fout.write(" ".join(outLine)+"\n")

            j += 1
            if j % 10000 == 0:
                print "{currentLine}/{total}".format(currentLine=j, total=num_lines)
            # if j == 6:
            #     break


os.rename(fileName + tmpSuffix, fileName)
