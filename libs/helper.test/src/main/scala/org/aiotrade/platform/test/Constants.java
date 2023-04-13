package org.aiotrade.platform.test;

/**
 *
 * @author lvheshan
 */
public interface Constants {

    /** look feel , value is fixed apc, gray, white or citylights*/
    public static final String K_LOOKFEEL = "lookfeel";
    public static final String V_LOOKFEEL_MODERN = "Modern";
    public static final String V_LOOKFEEL_GRAY = "Gray";
    public static final String V_LOOKFEEL_WHITE = "White";
    public static final String V_LOOKFEEL_CITYLIGHTS = "CityLights";
    /** WATERMARK , value is not fixed*/
    public static final String K_WATER_MARK_TXT = "watermark";
    public static final String K_WATER_MARK_FONTNAME = "m_fontname";
    public static final String K_WATER_MARK_FONTSIZE = "m_fontsize";
    public static final String K_WATER_MARK_COLOR = "m_color";
    public static final String K_WATER_MARK_X = "m_x";
    public static final String K_WATER_MARK_Y = "m_y";
    /** WIDTH:图片的宽，像素为单位 */
    public static final String K_IMAGE_WIDTH = "WIDTH";
    /** HIGH:图片的高，像素为单位 */
    public static final String K_IMAGE_HEIGHT = "HEIGHT";
    /**
     * CYCLE：数据的周期
     * 1:1分钟
     * 5:5分钟
     * 10:10分钟
     * 30:30分钟
     * 60:60分钟
     */
    public static final String K_CYCLE = "CYCLE";
    /** value is "1" */
    public static final String V_CYCLE_1M = "1m";
    /** value is "5" */
    public static final String V_CYCLE_5M = "5m";
    /** value is "10" */
    public static final String V_CYCLE_10M = "10m";
    /** value is "30" */
    public static final String V_CYCLE_30M = "30m";
    /** value is "60" */
    public static final String V_CYCLE_60M = "60m";
    /** 1 day */
    public static final String V_CYCLE_1D = "1d";
    /** 5 day */
    public static final String V_CYCLE_5D = "5d";
    /** 10 day */
    //public static final String V_CYCLE_10D  = "10d";
    /** 31 day */
    public static final String V_CYCLE_1MONTH = "1month";
    /**
     * LINE_TYPE:默认k线
     * 1:k线
     * 2:OHLC，美国线
     * 3:线图
     */
    public static final String K_LINE_TYPE = "LINE_TYPE";
    /** value is "1" */
    public static final String V_LINE_TYPE_K = "1";
    /** value is "2" */
    public static final String V_LINE_OHLC = "2";
    /** value is "3" */
    public static final String V_LINE_LINE = "3";
    /**
     * DIS_YES_CLOSE：是否显示收盘价的线，默认不显示
     * 1:显示，
     * 2:不显示
     */
    public static final String K_DIS_YESTERDAY_CLOSE = "DIS_YES_CLOSE";
    /** value is "1" */
    public static final String V_DIS_YESTERDAY_CLOSE_TRUE = "1";
    /** value is "2" */
    public static final String V_DIS_YESTERDAY_CLOSE_FALSE = "2";
    /** ---7---
     * MAIN_INDICATOR_1:主图上显示的第一个指标,如果不传则不显示
     * 0或不传：不显示
     * 1:历史成交量分布(HVD)
     * 2:指数平均数平均数指标(EMA)
     * 3:保力加通道指标(BB)    -----is it BOLL
     * 4:移动平均线(MA)
     * 5:之字形指标(Z)
     * 6:抛物线状止损和反转指标(SAR)
     */
    public static final String K_MAIN_INDICATOR_1 = "MAIN_INDICATOR_1";
    /** value is "0" */
    public static final String V_MAIN_INDICATOR_INVISIBLE = "0";
    /**历史成交量分布 value is "HVD" */
    public static final String V_MAIN_INDICATOR_HVD = "HVD";
    /**指数平均数平均数指标 value is "EMA" */
    public static final String V_MAIN_INDICATOR_EMA = "EMA";
    /**保力加通道指标 value is not fixed "BOLL?" */
    public static final String V_MAIN_INDICATOR_BB = "BOLL";
    /**移动平均线 value is "MA" */
    public static final String V_MAIN_INDICATOR_MA = "MA";
    /**之字形指标 value is "ZIGZAG" */
    public static final String V_MAIN_INDICATOR_Z = "ZIGZAG";
    /**抛物线状止损和反转指标 value is "SAR" */
    public static final String V_MAIN_INDICATOR_SAR = "SAR";
    /**
     * MAIN_INDICATOR_2:主图上显示的第二个指标,如果不传则不显示,VALUE 同上
     */
    public static final String K_MAIN_INDICATOR_2 = "MAIN_INDICATOR_2";
    /* ---9---
     * FIRST_INDICATOR:第一个副图上显示的指标,如果不传则不显示
     * 0或不传：不显示，
     * 1:成交量
     * 2:能量超指标(HVD) //两个地方显示???
     * 3：随机震荡指标(KD)
     * 4:移动平均乖离率(BIAS)
     * 5:商品通道指数指标(CCI)
     * 6:方向性运动指标(ADX)-----????????????NA
     * 7:离散指标(AR/BR)
     * 8:资金流量指数指标(MF)  -------is it MFI ?
     * 9:移动平均汇总/分离指标(MACD)
     * 10:动量指标(MOT)      --------???????NA
     * 11:变动率指标(ROC)
     * 12:相对强弱指数指标(RSI)
     * 13:顾比复合移动平均线(GMMA)
     * 14:威廉指标(%R)
     */
    public static final String K_AUXILIARY_INDICATOR_1 = "FIRST_INDICATOR";
    /** value is "0"*/
    public static final String V_AUXILIARY_INDICATOR_INVISIBLE = "0";
    /**成交量 value is "VOL"*/
    public static final String V_AUXILIARY_INDICATOR_VOL = "VOL";
    /**能量超指标 value is "HVD"*/
    public static final String V_AUXILIARY_INDICATOR_HVD = "HVD";
    /**随机震荡指标 value is "KD"*/
    public static final String V_AUXILIARY_INDICATOR_KD = "KD";
    /**移动平均乖离率 value is BIAS*/
    public static final String V_AUXILIARY_INDICATOR_BIAS = "BIAS";
    /**商品通道指数指标 value is CCI*/
    public static final String V_AUXILIARY_INDICATOR_CCI = "CCI";
    /**方向性运动指标 value is ADX? still NA*/
    public static final String V_AUXILIARY_INDICATOR_ADX = "ADX??";
    /**离散指标 value is "ARBR"*/
    public static final String V_AUXILIARY_INDICATOR_ARBR = "ARBR";
    /**资金流量指数指标 vlue is "MF"*/
    public static final String V_AUXILIARY_INDICATOR_MF = "MF";
    /**移动平均汇总/分离指标 vlue is "MACD"*/
    public static final String V_AUXILIARY_INDICATOR_MACD = "MACD";
    /**动量指标 vlue is still NA "MOT??"*/
    public static final String V_AUXILIARY_INDICATOR_MOT = "MOT??";
    /**变动率指标 vlue is "ROC"*/
    public static final String V_AUXILIARY_INDICATOR_ROC = "ROC";
    /**相对强弱指数指标 vlue is "RSI"*/
    public static final String V_AUXILIARY_INDICATOR_RSI = "RSI";
    /**顾比复合移动平均线 vlue is "GMMA"*/
    public static final String V_AUXILIARY_INDICATOR_GMMA = "GMMA";
    /**威廉指标 still NA vlue is "R??"*/
    public static final String V_AUXILIARY_INDICATOR_R = "R??";
    /* ---10---
     * SECOND_INDICATOR:第二个副图上显示的指标,如果不传则不显示
     * 参数同FIRST_INDICATOR
     */
    public static final String K_AUXILIARY_INDICATOR_2 = "SECOND_INDICATOR";

    /* ---11---
     * THIRD_INDICATOR:第三个副图上显示的指标,如果不传则不显示
     * 参数同FIRST_INDICATOR
     **/
    public static final String K_AUXILIARY_INDICATOR_3 = "THIRD_INDICATOR";
}
