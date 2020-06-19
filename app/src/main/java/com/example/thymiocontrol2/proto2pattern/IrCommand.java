package com.example.thymiocontrol2.proto2pattern;

import static com.example.thymiocontrol2.proto2pattern.IrCommandBuilder.irCommandBuilder;
import static com.example.thymiocontrol2.proto2pattern.IrCommandBuilder.simpleSequence;


/**
 *  Quelle: Timnew's Android Infrared Library: https://github.com/timnew/AndroidInfrared
 */


public class IrCommand {

    public final int frequency;
    public final int[] pattern;

    public IrCommand(int frequency, int[] pattern) {
        this.frequency = frequency;
        this.pattern = pattern;
    }



    public static class RC5 {

        private static final int FREQUENCY = 36000; // T = 27.78 us
        private static final int T1 = 32;


        private static final IrCommandBuilder.SequenceDefinition SEQUENCE_DEFINITION = new IrCommandBuilder.SequenceDefinition() {
            @Override
            public void one(IrCommandBuilder builder, int index) {
                builder.reversePair(T1, T1);
            }

            @Override
            public void zero(IrCommandBuilder builder, int index) {
                builder.pair(T1, T1);
            }
        };

        // Note: first bit must be a one (start bit)
        public static IrCommand buildRC5(int bitCount, long data) {
            return irCommandBuilder(FREQUENCY)
                    .mark(T1)
                    .space(T1)
                    .mark(T1)
                    .sequence(SEQUENCE_DEFINITION, bitCount, data << (64 - bitCount))
                    .build();
        }
    }



}
