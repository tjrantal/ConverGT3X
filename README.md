A java class to decompress, and subsequently read accelerations from an [Actigraph .gt3x](https://github.com/actigraph/GT3X-File-Format) file.

Based on a fork of Jan Brond's https://github.com/jbrond/ActigraphLib/blob/master/Gt3xFile.java. I did not find a license notification in his code and I therefore assumed Creative Commons BY license (https://creativecommons.org/licenses/by/4.0), which applies to this project as well.

COMPILE
Use gradle to compile (from command line in this folder)
	gradle jar

USE
You will need to have Actigraph .gt3x files to test, place them in a folder called 'gt3x'
Run Script S01... with [Octave](https://www.gnu.org/software/octave/) (or Matlab)
	This will decompress the .gt3x file, and unpack the byte-packing
Run script S02... to visualise the first file of exported data on screen.

Written by Timo Rantalainen tjrantal at gmail dot com 2015 to 2019. 