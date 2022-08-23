package com.leebaeng.util

import com.leebaeng.util.LLog.getPrintMethod
import kotlin.collections.ArrayList

/**
 * LeeBaeng Log Module For Kotlin JVM Platform
 *
 * @author LeeBaeng
 * @version 1.0.0
 */
object LLog {
    enum class LogLevel(val level: Int, val prefix: String) {
        VERBOSE(0, "V"),
        DEBUG(1, "D"),
        INFO(2, "I"),
        WARN(3, "W"),
        ERROR(4, "E"),
        EXCEPT(5, "X"),
        NONE(6, "N"),
        SYSTEM(7, "S")
    }

    //    private const val WEB_LOG_TAG = "WebLog"
    private const val DEFAULT_HEADER = "LLog"

    /** 출력을 무시할 Tag를 설정 한다. (Runtime중 필요에 따라 설정) */
    private var ignoreTagList: ArrayList<String> = arrayListOf()
    var logLevel: LogLevel = LogLevel.VERBOSE // logLevel for Logcat console
    var logHeader: String = DEFAULT_HEADER
        private set

    fun init() {
        logHeader = "" // TODO : Project Name 획득 및 설정
    }

    fun setLogHeader(obj: Any?) {
        logHeader = getTagString(obj) ?: DEFAULT_HEADER
    }

    fun setLogHeader(header: String?) {
        logHeader = header ?: DEFAULT_HEADER
    }

    fun println(priority:LogLevel, tag: String, msg: String){
        val sb = StringBuffer(priority.prefix)
        sb.append(" $tag")
        sb.append(" : $msg")
        println(sb.toString())
    }

    /** Verbose 로그를 출력한다.(Log level : 0) */
    fun verbose(log: String, tag: Any? = null) = log(LogLevel.VERBOSE, log, tag)

    /** Debug 로그를 출력한다.(Log level : 1) */
    fun debug(log: String, tag: Any? = null) = log(LogLevel.DEBUG, log, tag)

    /** Info 로그를 출력한다.(Log level : 2) */
    fun info(log: String, tag: Any? = null) = log(LogLevel.INFO, log, tag)

    /** Warning 로그를 출력한다.(Log level : 3) */
    fun warn(log: String, tag: Any? = null) = log(LogLevel.WARN, log, tag)

    /** Error 로그를 출력한다.(Log level : 4) */
    fun err(log: String, tag: Any? = null) = log(LogLevel.ERROR, log, tag)

