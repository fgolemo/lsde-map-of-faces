from mrjob.job import MRJob
from bisect import bisect_left

class LSDEbinning(MRJob):

    filePath = "/media/florian/Data/lsde-data/redFileSplit2-test"
    outPath = "/media/florian/Data/lsde-data/binning-output/"

    picsPerZoomLevel = { # only y axis, i.e. full image for zoom level 0 is 50x100, 4 is 100x200
        0: 50,
        4: 100,
        8: 200,
        12: 500,
        16: 1000
    }
    coordsX = []
    row = 0
    yTop = 90
    yBottom = -90

    def configure_options(self):
        super(LSDEbinning, self).configure_options()
        self.add_file_option('--picfile')

    def mapper_init(self):
        self.filePath = self.options.picfile

    def setCoordsList(self, line):
        lineSplit = line.split(',')
        picsXaxis = self.picsPerZoomLevel[int(lineSplit[0])] * 2
        picsYaxis = self.picsPerZoomLevel[int(lineSplit[0])]

        gridSpace = 180.0 / picsYaxis
        self.coordsX = [(x * gridSpace) - 180 for x in range(picsXaxis)] + [180]

        self.row = int(lineSplit[1])
        self.yBottom = float(lineSplit[2])
        self.yTop = float(lineSplit[3])

    def whichCell(self, coordX):
        if coordX >= 0:
            lo = int(round(len(self.coordsX) / 2) - 1)
            hi = len(self.coordsX)
        else:
            lo = 0
            hi = int(round(len(self.coordsX) / 2) + 1)
        x = bisect_left(self.coordsX, coordX, lo, hi) - 1
        return x

    def isCorrectRow(self, long):
        if self.yBottom <= long < self.yTop:
            return True
        return False

    def mapper(self, _, task):
        # each line contains: zoomlevel, row number, bottom Y coordinate, top Y coordinate
        # therefore each mapper gets a cell in the grid and has to find images for each cell in his row

        self.setCoordsList(task)

        cols = len(self.coordsX) + 1

        done = [0] * cols
        out = [0] * cols

        with open(self.filePath) as f:
            for fileLine in f:
                split1 = fileLine.find(" ")
                split2 = fileLine.find(" ", split1 + 1)
                lat = float(fileLine[:split1])
                long = float(fileLine[split1 + 1:split2])

                if not self.isCorrectRow(long):
                    continue
                col = self.whichCell(lat)
                if done[col] == 0:
                    done[col] = 1
                    out[col] = fileLine[split2 + 1:-1]
            yield self.row, out

    def reducer(self, key, values):
        tmpVals = []
        for tmpVal in values: # in case there is more than one set of values, concat them
            tmpVals += tmpVal
        out = ",".join(str(v) for v in tmpVals)
        yield key, out


if __name__ == '__main__':
    LSDEbinning.run()