package com.statemachine.config;

import com.statemachine.model.Student;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class StudentProcessor implements ItemProcessor<Student,Student> {
    @Override
    public Student process(Student item) throws Exception {
        if (item.getAge() < 0)
            throw new DataIntegrityViolationException("Negative age encountered for students: " + item.getFirstname() + " " + item.getLastname());

        return item;

    }
}
