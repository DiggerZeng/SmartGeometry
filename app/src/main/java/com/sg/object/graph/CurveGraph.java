package com.sg.object.graph;

import java.io.Serializable;
import java.util.List;

import com.sg.object.Point;
import com.sg.object.unit.CurveUnit;
import com.sg.object.unit.GUnit;

public class CurveGraph extends Graph implements Serializable {

    // 一般方程：aX^2 + bY^2 + cXY + dX + eY + f = 0

    //是否是三角形的内切圆
    boolean isTriangleConstraint;

    public CurveGraph() {
//		translationStratery = new CurveStrategy();  //选择线变换策略
        isTriangleConstraint = false;
    }

    public CurveGraph(List<Point> pList) {
        buildGraph(new CurveUnit(pList));
        isTriangleConstraint = false;
//		translationStratery = new CurveStrategy();  //选择线变换策略
    }

    public void bulidCurveGraph(List<GUnit> units) {
        for(GUnit unit : units) {
            buildGraph(unit);
        }
    }

    public boolean isTriangleConstraint() {
        return isTriangleConstraint;
    }

    public void setTriangleConstraint(boolean state) {
        isTriangleConstraint = state;
    }

    @Override
    public void move(float mx, float my) {
        // TODO Auto-generated method stub
    }

}
