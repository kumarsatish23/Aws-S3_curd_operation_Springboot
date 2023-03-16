package in.vanna.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.zeroturnaround.zip.ZipUtil;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.MultipleFileDownload;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.util.IOUtils;

@Service
public class S3Service {
	@Value("${bucketname}")
	private String bucketName;

	@Autowired
	private AmazonS3 amazonS3;

	public String uploadDocument(String id, MultipartFile file) throws Throwable, Throwable {
		if (amazonS3.listObjectsV2(new ListObjectsV2Request().withBucketName(bucketName).withPrefix(id.toLowerCase()))
				.getKeyCount() < 100) {
			String tempFileName = file.getOriginalFilename();
			File tempFile = new File(System.getProperty("java.io.tmpdir") + "/" + tempFileName);
			file.transferTo(tempFile);
			amazonS3.putObject(bucketName, id.toLowerCase() + "/" + tempFileName, tempFile);
			tempFile.deleteOnExit();
			return "upload success" + "\nFile Size:" + String.format("%.4f", (double) file.getSize() / (1024)) + "KB"
					+ "\nUpload time:" + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
		} else {
			return "this account is limited to 100 file delete some files";
		}
	}

	public List<S3ObjectSummary> listAllDocuments(String id) {
		return amazonS3.listObjectsV2(bucketName, id.toLowerCase()).getObjectSummaries();
	}

	public ListObjectsV2Result searchForFile(String id, String keyWord) {
		return amazonS3.listObjectsV2(
				new ListObjectsV2Request().withBucketName(bucketName).withPrefix(id.toLowerCase() + "/" + keyWord));
	}

	public ByteArrayResource downloadfile(String id, String fileName) throws IOException {
		S3Object data = amazonS3.getObject(bucketName, id + "/" + fileName);
		S3ObjectInputStream objectContent = data.getObjectContent();
		byte[] bytes = IOUtils.toByteArray(objectContent);
		ByteArrayResource resource = new ByteArrayResource(bytes);
		objectContent.close();
		return resource;
	}

	public String presignedUrl(String id, String fileName) {
		return amazonS3.generatePresignedUrl(bucketName, id + "/" + fileName,
				convertToDateViaInstant(LocalDate.now().plusDays(1))).toString();
	}

	private Date convertToDateViaInstant(LocalDate dateToConvert) {
		return java.util.Date.from(dateToConvert.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
	}

	public String deletefile(String id, String fileName) {
		amazonS3.deleteObject(bucketName, id + "/" + fileName);
		return "Successfully Deleted" + fileName;
	}

	public void downloadDir(String id)
			throws AmazonServiceException, AmazonClientException, InterruptedException, FileNotFoundException {
		TransferManager xfer_mgr = TransferManagerBuilder.standard().withS3Client(amazonS3).build();

		MultipleFileDownload xfer = xfer_mgr.downloadDirectory(bucketName, id, new File(id));
		xfer.waitForCompletion();
		ZipUtil.pack(new File(id), new File(id + ".zip"));

	}
	
	public String deleteDirectory(String id) {
	    ObjectListing objectList = this.amazonS3.listObjects( this.bucketName, id );
	    List<S3ObjectSummary> objectSummeryList =  objectList.getObjectSummaries();
	    String[] keysList = new String[ objectSummeryList.size() ];
	    int count = 0;
	    for( S3ObjectSummary summery : objectSummeryList ) {
	        keysList[count++] = summery.getKey();
	    }
	    DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest( bucketName ).withKeys( keysList );
	    this.amazonS3.deleteObjects(deleteObjectsRequest);
	    return "deletion success";
	}

}
