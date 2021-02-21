package com.crossover.FileOps.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@Service
public class FileOpsSrvImpl implements FileOpsSrv {

	@Autowired
    private AmazonS3 s3ws;
	
	@Value("${fupl.s3.bucket}")
	private String s3Bucket;
	
	@Value("${fSearch.maxKeys}")
	private Integer maxKeys;
	
    @Value("${fupl.maxFileSize}")
    private long maxFileSize;
    
    @Value("${fupl.minFileSize}")
    private long minFileSize;
    
    private boolean publicReadFlg = false;
    
	@Override
	public ResponseEntity<String> uploadFileSrv(MultipartFile multipartFile, String fileType, long fileSize,
			boolean enablePublicReadAccess, String fileDesc) {
//		HttpStatus.CREATED //200
//		HttpStatus.BAD_REQUEST //400
//		HttpStatus.INTERNAL_SERVER_ERROR //500

        File file;
		System.out.println("enablePublicReadAccess: "+ enablePublicReadAccess);
		this.publicReadFlg = enablePublicReadAccess;
		
		System.out.println("Calling convertMultiPartFileToFile");
		try {
			Pattern pattern = Pattern.compile(".*\\.(?:jpg|png|jpeg|JPG|PNG|JPEG)");
			System.out.println("fileSize:"+ fileSize);
			System.out.println("maxSize:"+ maxFileSize);
			if (fileSize > maxFileSize)
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File size Exceeded"); //400

			file = convertMultiPartFileToFile(multipartFile);
			System.out.println("fileName:"+ multipartFile.getOriginalFilename());
			if (multipartFile.getOriginalFilename() == null || multipartFile.getOriginalFilename().isEmpty()) {
				System.out.println("File name does not exist");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File name does not exist");  //400
            } else if (!pattern.matcher(multipartFile.getOriginalFilename()).matches()) {
            	System.out.println("Invalid filename" + multipartFile.getOriginalFilename());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid filename");  //400
            }
	        uploadFileToS3Bucket(s3Bucket, file, fileDesc);
	        file.delete();
	        
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()); //500
		}
		
