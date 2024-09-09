package com.statemachine.controller;

import com.statemachine.model.Student;
import com.statemachine.repository.StudentRepo;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

@RestController
@RequestMapping("/studentsData")
public class StudentJob1Controller {
    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private StudentRepo studentRepo;
    @Autowired
    private Job studentJob1;

    @PostMapping("/runjob1")
    public String job1() {
        JobParameters jobParameter = new JobParametersBuilder()
                .addLong("started", System.currentTimeMillis())
                .toJobParameters();
        try {
            jobLauncher.run(studentJob1, jobParameter);
        } catch (JobExecutionAlreadyRunningException
                 | JobInstanceAlreadyCompleteException
                 | JobParametersInvalidException
                 | JobRestartException e) {
            throw new RuntimeException(e);
        }
        return "Job execution completed";
    }

    @GetMapping("/getAllData")
    public List<Student> getAllData() {
        List<Student> all = studentRepo.findAll();
        int parallelism = 10;
        ForkJoinPool customThreadPool = new ForkJoinPool(parallelism);
        return customThreadPool.submit(() ->
                all.parallelStream().toList()
        ).join();

    }

}
