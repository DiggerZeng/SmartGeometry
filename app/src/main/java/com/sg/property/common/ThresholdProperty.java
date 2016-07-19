/*
 * 读取properties文件初始化常用阈值
 * 如果有常变量要添加：1，在threshold.properties文件中添加相应格式的信息，2，在该类中定义变量然后读取文件初始化
 * */

package com.sg.property.common;

import android.R.color;
import android.graphics.Color;

public class ThresholdProperty {

    //点阈值
    public static int POINT_COUNT;
    public static float POINT_DISTANCE;
    public static double POINT_SELECTED_DISTANCE;

    //线阈值
    public static double JUDGE_LINE_VALUE;

    //时间
    public static int PRESS_TIME_SHORT;
    public static int PRESS_TIME_LONG;

    //图形选中
    public static double GRAPH_CHECKED_DISTANCE;

    //三角形四边形
    public static double TWO_POINT_IS_CLOSED; //用于判断是否能成为三角形或四边形的阀值
    public static double TWO_POINT_IS_CONSTRAINTED;

    //画比大小
    public static float DRAW_WIDTH;

    //橡皮擦大小
    public static float ERASER_WIDTH;

    //放大镜的半径
    public static float MAGNIFIER_RADUIS;

    //三角形约束性识别使用
    public static double LINE_DISTANCE;

    //用户意图推测PopupWindow大小
    public static int TRI_WIDTH;
    public static int REC_WIDTH;
    public static int POP_HEIGTH;

    //RadioButton layout_width
    public static int BUTTON_WIDTH;

    public static void set(float density, int width) {
        POINT_COUNT = 20;
        POINT_DISTANCE = 8 * density;
        POINT_SELECTED_DISTANCE = 20 * density;
        JUDGE_LINE_VALUE = 0.9;
        PRESS_TIME_SHORT = 200;
        PRESS_TIME_LONG = 1000;
        GRAPH_CHECKED_DISTANCE = 30 * density;
        TWO_POINT_IS_CLOSED = 50 * density;
        TWO_POINT_IS_CONSTRAINTED = 30 * density;
        DRAW_WIDTH = 3 * density;
        ERASER_WIDTH = 15 * density;
        MAGNIFIER_RADUIS = 80 * density;
        LINE_DISTANCE = 15 * density;
        TRI_WIDTH = (int) (200 * density);
        REC_WIDTH = (int) (350 * density);
        POP_HEIGTH = (int) (70 * density);
//		BUTTON_WIDTH = (int) (96 * wDensity);
        BUTTON_WIDTH = width / 7;
    }
}