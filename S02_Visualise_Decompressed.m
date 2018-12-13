%%Read data in max 24 h chuncks to not run out of memory


close all;
fclose all;
clear all;
clc;

javaaddpath('build/libs/gt3x-1.0.jar');	%Add gt3x reader
dataPath = 'decompressedData/';
addpath('functions');

%For reading the byte-unpacked data
recordType = {'int16' 'int16' 'int16' 'int32'};
recordLen = [2 2 2 4]; %

%Create InfoReader object for sample rate
if ~exist('infoReader','var')
    infoReader = javaObject('com.video4coach.gt3x.ActigraphInfoParser');
end

fList = getFilesAndFolders(dataPath);	%Get a list of .gt3x files to handle
%Loop through the .gt3x files

for i = 1
	%Decompress the gt3x file
	disp(['Visualising participant ' fList(i).name]);
   %Decompress, if the data has not yet been decompressed
	%Read sample rate from info.txt
	info = javaMethod('extractInfo',infoReader,[dataPath fList(i).name '/info.txt']);
	sampleRate = javaMethod('getSampleRate',info);
	scale = javaMethod('getAccelerationScale',info);
	
	
	
	fid = fopen([dataPath fList(i).name '/' 'accelerations.bin'],'rb','ieee-le');
	channels = cell(1,numel(recordType));
	%Get file size
	fseek(fid,0,'eof');
	bytesOfData = ftell(fid);
	dataRows = int32(ceil(bytesOfData/sum(recordLen)));
	fseek(fid,0,'bof');
	chunksize = (sampleRate*60*60*24); %Read in 24 hour chunks

	%maxChuncksToRead = min(7*chunksize*sum(recordLen), bytesOfData);    %Read a maximum of 7 days

	maxChuncksToRead = bytesOfData;
    pointer = 0;
	%Figure out time stamps... Read all timestamps up to maxChuncksToRead
	 fseek(fid, 0, 'bof');                  %Get to beginning of the current chunk
     accumMads = [];
     accumtStamps = [];
     figure
     axes();
     hold on;
	while pointer < bytesOfData

        chunksize = min([chunksize, (bytesOfData-pointer)/sum(recordLen)]);
		tempData = zeros(chunksize,4);
		for i=1:numel(recordType)
			% seek to the first field of the first record
			fseek(fid, pointer, 'bof');                  %Get to beginning of the current chunk
			fseek(fid, sum(recordLen(1:(i-1))), 'cof');   %skip to correct var
			% read column with specified format, skipping required number of bytes
			tempData(:,i) = double(fread(fid, chunksize, ['*' recordType{i}], sum(recordLen)-recordLen(i)));
		end
		pointer = pointer+chunksize*sum(recordLen);
        resultant = sqrt(sum((double(tempData(:,1:3))./scale).^2,2));
		timeStamps = 1000*int64(tempData(:,4));	%unix ms timestamps
        
        %Calculate 1 s mads for visualisation
        remainder = mod(length(resultant),sampleRate);
        if  remainder ~= 0
            %Discard last datapoints that do not amount to an equal zero..
           resultants = resultant(1:end-remainder);
           timeStamps = timeStamps(1:end-remainder);
        end
        buffered = reshape(resultant,[sampleRate,size(resultant,1)/sampleRate]);
        bufferedTime = reshape(timeStamps,[sampleRate,size(timeStamps,1)/sampleRate]);
        meanVals = mean(buffered,1);
        meanShifted = bsxfun(@minus,buffered,meanVals);
        mads = mean(abs(meanShifted));
        tStamps = bufferedTime(1,:);
%         keyboard;
        accumMads = [accumMads, mads];
        accumtStamps = [accumtStamps,tStamps];
        
		plot(tStamps-accumtStamps(1),mads);
    end
    fclose(fid);
		
                
end