		return ResponseEntity.status(HttpStatus.CREATED).body("File [" + multipartFile.getOriginalFilename() + "] has been uploaded successfully."); //200
	}
	
	private File convertMultiPartFileToFile(final MultipartFile multipartFile) throws Exception {
		
		System.out.println("In convertMultiPartFileToFile");

		File file = null;
        try {
        	file = new File(multipartFile.getOriginalFilename());
            System.out.println("file created");
            
        	final FileOutputStream fos = new FileOutputStream(file);
        	fos.write(multipartFile.getBytes());
            fos.close();
			System.out.println("convertMultiPartFileToFile done");
			
        } catch (final IOException ioe) {
        	System.out.println("Error converting the multi-part file to file= "+ ioe.getMessage());
        	throw new Exception(ioe);
        }
        return file;
    }

	private void uploadFileToS3Bucket(final String bucketName, final File file, String fileDesc) throws Exception {
	
	try {
		System.out.print("uploadFileToS3Bucket");
		Map<String,String> customMeta = new HashMap<String,String>();
//		customMeta.put("x-amz-meta-fileDesc", fileDesc);
		customMeta.put("fileDesc", fileDesc); //prefix will be added by aws
        String uplFileName = file.getName();
        System.out.println("Uploading file with name= " + uplFileName);
        ObjectMetadata fileMetadata = new ObjectMetadata();
        fileMetadata.setUserMetadata(customMeta);
        PutObjectRequest putObjectRequest = new PutObjectRequest(this.s3Bucket, uplFileName, new FileInputStream(file), fileMetadata);

        if (this.publicReadFlg) {
            putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead);
        }
        this.s3ws.putObject(putObjectRequest);
		System.out.println("uploadFileToS3Bucket done");
        
    } catch (AmazonServiceException ase) {
        System.out.println("AmazonServiceException [" + ase.getMessage() + "] occurred while uploading [" + file.getName() + "] ");
        ase.printStackTrace();
        throw new Exception(ase);
        
    } catch (SdkClientException sce) {
    	System.out.println("SdkClientException [" + sce.getMessage() + "] occurred while uploading [" + file.getName() + "] ");
        sce.printStackTrace();
        throw new Exception(sce);
        
    } catch (AmazonClientException ace) {
    	System.out.println("Caught an AmazonClientException: ");
    	System.out.println("Error Message: " + ace.getMessage());
    }
}
	@Override
	public ResponseEntity<String> searchFileSrv(String id, String fileDesc, String fileType, 
			String minSize, String maxSize,
			String pageNumber, String pageSize) {
		ObjectListing objectListing = null;
		String resp =null;
		int totalCnt =0;
//		ObjectListing respObjectListing = null;
		try {
			System.out.println("------------Listing All objects-----------------");
			try {
				if (minSize!= null)
	            	Integer.parseInt(minSize);
	            if (maxSize!= null)
		            Integer.parseInt(maxSize);
	            if (pageNumber!= null )
		            Integer.parseInt(pageNumber);
	            if (pageSize != null)
		            Integer.parseInt(pageSize);
	        } catch (NumberFormatException e) {
	        	System.out.println("Number validations failed");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Number validations failed");
			}
			System.out.println("minSize validation");
			if (minSize != null)
				if (Integer.parseInt(minSize) <1)
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("minSize validation failed");
			System.out.println("pageSize validation");
			if (pageSize != null)
				if (Integer.parseInt(pageSize) <1)
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("pageSize validation failed");
			System.out.println("pageNumber validation");
			if (pageNumber != null)
				if (Integer.parseInt(pageNumber) <0) {
					System.out.println("pageNumber validation failed:" +pageNumber);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("pageNumber validation failed");
				}
			System.out.println("Getting objects from s3");
				objectListing = s3ws.listObjects(new ListObjectsRequest()
    		        .withBucketName(s3Bucket)
    		        .withMaxKeys(maxKeys)
    		        );
			
			System.out.println("Printing objects");
			for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
			    System.out.println(objectSummary.getKey() + "  " +
			            "(size = " + objectSummary.getSize() + ")");
			    ObjectMetadata objMeta = s3ws.getObjectMetadata(s3Bucket, objectSummary.getKey());
			    Map meta = objMeta.getUserMetadata();
//				    Map rawMeta = objMeta.getRawMetadata();
			    meta.forEach((key, value) -> System.out.println(key + ":" + value));
			    System.out.println("objMeta = "+ meta.get("fileDesc"));
			    resp+=objMeta.getRawMetadata();
			    System.out.println("resp = "+ resp + "\n fileDesc " +meta.get("fileDesc"));
	    		totalCnt++;
//				    rawMeta.forEach((key, value) -> System.out.println(key + ":" + value));
			}
			
//			IndexQuery indexQuery = new IndexQueryBuilder().withId(file.getId()).withObject(file).build();
//			String fileId = elasticsearchOperations.index(indexQuery, IndexCoordinates.of(PRODUCT_INDEX));
			
			List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
			List<String> lst = new ArrayList<>();
//			do {
			    lst.addAll(objectSummaries.stream()
			    		.filter(maxSize != null ? s -> (s.getSize()< Integer.parseInt(maxSize)):s->true)
			    		.filter(fileType != null ? s -> (s.getKey().toLowerCase().contains(fileType.toLowerCase())):s->true)
			            .map(S3ObjectSummary::getKey)
//			            .filter(fileType != null ? s -> s.toLowerCase().contains(fileType.toLowerCase()): s -> true)
			            .collect(Collectors.toList()));
			    lst.forEach((s) -> System.out.println("listkey:"+s));
//			    objectListing = s3ws.listNextBatchOfObjects(objectListing);
//			    objectSummaries = objectListing.getObjectSummaries();
				if (lst.size() == 0)
//			    if (totalCnt == 0)
					return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No data found"); //204
//			} while (!objectSummaries.isEmpty());
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.add("Status", "Success");
			return ResponseEntity
					.ok()
					.headers(responseHeaders)
					.body(resp);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}

}
