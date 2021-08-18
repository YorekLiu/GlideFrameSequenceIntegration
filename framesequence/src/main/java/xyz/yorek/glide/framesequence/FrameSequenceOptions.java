package xyz.yorek.glide.framesequence;

import com.bumptech.glide.load.Option;

/**
 * Created by yorek.liu on 2021/7/22
 *
 * @author yorek.liu
 */
public final class FrameSequenceOptions {

    public final static Option<Integer> LOOP_BEHAVIOR = Option.memory("xyz.yorek.glide.module.loop_behavior");
    public final static Option<Integer> LOOP_COUNT = Option.memory("xyz.yorek.glide.module.loop_count");

    /**
     * If set to {@code true}, downsample the gif decoding in {@link xyz.yorek.glide.decoder.StreamFrameSequenceDecoder}.
     * Defaults to {@code true}.
     */
    public static final Option<Boolean> ENABLE_SAMPLE = Option.memory(
            "xyz.yorek.glide.framesequence.FrameSequenceOptions", true);

    private FrameSequenceOptions() {
        // Utility class.
    }
}
