package org.eclipse.kura.internal.driver.aggregator.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.internal.driver.aggregator.AggregationCustomizer;
import org.eclipse.kura.internal.driver.aggregator.BlockAggregator;
import org.eclipse.kura.internal.driver.aggregator.BlockTask;
import org.eclipse.kura.internal.driver.aggregator.BlockTaskOptimizer;
import org.eclipse.kura.internal.driver.aggregator.Mode;
import org.eclipse.kura.internal.driver.aggregator.ToplevelBlockTask;
import org.eclipse.kura.internal.driver.aggregator.ToplevelBlockTaskFactory;
import org.junit.Assert;
import org.junit.Test;

public class BlockTaskOptimizerTest {

    private interface TaskListener {

        public void onRun(BlockTask task, BlockTask parent);

        public void onFail(BlockTask task, BlockTask parent);
    }

    private class TestTask extends ToplevelBlockTask {

        private TaskListener listener;

        public TestTask(int start, int end, TaskListener listener) {
            super(start, end, Mode.READ);
            this.listener = listener;
        }

        @Override
        public void run(ToplevelBlockTask parent) throws IOException {
            if (listener != null) {
                listener.onRun(this, parent);
            }
            runChildren();
        }

        @Override
        public void fail(ToplevelBlockTask parent, String reason) {
            if (listener != null) {
                listener.onFail(this, parent);
            }
            failChildren(reason);
        }

        @Override
        public void succeed(ToplevelBlockTask parent) {
            // TODO Auto-generated method stub

        }
    }

    private class TestToplevelTaskFactory implements ToplevelBlockTaskFactory {

        private TaskListener listener;

        public TestToplevelTaskFactory(TaskListener listener) {
            this.listener = listener;
        }

        @Override
        public ToplevelBlockTask build(int start, int end, Mode mode) {
            return new TestTask(start, end, listener);
        }

    }

    private class TestHelper {

        private TaskListener taskListener = new TaskListener() {

            @Override
            public void onRun(BlockTask task, BlockTask parent) {
                if (parent != null) {
                    Assert.assertTrue(task.getStart() >= parent.getStart());
                    Assert.assertTrue(task.getEnd() <= parent.getEnd());
                }
            }

            @Override
            public void onFail(BlockTask task, BlockTask parent) {
                Assert.fail();
            }
        };

        private AggregationCustomizer aggregationListener = new AggregationCustomizer() {

            @Override
            public void beforeAggregation(BlockAggregator aggregator) {
                aggregator.setTransferSizeHint(transferSizeHint);
                if (prohibitedBlocks != null) {
                    for (int i = 0; i < prohibitedBlocks.length; i += 2) {
                        try {
                            aggregator.insertProhibitedBlock(prohibitedBlocks[i], prohibitedBlocks[i + 1]);
                        } catch (KuraException e) {
                            Assert.fail(e.getMessage());
                        }
                    }
                }
            }

            @Override
            public void afterAggregation(BlockAggregator aggregator) {
            }
        };

        private int[] inputBlocks;
        private int[] prohibitedBlocks;
        private int[] outputBlocks;
        private int transferSizeHint = 0;
        BlockAggregator aggregator;

        private TestHelper() {
        }

        public TestHelper setInput(int... inputBlocks) {
            if (inputBlocks.length % 2 != 0) {
                fail("block list size must be a multiple of 2");
            }
            this.inputBlocks = inputBlocks;
            return this;
        }

        public TestHelper expect(int... outputBlocks) {
            if (outputBlocks.length % 2 != 0) {
                fail("block list size must be a multiple of 2");
            }
            this.outputBlocks = outputBlocks;
            return this;
        }

        public TestHelper prohibit(int... prohibitedBlocks) {
            if (prohibitedBlocks.length % 2 != 0) {
                fail("block list size must be a multiple of 2");
            }
            this.prohibitedBlocks = prohibitedBlocks;
            return this;
        }

        public TestHelper setTransferSizeHint(int transferSizeHint) {
            this.transferSizeHint = transferSizeHint;
            return (this);
        }

