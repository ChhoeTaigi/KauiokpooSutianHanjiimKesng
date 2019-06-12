package com.taccotap.kautianhanjiimkesng.dict

import dict.DictEntry

open class KauiokpooTaigiDictSrcEntry : DictEntry() {
    lateinit var id: String
    lateinit var kiplmj: String
    lateinit var kiplmjDialect: String
    lateinit var hanjiTaibun: String
    lateinit var wordProperty: String
    lateinit var wordBunPehProperty: String
    lateinit var wordKithannKhiunnkhauProperty: String
    lateinit var hoabun: String
    lateinit var descriptions: String
}