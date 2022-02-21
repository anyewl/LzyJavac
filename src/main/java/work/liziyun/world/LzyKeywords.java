package work.liziyun.world;


import work.liziyun.util.LzyContext;

public class LzyKeywords {
    public static final LzyContext.Key key = new LzyContext.Key();
    private final LzyTable lzyTable;
    // 下标原则: 通过Name在bytes中index
    private final LzyToken[] tokens;
    // 所有的Token建立Name，最大的Name在bytes中index
    private int maxKey = 0;
    // 敏感词的Name,下标原则: 枚举的下标    注意: 枚举中元素name不为空，才建立！
    private LzyName[] tokenName = new LzyName[LzyToken.values().length];

    public static LzyKeywords instance(LzyContext LzyContext){
        LzyKeywords lzyKeywords = (LzyKeywords)LzyContext.get(key);
        if (lzyKeywords == null){
            lzyKeywords = new LzyKeywords(LzyContext);
        }
        return lzyKeywords;
    }
    public LzyKeywords(LzyContext LzyContext){
        LzyContext.put(key,this);
        this.lzyTable = LzyTable.instance(LzyContext);
        // 所有的敏感词: Token转Name ---> 存储到tokenName
        for (LzyToken t : LzyToken.values()) {
            if (t.name != null){
                this.enterKeyword(t.name,t);
            }else{
                this.tokenName[t.ordinal()] = null;
            }
        }
        // 所有的Token: 标识符
        this.tokens = new LzyToken[this.maxKey+1];
        for (int i = 0; i < tokens.length; i++) {
            this.tokens[i] = LzyToken.IDENTIFIER;
        }
        // 所有的敏感词: Name转Token ---> 存储到tokens
        for (LzyToken t : LzyToken.values()) {
            if (t.name != null){
                // 获取每个Name的index
                int index = this.tokenName[t.ordinal()].getIndex();
                this.tokens[index] = t;
            }
        }

    }

    // 通过Name获取Token
    public LzyToken key(LzyName lzyName){
        // 标识符
        if ( lzyName.getIndex() > this.maxKey ){
            return LzyToken.IDENTIFIER;
        }else{
            return this.tokens[lzyName.getIndex()];
        }
    }

    // 敏感词: Token转Name，全部存储成Name
    private void enterKeyword(String str,LzyToken lzyToken){
        // 创建Name
        LzyName lzyName = this.lzyTable.fromString(str);
        this.tokenName[lzyToken.ordinal()] = lzyName;
        if (lzyName.getIndex() > this.maxKey){
            this.maxKey = lzyName.getIndex();
        }
    }

}
