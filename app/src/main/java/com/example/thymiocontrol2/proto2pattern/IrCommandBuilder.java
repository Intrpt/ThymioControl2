package com.example.thymiocontrol2.proto2pattern;
import java.util.ArrayList;
import java.util.List;

/**
 *  Quelle: Timnew's Android Infrared Library: https://github.com/timnew/AndroidInfrared
 */


public class IrCommandBuilder {

    public static final long TOP_BIT_64 = 0x1L << 63;

    private final int frequency;
    private final List<Integer> buffer;
    private Boolean lastMark;

    public static IrCommandBuilder irCommandBuilder(int frequency) {
        return new IrCommandBuilder(frequency);
    }

    private IrCommandBuilder(int frequencyKHz) {
        this.frequency = frequencyKHz;

        buffer = new ArrayList<Integer>();

        lastMark = null;
    }

    private IrCommandBuilder appendSymbol(boolean mark, int interval) {
        if (lastMark == null || lastMark != mark) {
            buffer.add(interval);
            lastMark = mark;
        } else {
            int lastIndex = buffer.size() - 1;
            buffer.set(lastIndex, buffer.get(lastIndex) + interval);
        }

        return this;
    }

    public IrCommandBuilder mark(int interval) {
        return appendSymbol(true, interval);
    }

    public IrCommandBuilder space(int interval) {
        return appendSymbol(false, interval);
    }

    public IrCommandBuilder pair(int on, int off) {
        return mark(on).space(off);
    }

    public IrCommandBuilder reversePair(int off, int on) {
        return space(off).mark(on);
    }


    public IrCommandBuilder sequence(SequenceDefinition definition, int length, long data) {
        return sequence(definition, TOP_BIT_64, length, data);
    }

    public IrCommandBuilder sequence(SequenceDefinition definition, long topBit, int length, long data) {
        for (int index = 0; index < length; index++) {
            if ((data & topBit) != 0) {
                definition.one(this, index);
            } else {
                definition.zero(this, index);
            }

            data <<= 1;
        }

        return this;
    }

    public IrCommand build() {
        return new IrCommand(getFrequency(), buildSequence());
    }

    public int[] buildSequence() {
        return buildRawSequence(buffer);
    }

    public int getFrequency() {
        return frequency;
    }



    public static SequenceDefinition simpleSequence(final int oneMark, final int oneSpace, final int zeroMark, final int zeroSpace) {
        return new SequenceDefinition() {
            @Override
            public void one(IrCommandBuilder builder, int index) {
                builder.pair(oneMark, oneSpace);
            }

            @Override
            public void zero(IrCommandBuilder builder, int index) {
                builder.pair(zeroMark, zeroSpace);
            }
        };
    }


    public static int[] buildRawSequence(List<Integer> buffer) {
        int[] result = new int[buffer.size()];

        for (int i = 0; i < buffer.size(); i++) {
            result[i] = buffer.get(i);
        }

        return result;
    }


    public static abstract interface SequenceDefinition {

        public abstract void one(IrCommandBuilder builder, int index);

        public abstract void zero(IrCommandBuilder builder, int index);

    }
}

