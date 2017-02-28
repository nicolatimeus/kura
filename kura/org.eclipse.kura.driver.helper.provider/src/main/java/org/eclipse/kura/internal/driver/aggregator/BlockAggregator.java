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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

public class BlockAggregator {

    private LinkedList<Block> blocks = new LinkedList<>();
    private boolean hasProhibitedBlocks = false;
    private boolean isReady = false;
    private int transferSizeHint = 0;

    public List<Block> getBlocks() {
        if (!isReady) {
            process();
        }
        return blocks;
    }

    public Block getDataBlock(int addr) {
        if (!isReady) {
            process();
        }
        for (Block b : blocks) {
            if (b.start > addr) {
                return null;
            }
            if (b.contains(addr)) {
                return b;
            }
        }
        return null;
    }

    public void insertRequiredBlock(int start, int end) throws KuraException {
        insertBlock(new Block(start, end, false));
    }

    public void insertProhibitedBlock(int start, int end) throws KuraException {
        insertBlock(new Block(start, end, true));
        hasProhibitedBlocks = true;
    }

    public void setTransferSizeHint(int transferSizeHint) {
        this.transferSizeHint = transferSizeHint;
    }

    private void process() {
        if (!isReady) {
            if (transferSizeHint > 0) {
                optimizeTransferSize();
            }
            removeProhibitedBlocks();
            isReady = true;
        }
    }

    private void insertBlock(Block block) throws KuraException {
        if (isReady) {
            throw new IllegalStateException("Aggregation has already been performed, cannot add blocks");
        }
        ListIterator<Block> i = blocks.listIterator(blocks.size());
        while (i.hasPrevious()) {
            Block b = i.previous();
            if (b.start <= block.start) {
                i.next();
                break;
            }
        }
        i.add(block);
        i.previous();
        if (i.hasPrevious()) {
            i.previous();
            processBlocks(i);
            i.previous();
        }
        processBlocks(i);
    }

    private void processBlocks(ListIterator<Block> i) throws KuraException {
        while (i.nextIndex() < blocks.size() - 1 && processBlock(i))
            ;
    }

    private boolean processBlock(ListIterator<Block> i) throws KuraException {
        Block first = i.next();
        Block second = i.next();
        if (first.end < second.start) {
            return false;
        }
        boolean isTypeDifferent = first.isProhibited ^ second.isProhibited;

        if (first.end > second.start && isTypeDifferent) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "Conflicting blocks: " + first + " " + second);
        }

        if (isTypeDifferent) {
            return false;
        }

        i.remove();
        first.end = Math.max(first.end, second.end);
        return true;
    }

    private void removeProhibitedBlocks() {
        if (!hasProhibitedBlocks) {
            return;
        }

        Iterator<Block> i = blocks.iterator();
        while (i.hasNext()) {
            Block b = i.next();
            if (b.isProhibited) {
                i.remove();
            }
        }
    }

    private Block skipToNonProhibitedBlock(ListIterator<Block> i, Block current) {
        while (current.isProhibited && i.hasNext()) {
            current = i.next();
        }
        return current;
    }

    private void optimizeTransferSize() {
        if (blocks.size() <= 1) {
            return;
        }
        ListIterator<Block> i = blocks.listIterator();
        Block current = skipToNonProhibitedBlock(i, i.next());
        while (i.hasNext()) {
            Block next = i.next();
            if (!next.isProhibited && next.end - current.start <= transferSizeHint) {
                current.end = next.end;
                i.remove();
            } else {
                current = skipToNonProhibitedBlock(i, next);
            }
        }
    }

    public static class Block {

        private int start;
        private int end;
        private boolean isProhibited;

        public Block(int start, int end, boolean isProhibited) {
            this.start = Math.min(start, end);
            this.end = Math.max(start, end);
            this.isProhibited = isProhibited;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public boolean isProhibited() {
            return isProhibited;
        }

        public boolean contains(int addr) {
            return start <= addr && addr < end;
        }

        @Override
        public String toString() {
            return "[" + start + ", " + end + ", " + (isProhibited ? "P" : "R") + "]";
        }
    }
}
