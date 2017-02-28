package org.eclipse.kura.internal.driver.aggregator.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.internal.driver.aggregator.BlockAggregator;
import org.eclipse.kura.internal.driver.aggregator.BlockAggregator.Block;
import org.junit.Test;

public class BlockAggregatorTest {

    private static class TestHelper {

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

        public TestHelper setTransferSizeHint(int transferSizeHint) {
            this.transferSizeHint = transferSizeHint;
            return (this);
        }

        public TestHelper prohibit(int... prohibitedBlocks) {
            if (prohibitedBlocks.length % 2 != 0) {
                fail("block list size must be a multiple of 2");
            }
            this.prohibitedBlocks = prohibitedBlocks;
            return this;
        }

        public TestHelper assertHasBlockFor(int... addrs) {
            for (int i = 0; i < addrs.length; i++) {
                assertNotNull(aggregator.getDataBlock(addrs[i]));
            }
            return this;
        }

        public TestHelper assertHasNotBlockFor(int... addrs) {
            for (int i = 0; i < addrs.length; i++) {
                assertNull(aggregator.getDataBlock(addrs[i]));
            }
            return this;
        }

        public TestHelper exec() throws KuraException {
            aggregator = new BlockAggregator();
            aggregator.setTransferSizeHint(transferSizeHint);
            if (inputBlocks != null) {
                for (int i = 0; i < inputBlocks.length; i += 2) {
                    aggregator.insertRequiredBlock(inputBlocks[i], inputBlocks[i + 1]);
                }
            }
            if (prohibitedBlocks != null) {
                for (int i = 0; i < prohibitedBlocks.length; i += 2) {
                    aggregator.insertProhibitedBlock(prohibitedBlocks[i], prohibitedBlocks[i + 1]);
                }
            }
            List<Block> blocks = aggregator.getBlocks();

            if (outputBlocks != null) {
                assertEquals(outputBlocks.length / 2, blocks.size());

                for (int i = 0; i < blocks.size(); i++) {
                    assertEquals(outputBlocks[i * 2], blocks.get(i).getStart());
                    assertEquals(outputBlocks[i * 2 + 1], blocks.get(i).getEnd());
                }
            }
            return this;
        }
    }

    @Test
    public void shouldSupportEmptyList() throws KuraException {
        assertEquals(0, new BlockAggregator().getBlocks().size());
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

    @Test
    public void shouldSupportGettingBlockByAddress() throws KuraException {
        new TestHelper().setInput(0, 1, 4, 5, 20, 25, 8, 10).expect(0, 1, 4, 5, 8, 10, 20, 25).exec()
                .assertHasBlockFor(0, 4, 8, 9, 20, 21, 22, 23, 24)
                .assertHasNotBlockFor(1, 5, 10, 25, 2, 6, 12, 26, 100, -45);
        new TestHelper().setInput(0, 1, 1, 3, 20, 25, 15, 21).expect(0, 3, 15, 25).exec()
                .assertHasBlockFor(0, 1, 2, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24)
                .assertHasNotBlockFor(3, 4, 25, 26, 10, 6, 12, 27, 100, -45);
    }

}
