package com.statemachine.config;

import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.IOException;

public class CustomSkipPlicy  implements SkipPolicy {
    @Override
    public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException
    {
       if (t instanceof DataIntegrityViolationException && skipCount <= 4) {
           return true;
       }
        //If an IOException or FlatFileParseException occurs,
        // the job will fail because these exceptions are non-skippable.
        if (t instanceof IOException || t instanceof FlatFileParseException) {
            return false;
        }


  return true;


    }
}