    /** Exception 로그를 출력한다.(Log level : 5) */
    fun except(e: Throwable, log: String? = null, tag: Any? = null) {
        if (logLevel <= LogLevel.EXCEPT) {
            val header = getHeader(tag)
            val priority = LogLevel.EXCEPT

            println(priority, header, "\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            println(priority, header, "${log ?: "thrown Exception at ${getCallerClassName()}"} → $e")
            for (st in e.stackTrace) println(priority, header, "     $st")
            if (e.cause != null) {
                val cuz = e.cause
                println(priority, header, "   cause: " + cuz.toString())
                for (st in cuz!!.stackTrace) println(priority, header, "     $st")
            }
            println(priority, header, "\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }

    /** System 로그를 출력한다.(Log level : 7) */
    fun sys(log: String, tag: Any? = null, printSpline: Boolean = true) {
        val header = getHeader(tag)
        val priority = LogLevel.SYSTEM

        if (printSpline) println(priority, header, "────────────────────────────────────────────────")
        println(priority, header, log)
        if (printSpline) println(priority, header, "────────────────────────────────────────────────")
    }

    private fun log(level: LogLevel, msg: String, tag: Any? = null) {
        // 출력 설정 값 보다 저수준 로그레벨 출력 무시
        if (logLevel > level) return

        // IgnoreTagList에 등록된 Tag 출력 무시
        val additionalTag = getAdditionalTag(tag)
        if (tag != null && ignoreTagList.contains(tag)) return

        println(level, getHeader(tag, additionalTag), msg)
    }

    /**
     * 로그헤더를 반환한다.
     * tag가 String type이 아닌 경우 해당 Object의 Class Name을 Tag로 사용한다.
     *
     * @param tag 헤더에 포함할 로그를 출력하는 개체
     * @param additionalTag (optional) additional Tag 입력된 경우 LLog.getAdditionalTag()을 수행하지 않고 커스텀한 AdditionalLog를 출력한다
     * @return 반환형식 : [logHeader|tag]
     */
    fun getHeader(tag: Any?, additionalTag: String? = null): String {
        val additionalTag = additionalTag ?: getAdditionalTag(tag)
        return if (additionalTag != null) "[$logHeader|${additionalTag}]"
        else "[$logHeader]"
    }

    /** LogHeader 뒤에 붙는 AdditionalTag를 반환한다. */
    fun getAdditionalTag(tag: Any?): String? {
        return if (tag != null) {
            getTagString(tag) ?: getCallerClassName()
        } else getCallerClassName()
    }

    /** 태그 String을 반환 한다 */
    fun getTagString(tag: Any?): String? {
        if (tag != null) {
            return if (tag is String || (!tag.toString().contains("@") && tag.toString().length < 15)) tag.toString()
            else tag.javaClass.simpleName
        }
        return null
    }

    /** 로그를 출력한 Class의 이름을 반환한다 */
    fun getCallerClassName(): String? {
        val stElements = Thread.currentThread().stackTrace
        val filterPackages = setOf(this::class.qualifiedName!!, Thread::class.qualifiedName!!)
        for (i in 1 until stElements.size) {
            val ste = stElements[i]
            if (ste.className !in filterPackages) {
                return Class.forName(ste.className).simpleName
            }
        }
        return null
    }

    fun getPrintMethod(printLevel: LogLevel): (String, Any?) -> Unit {
        return when (printLevel) {
            LogLevel.VERBOSE -> this::verbose
            LogLevel.DEBUG -> this::debug
            LogLevel.INFO -> this::info
            LogLevel.WARN -> this::warn
            LogLevel.ERROR, LogLevel.EXCEPT -> this::err
            LogLevel.SYSTEM -> this::sys
            else -> this::info
        }
    }

    /** Print를 무시할 Tag를 List에 추가한다. (출력 거부등록. Log 출력시 입력하는 Tag와 동일 객체 삽입) */
    fun addIgnoreTag(tag: Any) {
        val tag = getAdditionalTag(tag) ?: return
        ignoreTagList.add(tag)
    }

    /** Print를 무시할 Tag를 List에서 제거한다. (출력 승인으로 전환) */
    fun removeIgnoreTag(tag: Any) {
        val tag = getAdditionalTag(tag) ?: return
        if (ignoreTagList.contains(tag)) ignoreTagList.remove(tag)
    }

    /** Print를 무시할 Tag 목록을 초기화 한다. */
    fun clearIgnoreTag() {
        ignoreTagList.clear()
    }
}


/** Verbose 로그를 출력한다.(Log level : 0) */
fun Any.logV(tag: Any? = null) = getPrintMethod(LLog.LogLevel.VERBOSE).invoke(this.toString(), tag)

/** Debug 로그를 출력한다.(Log level : 1) */
fun Any.logD(tag: Any? = null) = getPrintMethod(LLog.LogLevel.DEBUG).invoke(this.toString(), tag)

/** Info 로그를 출력한다.(Log level : 2) */
fun Any.logI(tag: Any? = null) = getPrintMethod(LLog.LogLevel.INFO).invoke(this.toString(), tag)

/** Warning 로그를 출력한다.(Log level : 3) */
fun Any.logW(tag: Any? = null) = getPrintMethod(LLog.LogLevel.WARN).invoke(this.toString(), tag)

/** Error 로그를 출력한다.(Log level : 4) */
fun Any.logE(tag: Any? = null) = getPrintMethod(LLog.LogLevel.ERROR).invoke(this.toString(), tag)

/** Exception 로그를 출력한다.(Log level : 5) */
fun Exception.logEX(log: String? = null, tag: Any? = null) = LLog.except(this, log, tag)

/** System 로그를 출력한다.(Log level : 7) */
fun Any.logS(tag: Any? = null, printSpline: Boolean = true) = LLog.sys(this.toString(), tag, printSpline)

