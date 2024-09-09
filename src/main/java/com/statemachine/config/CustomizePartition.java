package com.statemachine.config;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CustomizePartition implements Partitioner {

    // This method is overridden to provide the partition logic.
    // It divides the data set into partitions based on gridSize (number of partitions).
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        // Creates a HashMap to hold the partition information.
        HashMap<String, ExecutionContext> executionContextHashMap = new HashMap<>();

        // Defines the partition size, meaning each partition will handle 1000 items.
        int partitionSize = 1000;

        // Loops through the grid size to create multiple partitions.
        for (int i = 0; i < gridSize; i++) {
            // Creates a new ExecutionContext for each partition.
            // ExecutionContext is used to store state information that can be passed to each partition.
            ExecutionContext executionContext = new ExecutionContext();

            // Assigns the minimum value (start index) for the current partition.
            executionContext.putInt("minValue", i * partitionSize);

            // Assigns the maximum value (end index) for the current partition.
            // Subtracts 1 to ensure it's the last item of this partition.
            executionContext.putInt("maxValue", (i + 1) * partitionSize-1);
            executionContextHashMap.put("partition" +i, executionContext);
        }
        return executionContextHashMap;

    }

    }
