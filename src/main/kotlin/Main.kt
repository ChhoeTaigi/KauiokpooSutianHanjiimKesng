package com.taccotap.kautianhanjiimkesng

import com.taccotap.kautianhanjiimkesng.dict.Hanjiim
import com.taccotap.kautianhanjiimkesng.dict.KauiokpooTaigiDictOutEntry
import com.taccotap.kautianhanjiimkesng.dict.KauiokpooTaigiDictSrcEntry
import com.taccotap.kautianhanjiimkesng.util.SplitStringByCodePoint
import org.apache.commons.csv.CSVFormat
import util.CsvIO
import util.XlsxIO
import java.lang.StringBuilder

private const val SRC_FILENAME = "kautian_main_20190520.xls"
private const val SAVE_FILENAME_PATH = "./output/KautianHanjiimKesng.csv"

fun main(args: Array<String>) {
    val dict = loadDict()
    val processedDictArray = processDict(dict)
    saveDict(processedDictArray)
}

// return Hashmap<wordId, SrcEntry>
fun loadDict(): HashMap<String, KauiokpooTaigiDictSrcEntry> {
    val dict = HashMap<String, KauiokpooTaigiDictSrcEntry>()
    loadWordDict(dict)
    return dict
}

private fun loadWordDict(dict: HashMap<String, KauiokpooTaigiDictSrcEntry>) {
    val resource = Thread.currentThread().contextClassLoader.getResource(SRC_FILENAME)
    println("path: " + resource.path)

    val readXlsDictArrayList = XlsxIO.read(resource.path, "holo", true)

    for (recordColumnArrayList in readXlsDictArrayList) {
        val dictSrcEntry = KauiokpooTaigiDictSrcEntry()

        dictSrcEntry.id = recordColumnArrayList[0]
        dictSrcEntry.wordProperty = recordColumnArrayList[1]
        dictSrcEntry.hanjiTaibun = recordColumnArrayList[2]
        dictSrcEntry.kiplmj = recordColumnArrayList[3]

        // init first
        dictSrcEntry.wordBunPehProperty = ""
        dictSrcEntry.kiplmjDialect = ""
        dictSrcEntry.wordKithannKhiunnkhauProperty = ""
        dictSrcEntry.hoabun = ""
        dictSrcEntry.descriptions = ""

        dict[dictSrcEntry.id] = dictSrcEntry
    }
}

private fun processDict(dict: HashMap<String, KauiokpooTaigiDictSrcEntry>): ArrayList<Hanjiim> {
    var hanjiimMap = HashMap<String, MutableSet<String>>()   // HashMap<Hanji,HanjiimArray<Lomaji>>
    var hanjiimArrayList = ArrayList<Hanjiim>()

    for (srcEntry: KauiokpooTaigiDictSrcEntry in dict.values) {
        if (srcEntry.wordProperty.toInt() == 12 || srcEntry.wordProperty.toInt() == 25) {
            continue
        }
        if (srcEntry.kiplmj.isEmpty()) {
//            println("word no lomaji: hanji=${srcEntry.hanjiTaibun}")
            continue
        }

        val hanjiString = srcEntry.hanjiTaibun.trim()
        val splitHanjiStringArray: ArrayList<String> = SplitStringByCodePoint.split(hanjiString)

        val fixLmj = if (srcEntry.kiplmj.startsWith("--")) {
            srcEntry.kiplmj.substring(2)
        } else {
            srcEntry.kiplmj
        }
        val splitLomajisList = fixLmj.trim().split("(/|、)".toRegex())

        for (lmj in splitLomajisList) {
            val splitLomajiList = lmj.trim().split("(--|-|‑| )".toRegex())
            val splitLomajiStringArray: ArrayList<String> = ArrayList(splitLomajiList)

            // check word count
            if (splitHanjiStringArray.size != splitLomajiStringArray.size) {
                println("word count not match: hanji=${srcEntry.hanjiTaibun}(${splitHanjiStringArray.size}), lmj=${srcEntry.kiplmj}(${splitLomajiStringArray.size})")
                continue
            }

            val size = splitHanjiStringArray.size
            for (i in 0 until size) {
                val hanji: String = splitHanjiStringArray[i]
                val lomaji: String = splitLomajiStringArray[i].toLowerCase().replace(".", "")

                if (lomaji.isEmpty()) {
                    continue
                }

                var hanjiimSet: MutableSet<String>? = hanjiimMap[hanji]
                if (hanjiimSet == null) {
                    var newHanjiimSet = mutableSetOf<String>()
                    newHanjiimSet.add(lomaji)
                    hanjiimMap.put(hanji, newHanjiimSet)
                } else {
                    if (!hanjiimSet.contains(lomaji)) {
                        hanjiimSet.add(lomaji)
                    }
                }
            }
        }
    }

    for (hanji in hanjiimMap.keys) {
        var hanjiimSet = hanjiimMap[hanji]
        var lomajiArrayList = hanjiimSet!!.toCollection(ArrayList())
        var stringBuilder = StringBuilder()

        var hanjiim = Hanjiim()
        hanjiim.hanji = hanji
        hanjiim.lomajiArrayList = lomajiArrayList

        for (i in 0 until hanjiim.lomajiArrayList.size) {
            stringBuilder.append(hanjiim.lomajiArrayList[i])

            if (i < hanjiim.lomajiArrayList.size - 1) {
                stringBuilder.append(", ")
            }
        }
        hanjiim.lomajiArrayListString = stringBuilder.toString()

        if (lomajiArrayList.size > 1) {
            hanjiimArrayList.add(hanjiim)
        }

        println("hanji=${hanji}, lmj=${hanjiim.lomajiArrayListString}")
    }

    hanjiimArrayList.sortWith(compareByDescending { it.lomajiArrayList.size })

    println("教育部台語辭典：")
    println("漢字總共ê字數 = ${hanjiimMap.size}, 漢字有超過1-ê音ê字數 = ${hanjiimArrayList.size}")

    val percentage = hanjiimArrayList.size.toDouble() / hanjiimMap.size.toDouble() * 100.0
    println("漢字有超過1-ê音ê字比例 = $percentage %")

    return hanjiimArrayList
}

private fun saveDict(hanjiimArrayList: ArrayList<Hanjiim>) {
    val dict: ArrayList<ArrayList<String>> = ArrayList()
    for (outEntry: Hanjiim in hanjiimArrayList) {
        val entryArray: ArrayList<String> = ArrayList()

        outEntry.hanji.let { entryArray.add(it) }

        var stringBuilder = StringBuilder()
        for (i in 0 until outEntry.lomajiArrayList.size) {
            stringBuilder.append(outEntry.lomajiArrayList[i])

            if (i < outEntry.lomajiArrayList.size - 1) {
                stringBuilder.append(", ")
            }
        }
        stringBuilder.toString().let { entryArray.add(it) }

        outEntry.lomajiArrayList.size.toString().let { entryArray.add(it) }

        dict.add(entryArray)
    }

    val path = SAVE_FILENAME_PATH
    val csvFormat: CSVFormat = CSVFormat.DEFAULT.withHeader(
        "Hàn-jī",
        "Lô-má-jī",
        "Kúi ê im"
    )

    CsvIO.write(path, dict, csvFormat)
}
