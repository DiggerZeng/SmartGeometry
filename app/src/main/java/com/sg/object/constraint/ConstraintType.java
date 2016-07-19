package com.sg.object.constraint;

/**
 * 约束类型
 * 外切圆：ExternallyTangentCircle；
 * 内切圆：InternallyTangentCircle；
 * 圆的弦，半径：HypotenuseOfCircle
 * 三角形外接圆： CircumCircleOfTriangle
 * 三角形内切圆： InternallyTangentCircleOfTriangle
 * 切线：TangentOfCircle
 * 点在直线上: PointOnLine
 * 点在曲线上: PointOnCurve
 * @author cai
 *
 */
public enum ConstraintType {

    NONE,
    ExternallyTangentCircle,
    InternallyTangentCircle,
    HypotenuseOfCircle,
    CircumCircleOfTriangle,
    InternallyTangentCircleOfTriangle,
    TangentOfCircle,
    PointOnLine,
    PointOnCurve;
}
