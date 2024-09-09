package com.statemachine.config;

import com.statemachine.model.Product;
import com.statemachine.model.Student;
import com.statemachine.repository.ProductRrepo;
import com.statemachine.repository.StudentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.SkipListenerSupport;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Configuration // Marks this class as a configuration class
@EnableBatchProcessing // Enables Spring Batch processing
public class BatchConfig {

    private final StudentRepo studentRepo; // Injects the repository to save student data
    private final JobRepository jobRepository; // Repository for managing jobs
    private final PlatformTransactionManager transactionManager; // Manages transactions
    private final ProductRrepo productRepo; // Injects the repository to save product data

    @Bean("itemReaderUsingPartition")
    @StepScope // Step-scoped bean to read chunks of data using partitioning
    public FlatFileItemReader<Student> itemReaderUsingPartition(@Value("#{stepExecutionContext[minValue]}") Integer minValue,
                                                                @Value("#{stepExecutionContext[maxValue]}") Integer maxValue) {
        FlatFileItemReader<Student> itemReader = new FlatFileItemReader<>();
        itemReader.setResource(new ClassPathResource("students_data.csv")); // CSV resource
        itemReader.setName("csvReader");
        itemReader.setLinesToSkip(1); // Skips header
        itemReader.setLineMapper(lineMapper()); // Maps CSV lines to Student objects
        return itemReader;
    }

    @Bean
    public FlatFileItemReader<Student> itemReader() {
        FlatFileItemReader<Student> itemReader = new FlatFileItemReader<>();
        itemReader.setResource(new ClassPathResource("students_data.csv")); // CSV resource
        itemReader.setName("csvReader");
        itemReader.setLinesToSkip(1); // Skips header
        itemReader.setLineMapper(lineMapper()); // Maps CSV lines to Student objects
        return itemReader;
    }

    private LineMapper<Student> lineMapper() {
        DefaultLineMapper<Student> defaultLineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
        delimitedLineTokenizer.setNames("id", "firstname", "lastname", "age"); // Columns in CSV
        delimitedLineTokenizer.setDelimiter(","); // Comma-separated
        delimitedLineTokenizer.setStrict(false);
        BeanWrapperFieldSetMapper<Student> beanWrapperFieldSetMapper = new BeanWrapperFieldSetMapper<>();
        beanWrapperFieldSetMapper.setTargetType(Student.class); // Maps fields to Student class
        defaultLineMapper.setFieldSetMapper(beanWrapperFieldSetMapper);
        defaultLineMapper.setLineTokenizer(delimitedLineTokenizer);
        return defaultLineMapper;
    }

    @Bean
    public StudentProcessor stdprocessor() {
        return new StudentProcessor(); // Processor for Student items
    }

    @Bean
    public RepositoryItemWriter<Student> itemWriter() {
        RepositoryItemWriter<Student> itemWriter = new RepositoryItemWriter<>();
        itemWriter.setRepository(studentRepo); // Repository for saving student data
        itemWriter.setMethodName("save"); // Saves the student data to the database
        return itemWriter;
    }

    @Bean
    public Step entryStep() {
        return new StepBuilder("entryStep", jobRepository)
                .partitioner(step1().getName(), new CustomizePartition()) // Partitions the data
                .partitionHandler(partitionHandler()) // Handles partition execution
                .build();
    }

    private PartitionHandler partitionHandler() {
        TaskExecutorPartitionHandler taskExecutorPartitionHandler = new TaskExecutorPartitionHandler();
        taskExecutorPartitionHandler.setTaskExecutor(taskExecutor()); // Executes partitions concurrently
        taskExecutorPartitionHandler.setGridSize(4); // 4 partitions
        taskExecutorPartitionHandler.setStep(step1()); // Step to execute in each partition
        return taskExecutorPartitionHandler;
    }

    @Bean
    public Step step1() {
        return new StepBuilder("csvImport", jobRepository)
                .<Student, Student>chunk(100, transactionManager) // Process 100 students at a time
                .reader(itemReaderUsingPartition(1,100000)) // Reads CSV data
                .processor(stdprocessor()) // Processes data
                .writer(itemWriter()) // Writes data to the database
                .faultTolerant() // Enables fault tolerance
                //.skipLimit(10) // Allows 10 skips
                .skipPolicy(new CustomSkipPlicy()) // Custom skip policy
                .listener(skipListener()) // Listener for skipped items
                .build();
    }

    @Bean
    public Job studentJob1() {
        return new JobBuilder("importStudents", jobRepository)
                //.start(entryStep())//start job for parallel processing
                .start(step1()) // Starts the job with step1
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(20); // Limits concurrent executions
        return taskExecutor;
    }

    // DataSource initializer for setting up database schema
    @Bean
    public DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
        databasePopulator.setIgnoreFailedDrops(false);
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setDataSource(dataSource);
        dataSourceInitializer.setDatabasePopulator(databasePopulator);
        return dataSourceInitializer;
    }

    // Product batch job configuration
    @Bean
    @StepScope
    public FlatFileItemReader<Product> productItemReader(@Value("#{jobParameters[filepath]}") String filepath) {
        FlatFileItemReader<Product> itemReader = new FlatFileItemReader<>();
        itemReader.setResource(new FileSystemResource(filepath)); // Reads CSV from file path
        itemReader.setName("productCsvReader");
        itemReader.setLinesToSkip(1); // Skips header
        itemReader.setLineMapper(productLineMapper()); // Maps CSV to Product objects
        return itemReader;
    }

    private LineMapper<Product> productLineMapper() {
        DefaultLineMapper<Product> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("id", "name", "category", "description", "price", "quantity"); // CSV columns
        tokenizer.setDelimiter(",");
        BeanWrapperFieldSetMapper<Product> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Product.class); // Maps fields to Product class
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return lineMapper;
    }

    @Bean
    public RepositoryItemWriter<Product> productItemWriter() {
        RepositoryItemWriter<Product> itemWriter = new RepositoryItemWriter<>();
        itemWriter.setRepository(productRepo); // Repository for saving products
        itemWriter.setMethodName("save"); // Saves products to the database
        return itemWriter;
    }

    @Bean
    public Step productStep1() {
        return new StepBuilder("productStep1", jobRepository)
                .<Product, Product>chunk(100, transactionManager) // Processes 100 products at a time
                .reader(productItemReader(null)) // Reads product data from CSV
                .processor(productProcessor()) // Processes product data
                .writer(productItemWriter()) // Writes product data to database
                //.faultTolerant() // Enables fault tolerance
               // .skipLimit(10) // Allows 10 skips
                //.skipPolicy(new CustomSkipPlicy())
                // Custom skip policy for exceptions
                .build();
    }

    @Bean
    public ProductProcessor productProcessor() {
        return new ProductProcessor(); // Processor for product items
    }

    @Bean
    public Job productJob() {
        return new JobBuilder("productJob", jobRepository)
                .start(productStep1()) // Starts the job with step1
                .build();
    }

    @Bean
    public SkipListener<Student, Student> skipListener() {
        return new SkipListenerSupport<>() {
            @Override
            public void onSkipInRead(Throwable t) {
                System.err.println("Skipped product in read due to error: " + t.getMessage());
            }

            @Override
            public void onSkipInProcess(Student student, Throwable t) {
                System.err.println("Skipped student in process: " + student + " due to error: " + t.getMessage());
            }

            @Override
            public void onSkipInWrite(Student student, Throwable t) {
                System.err.println("Skipped student in write: " + student + " due to error: " + t.getMessage());
            }
        };
    }

}
