close all;
fclose all;
clear all;
clc;

javaaddpath('build/libs/gt3x-1.0.jar');	%Add gt3x reader
gt3xPath = 'gt3x/';
decompressedFilePath = 'decompressedData/';

if ~exist(decompressedFilePath,'dir')
	mkdir(decompressedFilePath);
end

fList = dir([gt3xPath '*.gt3x']);	%Get a list of .gt3x files to handle
%Loop through the .gt3x files
for i = 1:length(fList)
	%Decompress the gt3x file
	disp(['Starting file ' fList(i).name]);
   %Decompress, if the data has not yet been decompressed
	if ~exist([decompressedFilePath '/' fList(i).name(1:end-5) '/accelerations.bin'])
		%Decompress the zip file
		if ~exist([decompressedFilePath '/' fList(i).name(1:end-5) ])
		   mkdir([decompressedFilePath '/' fList(i).name(1:end-5) ]);
		end
		%Unzip the .gt3x
		javaMethod('unzipActivityFile','com.video4coach.DecompressGT3x',[gt3xPath fList(i).name],[decompressedFilePath fList(i).name(1:end-5) '/']);
		%read the bin file, and remove the byte-packing
		binFile = dir([decompressedFilePath '/' fList(i).name(1:end-5) '/*.bin']);
		javaObject('com.video4coach.LogBinFileReader',[decompressedFilePath fList(i).name(1:end-5) '/' binFile(1).name ],[decompressedFilePath fList(i).name(1:end-5) '/accelerations.bin']);
	end
end
