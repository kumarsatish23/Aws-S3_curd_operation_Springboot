package in.vanna.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.file.ConfigurationSource.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.zeroturnaround.zip.ZipUtil;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.MultipleFileDownload;

import in.vanna.service.S3Service;

@RestController
public class S3Controller {
	@Autowired
	S3Service s3Service;

	@PostMapping(value = "/uploadDocument/{id}", consumes = { "multipart/form-data" })
	public String uploadDocument(@PathVariable String id, @RequestPart MultipartFile file) throws Throwable {
		if (file.getSize() > 1024 * 1024 * 10) {
			return file.getOriginalFilename() + " size is exceeds 10MB";
		}
		return s3Service.uploadDocument(id, file);
	}

	@GetMapping(value = "/GetAllDocuments/{id}")
	public List<S3ObjectSummary> getAllDocumnetsList(@PathVariable String id) {
		return s3Service.listAllDocuments(id);
	}

	@GetMapping(value = "/SearchFile/{id}/{keyWord}")
	public ListObjectsV2Result SearchFile(@PathVariable String id, String keyWord) {
		return s3Service.searchForFile(id, keyWord);
	}

	@GetMapping(value = "/DownloadFile/{id}/{filename}")
	public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable String id, String filename) throws IOException {
		ByteArrayResource file = s3Service.downloadfile(id, filename);
		return ResponseEntity.ok()
				.header("file-size", String.format("%.4f", (double) file.contentLength() / (1024 * 1024)) + "MB")
				.header("Content-type", "application/octet-stream")
				.header("Content-disposition", "attachment; filename=\"" + filename + "\"").body(file);
	}

	@GetMapping(value = "/ShareURL/{id}/{filename}")
	public String shareLink(@PathVariable String id, String filename) {
		return s3Service.presignedUrl(id, filename);
	}

	@DeleteMapping(value = "/Delete/{id}/{fileName}")
	public String deleteFile(@PathVariable String id, String fileName) {
		return s3Service.deletefile(id, fileName);
	}

	@GetMapping(value = "/DownloadDir/{id}")
	public @ResponseBody ResponseEntity<StreamingResponseBody> downloadDirectory(@PathVariable String id)
			throws AmazonServiceException, AmazonClientException, InterruptedException, IOException {

		s3Service.downloadDir(id);
		final String resourceName = id + ".zip";
		final File iFile = new File(resourceName);
		final long resourceLength = iFile.length();
		final long lastModified = iFile.lastModified();
		InputStream inputStream = new FileInputStream(iFile);
		StreamingResponseBody body = outputStream -> FileCopyUtils.copy(inputStream, outputStream);

		return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=" + resourceName)
				.contentLength(resourceLength).lastModified(lastModified).body(body);

	}
	@DeleteMapping(value="/DeleteId/{id}")
	public String deleteId(@PathVariable String id) {
		return s3Service.deleteDirectory(id);
	}
}
