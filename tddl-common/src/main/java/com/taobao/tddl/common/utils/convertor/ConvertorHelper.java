package com.taobao.tddl.common.utils.convertor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;

/**
 * convert转化helper类，注册一些默认的convertor
 * 
 * @author jianghang 2011-5-20 下午04:44:38
 */
public class ConvertorHelper {

    public static final String              ALIAS_DATE_TIME_TO_STRING     = StringAndDateConvertor.DateTimeToString.class.getSimpleName();
    public static final String              ALIAS_DATE_DAY_TO_STRING      = StringAndDateConvertor.DateDayToString.class.getSimpleName();
    public static final String              ALIAS_STRING_TO_DATE_TIME     = StringAndDateConvertor.StringToDateTime.class.getSimpleName();
    public static final String              ALIAS_STRING_TO_DATE_DAY      = StringAndDateConvertor.StringToDateDay.class.getSimpleName();
    public static final String              ALIAS_CALENDAR_TIME_TO_STRING = StringAndDateConvertor.CalendarTimeToString.class.getSimpleName();
    public static final String              ALIAS_CALENDAR_DAY_TO_STRING  = StringAndDateConvertor.CalendarDayToString.class.getSimpleName();
    public static final String              ALIAS_STRING_TO_CALENDAR_TIME = StringAndDateConvertor.StringToCalendarTime.class.getSimpleName();
    public static final String              ALIAS_STRING_TO_CALENDAR_DAY  = StringAndDateConvertor.StringToCalendarDay.class.getSimpleName();

    // common对象范围：8种Primitive和对应的Java类型，BigDecimal, BigInteger
    public static Map<Class, Object>        commonTypes                   = new HashMap<Class, Object>();
    public static final Convertor           stringToCommon                = new StringAndCommonConvertor.StringToCommon();
    public static final Convertor           commonToCommon                = new CommonAndCommonConvertor.CommonToCommon();
    // toString处理
    public static final Convertor           objectToString                = new StringAndObjectConvertor.ObjectToString();
    // 枚举处理
    public static final Convertor           stringToEnum                  = new StringAndEnumConvertor.StringToEnum();
    public static final Convertor           enumToString                  = new StringAndEnumConvertor.EnumToString();
    public static final Convertor           sqlToDate                     = new SqlDateAndDateConvertor.SqlDateToDateConvertor();
    public static final Convertor           dateToSql                     = new SqlDateAndDateConvertor.DateToSqlDateConvertor();
    public static final Convertor           blobToBytes                   = new BlobAndBytesConvertor.BlobToBytes();
    public static final Convertor           stringToBytes                 = new StringAndObjectConvertor.StringToBytes();

    private static volatile ConvertorHelper singleton                     = null;

    private ConvertorRepository             repository                    = null;

    public ConvertorHelper(){
        repository = new ConvertorRepository();
        initDefaultRegister();
    }

    public ConvertorHelper(ConvertorRepository repository){
        // 允许传入自定义仓库
        this.repository = repository;
        initDefaultRegister();
    }

    /**
     * 单例方法
     */
    public static ConvertorHelper getInstance() {
        if (singleton == null) {
            synchronized (ConvertorHelper.class) {
                if (singleton == null) { // double check
                    singleton = new ConvertorHelper();
                }
            }
        }
        return singleton;
    }

    /**
     * 根据class获取对应的convertor
     * 
     * @return
     */
    public Convertor getConvertor(Class src, Class dest) {
        if (src == dest) {
            // 对相同类型的直接忽略，不做转换
            return null;
        }

        // 按照src->dest来取映射
        Convertor convertor = repository.getConvertor(src, dest);

        // 如果dest是string，获取一下object->string.
        // (系统默认注册了一个Object.class -> String.class的转化)
        if (convertor == null && dest == String.class) {
            if (src.isEnum()) {// 如果是枚举
                convertor = enumToString;
            } else { // 默认进行toString输出
                convertor = objectToString;
            }
        }

        // 如果是其中一个是String类
        if (convertor == null && src == String.class) {
            if (commonTypes.containsKey(dest)) { // 另一个是Common类型
                convertor = stringToCommon;
            } else if (dest.isEnum()) { // 另一个是枚举对象
                convertor = stringToEnum;
            }
        }

        // 如果src/dest都是Common类型，进行特殊处理
        if (convertor == null && commonTypes.containsKey(src) && commonTypes.containsKey(dest)) {
            convertor = commonToCommon;
        }

        return convertor;
    }

    /**
     * 根据alias获取对应的convertor
     * 
     * @return
     */
    public Convertor getConvertor(String alias) {
        return repository.getConvertor(alias);
    }

