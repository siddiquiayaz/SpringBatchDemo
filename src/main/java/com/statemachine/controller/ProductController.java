package com.statemachine.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@RestController
@RequestMapping("/products")
public class ProductController {
    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    protected Job productJob;
    @PostMapping("/uploadCsv")
    public String uploadCsv(@RequestParam("file") MultipartFile file) throws IOException {
        //File tempFile = File.createTempFile("products", ".csv");
       // File tempFile = new File(STR."\{System.getProperty("java.io.tmpdir")}/\{file.getOriginalFilename()}");
       // file.transferTo(tempFile);
        Path tempDir = Files.createTempDirectory("");
        Path tempFile = tempDir.resolve(Objects.requireNonNull(file.getOriginalFilename()));
        Files.write(tempFile, file.getBytes());
     //   Resource resource = new FileSystemResource(tempFile);
        //  Resource resource = new ByteArrayResource(file.getBytes());
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("filepath", tempFile.toString())
                .addLong("execution-started",System.currentTimeMillis())
                .toJobParameters();

        try {
            jobLauncher.run(productJob ,jobParameters);
        } catch (JobExecutionAlreadyRunningException
                 | JobParametersInvalidException
                 | JobInstanceAlreadyCompleteException
                 | JobRestartException e) {
           return STR."job execution failed\{e.getMessage()}";
        }
             return "Job executed successfully!";

    }


}
