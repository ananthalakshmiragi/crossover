package com.crossover.FileOps.services;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface FileOpsSrv {
	
	ResponseEntity<String> uploadFileSrv(MultipartFile multipartFile, String fileType,
			long fileSize, boolean enablePublicReadAccess, String fileDesc);
	
	ResponseEntity<String> searchFileSrv(String id, String fileDesc, String fileType, String minSize, String maxSize,
			String pageNumber, String pageSize);
}
