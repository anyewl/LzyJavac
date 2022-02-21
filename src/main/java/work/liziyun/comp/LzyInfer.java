package work.liziyun.comp;

import work.liziyun.code.type.LzyType;
import work.liziyun.code.type.LzyTypeTags;
import work.liziyun.tag.LzyFlags;
import work.liziyun.tag.LzyKinds;

public class LzyInfer implements LzyKinds, LzyFlags, LzyTypeTags {
    public static final LzyType anyPoly = new LzyType(LzyTypeTags.NONE,null);
}
