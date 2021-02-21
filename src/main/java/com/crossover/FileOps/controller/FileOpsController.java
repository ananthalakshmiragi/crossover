package com.crossover.FileOps.controller;

import javax.validation.constraints.NotEmpty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.crossover.FileOps.services.FileOpsSrv;


@Validated
@RestController
@RequestMapping("/api")
public class FileOpsController {
	
	@Autowired
	private FileOpsSrv fOpsSrv;

	@PostMapping(value= "/images")
    public ResponseEntity<String> uploadFile(@RequestPart(value = "file") MultipartFile file,
    										 @RequestPart(value = "description") @NotEmpty String fileDesc)
    {
		System.out.println("----------------uploadFile----------------");
		ResponseEntity<String> resp = null;
		System.out.println("Type:"+file.getContentType());
		System.out.println("Size:"+file.getSize());
		resp = this.fOpsSrv.uploadFileSrv(file, file.getContentType(), file.getSize(), true, fileDesc);
		System.out.println(resp);
		
        return resp; //201
    }
	
	@GetMapping("/images")
	@ResponseBody
    public ResponseEntity<String> searchFileSrv
    		(@RequestParam(value = "id",required = false) String id, 
    		@RequestParam(value = "description",required = false) String fileDesc,
    		@RequestParam(value = "type",required = false) String fileType, 
//    		@RequestParam(value = "minSize",required = false) @Min(1) Long minSize,
    		@RequestParam(value = "minSize",required = false) String minSize,
    		@RequestParam(value = "maxSize",required = false) String maxSize,
    		@RequestParam(value = "pageNumber",required = false) String pageNumber,
    		@RequestParam(value = "pageSize",required = false) String pageSize)
    {
//		HttpStatus.OK //200
//		HttpStatus.NO_CONTENT //204
//		HttpStatus.BAD_REQUEST //400
		
//		@Min(value = 1, message = "Minimum file size should be 1")
		System.out.println("Search called.....");
		
		ResponseEntity<String> resp = null;

		resp = this.fOpsSrv.searchFileSrv(id,fileDesc,fileType,
				minSize,maxSize,pageNumber,pageSize);
//	    return new ResponseEntity<>(fileList, HttpStatus.OK); //200
        return resp;
    }
}
