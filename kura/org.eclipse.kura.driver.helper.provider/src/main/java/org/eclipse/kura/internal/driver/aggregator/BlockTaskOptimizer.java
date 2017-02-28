/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.internal.driver.aggregator;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.kura.KuraException;

public class BlockTaskOptimizer {

    public static class SetTransferSizeHintCustomizer implements AggregationCustomizer {

        private int transferSizeHint;

        public SetTransferSizeHintCustomizer(int transferSizeHint) {
            this.transferSizeHint = transferSizeHint;
        }

        @Override
        public void beforeAggregation(BlockAggregator aggregator) {
            aggregator.setTransferSizeHint(transferSizeHint);
        }

        @Override
        public void afterAggregation(BlockAggregator aggregator) {
        }
    }

    public static void assignTasks(ToplevelBlockTask toplevelTask, List<BlockTask> tasks) {
        while (!tasks.isEmpty() && tasks.get(0).getEnd() <= toplevelTask.getEnd()) {
            BlockTask first = tasks.remove(0);
            toplevelTask.addChild(first);
        }
    }

    private static void optimizeInternal(ToplevelBlockTaskFactory blockLevelTaskFactory, List<BlockTask> tasks,
            List<BlockTask> result, AggregationCustomizer customizer, Mode mode) throws KuraException {
        tasks.sort(new Comparator<BlockTask>() {

            @Override
            public int compare(BlockTask o1, BlockTask o2) {
                return o1.getStart() - o2.getStart();
            }
        });
        BlockAggregator aggregator = new BlockAggregator();
        if (customizer != null) {
            customizer.beforeAggregation(aggregator);
        }
        for (BlockTask task : tasks) {
            aggregator.insertRequiredBlock(task.getStart(), task.getEnd());
        }
        List<BlockAggregator.Block> resultBlocks = aggregator.getBlocks();
        if (customizer != null) {
            customizer.afterAggregation(aggregator);
        }
        for (BlockAggregator.Block block : resultBlocks) {
            ToplevelBlockTask blockLevelTask = blockLevelTaskFactory.build(block.getStart(), block.getEnd(), mode);
            result.add(blockLevelTask);
            assignTasks(blockLevelTask, tasks);
        }
    }

    public static void optimize(ToplevelBlockTaskFactory blockLevelTaskFactory, List<BlockTask> tasks,
            List<BlockTask> result, AggregationCustomizer customizer) throws KuraException {

        LinkedList<BlockTask> readTasks = new LinkedList<>();
        LinkedList<BlockTask> writeTasks = new LinkedList<>();

        for (BlockTask task : tasks) {
            Mode taskMode = task.getMode();
            if (taskMode == Mode.UPDATE) {
                readTasks.add(task);
                writeTasks.add(task);
            } else if (taskMode == Mode.READ) {
                readTasks.add(task);
            } else {
                writeTasks.add(task);
            }
        }

        if (!readTasks.isEmpty()) {
            optimizeInternal(blockLevelTaskFactory, readTasks, result, customizer, Mode.READ);
        }
        if (!writeTasks.isEmpty()) {
            optimizeInternal(blockLevelTaskFactory, writeTasks, result, customizer, Mode.WRITE);
        }
    }
}
