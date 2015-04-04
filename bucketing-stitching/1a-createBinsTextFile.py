picsPerZoomLevel = { # only y axis, i.e. full image for zoom level 0 is 50x100, 4 is 100x200
    0: 50,
    4: 100,
    8: 200,
    12: 500,
    16: 1000
}

for zoom, yRes in picsPerZoomLevel.iteritems():
    picsXaxis = yRes * 2
    picsYaxis = yRes

    gridSpace = 180.0 / picsYaxis

    coordsX = [(x * gridSpace) - 180 for x in range(picsXaxis)] + [180]
    coordsY = [(y * gridSpace) - 90 for y in range(picsYaxis)] + [90]


    with open("mapperJobs-z{zoom}.txt".format(zoom=zoom), "wb") as fout:
        for j in range(0, len(coordsY)-1):
            yPair = (coordsY[j], coordsY[j+1])
            fout.write("{zoom},{row},{left},{right}\n".format(left=yPair[0], right=yPair[1], zoom=zoom, row=j))