    /**
     * 注册class对应的convertor
     */
    public void registerConvertor(Class src, Class dest, Convertor convertor) {
        repository.registerConvertor(src, dest, convertor);
    }

    /**
     * 注册alias对应的convertor
     */
    public void registerConvertor(String alias, Convertor convertor) {
        repository.registerConvertor(alias, convertor);
    }

    // ======================= register处理 ======================

    public void initDefaultRegister() {
        initCommonTypes();
        StringDateRegister();
    }

    private void StringDateRegister() {
        // 注册string<->date对象处理
        Convertor stringToDateDay = new StringAndDateConvertor.StringToDateDay();
        Convertor stringToDateTime = new StringAndDateConvertor.StringToDateTime();
        Convertor stringToCalendarDay = new StringAndDateConvertor.StringToCalendarDay();
        Convertor stringToCalendarTime = new StringAndDateConvertor.StringToCalendarTime();
        Convertor dateDayToString = new StringAndDateConvertor.DateDayToString();
        Convertor dateTimeToString = new StringAndDateConvertor.DateTimeToString();
        Convertor calendarDayToString = new StringAndDateConvertor.CalendarDayToString();
        Convertor calendarTimeToString = new StringAndDateConvertor.CalendarTimeToString();
        // 注册默认的String <-> Date的处理
        repository.registerConvertor(String.class, Date.class, stringToDateTime);
        repository.registerConvertor(Date.class, String.class, dateTimeToString);
        repository.registerConvertor(String.class, Calendar.class, stringToCalendarTime);
        repository.registerConvertor(Calendar.class, String.class, calendarTimeToString);
        // 注册默认的Date <-> SqlDate的处理
        repository.registerConvertor(java.sql.Date.class, Date.class, sqlToDate);
        repository.registerConvertor(java.sql.Time.class, Date.class, sqlToDate);
        repository.registerConvertor(java.sql.Timestamp.class, Date.class, sqlToDate);
        repository.registerConvertor(Date.class, java.sql.Date.class, dateToSql);
        repository.registerConvertor(Date.class, java.sql.Time.class, dateToSql);
        repository.registerConvertor(Date.class, java.sql.Timestamp.class, dateToSql);
        repository.registerConvertor(Blob.class, byte[].class, blobToBytes);
        repository.registerConvertor(String.class, byte[].class, stringToBytes);
        // 注册为别名
        repository.registerConvertor(ALIAS_STRING_TO_DATE_DAY, stringToDateDay);
        repository.registerConvertor(ALIAS_STRING_TO_DATE_TIME, stringToDateTime);
        repository.registerConvertor(ALIAS_STRING_TO_CALENDAR_DAY, stringToCalendarDay);
        repository.registerConvertor(ALIAS_STRING_TO_CALENDAR_TIME, stringToCalendarTime);
        repository.registerConvertor(ALIAS_DATE_DAY_TO_STRING, dateDayToString);
        repository.registerConvertor(ALIAS_DATE_TIME_TO_STRING, dateTimeToString);
        repository.registerConvertor(ALIAS_CALENDAR_DAY_TO_STRING, calendarDayToString);
        repository.registerConvertor(ALIAS_CALENDAR_TIME_TO_STRING, calendarTimeToString);
    }

    private void initCommonTypes() {
        commonTypes.put(int.class, ObjectUtils.NULL);
        commonTypes.put(Integer.class, ObjectUtils.NULL);
        commonTypes.put(short.class, ObjectUtils.NULL);
        commonTypes.put(Short.class, ObjectUtils.NULL);
        commonTypes.put(long.class, ObjectUtils.NULL);
        commonTypes.put(Long.class, ObjectUtils.NULL);
        commonTypes.put(boolean.class, ObjectUtils.NULL);
        commonTypes.put(Boolean.class, ObjectUtils.NULL);
        commonTypes.put(byte.class, ObjectUtils.NULL);
        commonTypes.put(Byte.class, ObjectUtils.NULL);
        commonTypes.put(char.class, ObjectUtils.NULL);
        commonTypes.put(Character.class, ObjectUtils.NULL);
        commonTypes.put(float.class, ObjectUtils.NULL);
        commonTypes.put(Float.class, ObjectUtils.NULL);
        commonTypes.put(double.class, ObjectUtils.NULL);
        commonTypes.put(Double.class, ObjectUtils.NULL);
        commonTypes.put(BigDecimal.class, ObjectUtils.NULL);
        commonTypes.put(BigInteger.class, ObjectUtils.NULL);
    }

    // ========================= setter / getter ===================

    public void setRepository(ConvertorRepository repository) {
        this.repository = repository;
    }
}