package in.vanna.springs3.rest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class Controller {

    @Value("${document.bucket-name}")
    private String bucketName;

    @Autowired
    private AmazonS3 amazonS3;

    @DeleteMapping("/{fileName}")
    public ResponseEntity deleteDocument(@PathVariable String fileName) {
        log.info("Deleting file {}", fileName);
        amazonS3.deleteObject(bucketName, fileName);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<String> getAllDocuments() {
        return amazonS3.listObjectsV2(bucketName).getObjectSummaries().stream()
                .map(S3ObjectSummary::getKey)
                .collect(Collectors.toList());
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity uploadDocument(@RequestPart MultipartFile file) throws IOException {
        String tempFileName =  file.getOriginalFilename();
        File tempFile = new File(System.getProperty("java.io.tmpdir") + "/" + tempFileName);
        file.transferTo(tempFile);
        amazonS3.putObject(bucketName, file.getOriginalFilename(), tempFile);
        tempFile.deleteOnExit();
        return ResponseEntity.created(URI.create("SOME-LOCATION")).build();
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable String fileName) throws IOException {
        S3Object data = amazonS3.getObject(bucketName, fileName);
        S3ObjectInputStream objectContent = data.getObjectContent();
        byte[] bytes = IOUtils.toByteArray(objectContent);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        objectContent.close();
        return ResponseEntity
                .ok()
                .contentLength(bytes.length)
                .header("Content-type", "application/octet-stream")
                .header("Content-disposition", "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @GetMapping("/presigned-url/{fileName}")
    public String presignedUrl(@PathVariable String fileName) throws IOException {
        return amazonS3.generatePresignedUrl(bucketName, fileName, convertToDateViaInstant(LocalDate.now().plusDays(1))).toString();

    }

    private Date convertToDateViaInstant(LocalDate dateToConvert) {
        return java.util.Date.from(dateToConvert.atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }

}
