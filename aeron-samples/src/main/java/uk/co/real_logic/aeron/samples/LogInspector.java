/*
 * Copyright 2015 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.aeron.samples;

import uk.co.real_logic.aeron.LogBuffers;
import uk.co.real_logic.aeron.common.concurrent.logbuffer.FrameDescriptor;
import uk.co.real_logic.aeron.common.protocol.DataHeaderFlyweight;
import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.io.PrintStream;
import java.util.Date;

import static uk.co.real_logic.aeron.common.concurrent.logbuffer.LogBufferDescriptor.*;
import static uk.co.real_logic.aeron.common.protocol.DataHeaderFlyweight.HEADER_LENGTH;

/**
 * Command line utility for inspecting a log buffer to see what terms and messages it contains.
 */
public class LogInspector
{
    public static void main(final String[] args) throws Exception
    {
        final PrintStream out = System.out;
        if (args.length < 1)
        {
            out.println("Usage: LogInspector <logFileName> [message dump limit]");
            return;
        }

        final String logFileName = args[0];
        final int messageDumpLimit = args.length >= 2 ? Integer.parseInt(args[1]) : Integer.MAX_VALUE;

        try (final LogBuffers logBuffers = new LogBuffers(logFileName))
        {
            out.println("======================================================================");
            out.format("%s Inspection dump for %s\n", new Date(), logFileName);
            out.println("======================================================================");

            final UnsafeBuffer[] atomicBuffers = logBuffers.atomicBuffers();
            final DataHeaderFlyweight dataHeaderFlyweight = new DataHeaderFlyweight();
            final int termLength = atomicBuffers[0].capacity();

            final UnsafeBuffer logMetaDataBuffer = atomicBuffers[PARTITION_COUNT * 2];

            out.format("Initial term id: %d\n", initialTermId(logMetaDataBuffer));
            out.format(" Active term id: %d\n", activeTermId(logMetaDataBuffer));
            out.format("   Active Index: %d\n", indexByTerm(initialTermId(logMetaDataBuffer), activeTermId(logMetaDataBuffer)));
            out.format("    Term Length: %d\n\n", termLength);

            if (!Boolean.getBoolean("loginspector.skipHeaders"))
            {
                final UnsafeBuffer[] defaultFrameHeaders = defaultFrameHeaders(logMetaDataBuffer);
                for (int i = 0; i < defaultFrameHeaders.length; i++)
                {
                    dataHeaderFlyweight.wrap(defaultFrameHeaders[i]);
                    out.format("Index %d Default %s\n", i, dataHeaderFlyweight);
                }
            }

            out.println();

            for (int i = 0; i < PARTITION_COUNT; i++)
            {
                final UnsafeBuffer metaDataBuffer = atomicBuffers[i + PARTITION_COUNT];
                out.format("Index %d Term Meta Data status=%s tail=%d\n",
                    i,
                    termStatus(metaDataBuffer),
                    metaDataBuffer.getInt(TERM_TAIL_COUNTER_OFFSET));
            }

            for (int i = 0; i < PARTITION_COUNT; i++)
            {
                out.println("\n======================================================================");
                out.format("Index %d Term Data\n\n", i);

                final UnsafeBuffer termBuffer = logBuffers.atomicBuffers()[i];
                dataHeaderFlyweight.wrap(termBuffer);

                int offset = 0;
                do
                {
                    dataHeaderFlyweight.offset(offset);
                    out.println(dataHeaderFlyweight.toString());

                    final int frameLength = dataHeaderFlyweight.frameLength();
                    if (frameLength == 0)
                    {
                        final int limit = Math.min(termLength - (offset + HEADER_LENGTH), messageDumpLimit);
                        out.println(bytesToHex(termBuffer, offset + HEADER_LENGTH, limit));

                        break;
                    }

                    final int limit = Math.min(frameLength - HEADER_LENGTH, messageDumpLimit);
                    out.println(bytesToHex(termBuffer, offset + HEADER_LENGTH, limit));

                    offset += BitUtil.align(frameLength, FrameDescriptor.FRAME_ALIGNMENT);
                }
                while (offset < termLength);
            }
        }
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static char[] bytesToHex(final DirectBuffer buffer, final int offset, final int length)
    {
        final char[] hexChars = new char[length * 2];

        for (int i = 0; i < length; i++)
        {
            final int b = buffer.getByte(offset + i) & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[b >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[b & 0x0F];
        }

        return hexChars;
    }

    private static String termStatus(final UnsafeBuffer metaDataBuffer)
    {
        final int status = metaDataBuffer.getInt(TERM_STATUS_OFFSET);
        switch (status)
        {
            case CLEAN:
                return "CLEAN";

            case NEEDS_CLEANING:
                return "NEEDS_CLEANING";

            default:
                return status + " <UNKNOWN>";
        }
    }
}