        public TestHelper exec() throws KuraException {
            List<BlockTask> tasks = new LinkedList<BlockTask>();
            if (inputBlocks != null) {
                for (int i = 0; i < inputBlocks.length; i += 2) {
                    tasks.add(new TestTask(inputBlocks[i], inputBlocks[i + 1], taskListener));
                }
            }
            List<BlockTask> result = new LinkedList<BlockTask>();
            BlockTaskOptimizer.optimize(new TestToplevelTaskFactory(taskListener), tasks, result, aggregationListener);

            if (outputBlocks != null) {
                assertEquals(outputBlocks.length / 2, result.size());

                for (int i = 0; i < result.size(); i++) {
                    assertEquals(outputBlocks[i * 2], result.get(i).getStart());
                    assertEquals(outputBlocks[i * 2 + 1], result.get(i).getEnd());
                }
            }

            int childCount = 0;
            for (BlockTask task : result) {
                ToplevelBlockTask t = (ToplevelBlockTask) task;
                childCount += t.getChildren().size();
                try {
                    task.run(null);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            if (inputBlocks != null) {
                assertEquals(inputBlocks.length / 2, childCount);
            }
            return this;
        }
    }

    @Test
    public void shouldOptimizeSingleTask() throws KuraException {
        new TestHelper().setInput(0, 1).expect(0, 1).exec();
    }

    @Test
    public void shouldOptimizeAdjacentBlocks() throws KuraException {
        new TestHelper().setInput(0, 1, 1, 2, 2, 3).expect(0, 3).exec();
    }

    @Test
    public void shouldSupportBlockInsertion() throws KuraException {
        new TestHelper().setInput(0, 1).expect(0, 1).exec();
    }

    @Test
    public void shouldAggregateAdjacentBlocks() throws KuraException {
        new TestHelper().setInput(0, 1, 1, 2).expect(0, 2).exec();
        new TestHelper().setInput(1, 2, 0, 1).expect(0, 2).exec();
        new TestHelper().setInput(0, 1, 1, 2, 2, 10).expect(0, 10).exec();
        new TestHelper().setInput(2, 10, 1, 2, 0, 1).expect(0, 10).exec();
        new TestHelper().setInput(0, 1, 1, 2, 3, 4, 2, 3).expect(0, 4).exec();
    }

    @Test
    public void shouldNotAggregateNonAdjacentBlocks() throws KuraException {
        new TestHelper().setInput(0, 1, 2, 3).expect(0, 1, 2, 3).exec();
    }

    @Test
    public void shouldSupportOverlappingBlocks() throws KuraException {
        new TestHelper().setInput(1, 9, 2, 4).expect(1, 9).exec();
        new TestHelper().setInput(1, 9, 2, 4, 3, 6).expect(1, 9).exec();
        new TestHelper().setInput(0, 1, 0, 1, 0, 1).expect(0, 1).exec();
        new TestHelper().setInput(0, 1, 1, 3, 20, 25, 15, 21).expect(0, 3, 15, 25).exec();
    }

    @Test
    public void shouldSupportProhibitedBlocks() throws KuraException {
        new TestHelper().prohibit(0, 1).expect().exec();
        new TestHelper().setInput(0, 1, 1, 2, 3, 4).prohibit(2, 3).expect(0, 2, 3, 4).exec();
    }

    @Test(expected = KuraException.class)
    public void shouldReportUnfeasibleProblem1() throws KuraException {
        new TestHelper().setInput(0, 2).prohibit(0, 1).expect().exec();
    }

    @Test(expected = KuraException.class)
    public void shouldReportUnfeasibleProblem2() throws KuraException {
        new TestHelper().setInput(0, 2).prohibit(1, 2).expect().exec();
    }

    @Test(expected = KuraException.class)
    public void shouldReportUnfeasibleProblem3() throws KuraException {
        new TestHelper().setInput(0, 2).prohibit(1, 3).expect().exec();
    }

    @Test
    public void shouldAggregateAccordingToTransferSizeHint() throws KuraException {
        new TestHelper().setInput(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).expect(0, 9).setTransferSizeHint(10).exec();
        new TestHelper().setInput(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).expect(0, 5, 6, 9).setTransferSizeHint(5).exec();
    }

    @Test
    public void shouldSupportProhibitedBlocksWithTransferSizeHint() throws KuraException {
        new TestHelper().setInput(0, 1, 2, 3, 8, 9).expect(0, 9).setTransferSizeHint(10).exec();
        new TestHelper().setInput(0, 1, 2, 3, 8, 9).prohibit(4, 7).expect(0, 3, 8, 9).setTransferSizeHint(10).exec();
    }
}
