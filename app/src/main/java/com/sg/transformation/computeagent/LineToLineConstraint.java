package com.sg.transformation.computeagent;

import java.util.List;

import com.sg.control.GraphControl;
import com.sg.logic.common.CommonFunc;
import com.sg.object.graph.Graph;
import com.sg.object.graph.LineGraph;
import com.sg.object.graph.RectangleGraph;
import com.sg.object.graph.TriangleGraph;
import com.sg.object.unit.GUnit;
import com.sg.object.unit.LineUnit;
import com.sg.object.unit.PointUnit;
import com.sg.property.common.ThresholdProperty;

public class LineToLineConstraint {

    public LineToLineConstraint() {

    }

    //直线图形curGraph与直线图形graph的约束
    public Graph lineToLineConstrain(Graph constraintGraph, Graph curGraph){
        double minDist = ThresholdProperty.TWO_POINT_IS_CONSTRAINTED;
        PointUnit constraintPointUnit = null;

        int consNum = constraintGraph.getGraph().size();
        PointUnit firstPointUnit = (PointUnit) constraintGraph.getGraph().get(0);    //约束图形的第一个点元
        PointUnit lastPointUnit = (PointUnit) constraintGraph.getGraph().get(consNum - 1);  //约束图形的最后一个点元

        //判断点是否是圆心
        boolean isFirstCenter = (firstPointUnit.isInLine() && !firstPointUnit.isCommonConstrainted());
        boolean isLastCenter = (lastPointUnit.isInLine() && !lastPointUnit.isCommonConstrainted());

        int curNum = curGraph.getGraph().size();
        PointUnit onePointUnit = (PointUnit) curGraph.getGraph().get(0);    //当前图形的第一个点元
        LineUnit oneLineUnit = (LineUnit) curGraph.getGraph().get(1);
        boolean isOneStart = (oneLineUnit.getStartPointUnit() == onePointUnit);
        PointUnit twoPointUnit = (PointUnit) curGraph.getGraph().get(curNum - 1);  //当前图形的最后一个点元
        LineUnit twoLineUnit = (LineUnit) curGraph.getGraph().get(curNum - 2);
        boolean isTwoStart = (twoLineUnit.getStartPointUnit() == twoPointUnit);

        PointUnit otherPointUnit;  //被约束图形（直线）的另一个点元

        if(CommonFunc.distance(firstPointUnit, onePointUnit) < minDist && !isFirstCenter) {
            constraintPointUnit = firstPointUnit;
            onePointUnit = firstPointUnit;
            if(isOneStart)
                oneLineUnit.setStartPointUnit(firstPointUnit);
            else
                oneLineUnit.setEndPointUnit(firstPointUnit);
            otherPointUnit = twoPointUnit;
        } else {
            if(CommonFunc.distance(firstPointUnit, twoPointUnit) < minDist && !isFirstCenter) {
                constraintPointUnit = firstPointUnit;
                twoPointUnit = firstPointUnit;
                if(isTwoStart)
                    twoLineUnit.setStartPointUnit(firstPointUnit);
                else
                    twoLineUnit.setEndPointUnit(firstPointUnit);
                otherPointUnit = onePointUnit;
            } else {
                if(CommonFunc.distance(lastPointUnit, onePointUnit) < minDist && !isLastCenter) {
                    constraintPointUnit = lastPointUnit;
                    onePointUnit = lastPointUnit;
                    if(isOneStart)
                        oneLineUnit.setStartPointUnit(lastPointUnit);
                    else
                        oneLineUnit.setEndPointUnit(lastPointUnit);
                    otherPointUnit = twoPointUnit;
                } else {
                    if(CommonFunc.distance(lastPointUnit, twoPointUnit) < minDist && !isLastCenter) {
                        constraintPointUnit = lastPointUnit;
                        twoPointUnit = lastPointUnit;
                        if(isTwoStart)
                            twoLineUnit.setStartPointUnit(lastPointUnit);
                        else
                            twoLineUnit.setEndPointUnit(lastPointUnit);
                        otherPointUnit = onePointUnit;
                    } else {
                        return null;           //没有约束
                    }
                }
            }
        }

        boolean isALine = ((consNum == 3) && (curNum == 3));    //如果约束图形是一条直线与一条直线直线，只需进行一个点的约束识别，如果识别两点会造成约束图形与被约束图形直线重合
        otherPointUnit.clearDegree();    //成为一个独立的点


        if(!isALine) {
            if(constraintPointUnit == firstPointUnit){          //如果约束点是第一个点
                if(CommonFunc.distance(lastPointUnit, otherPointUnit) < minDist && !constraintGraph.isGraphConstrainted()){         //有另一个约束点，则是闭合图形
                    if(otherPointUnit == onePointUnit) {
                        if(isOneStart)
                            oneLineUnit.setStartPointUnit(lastPointUnit);
                        else
                            oneLineUnit.setEndPointUnit(lastPointUnit);
                    }
                    else {
                        if(isTwoStart)
                            twoLineUnit.setStartPointUnit(lastPointUnit);
                        else
                            twoLineUnit.setEndPointUnit(lastPointUnit);
                    }
                    for(int i = 1; i < (curNum - 1); i++) {
                        constraintGraph.buildGraph(curGraph.getGraph().get(i));
                    }
                    constraintGraph.setIsClosed(true);
                }else{                                                                    //没有另一个约束点，则图元加在第一个点之前，保持点线点的顺序
                    if(constraintPointUnit == onePointUnit) {
                        for(int i = 1; i < curNum; i++) {
                            constraintGraph.buildGraph(0, curGraph.getGraph().get(i));
                        }
                    } else {
                        for(int i = curNum - 2; i >= 0; i--) {
                            constraintGraph.buildGraph(0, curGraph.getGraph().get(i));
                        }
                    }
                }
            }else{                                             //如果约束点是最后一个点
                if(CommonFunc.distance(firstPointUnit, otherPointUnit) < minDist && !constraintGraph.isGraphConstrainted()){
                    if(otherPointUnit == onePointUnit) {
                        if(isOneStart)
                            oneLineUnit.setStartPointUnit(firstPointUnit);
                        else
                            oneLineUnit.setEndPointUnit(firstPointUnit);
                    }
                    else {
                        if(isTwoStart)
                            twoLineUnit.setStartPointUnit(firstPointUnit);
                        else
                            twoLineUnit.setEndPointUnit(firstPointUnit);
                    }
                    for(int i = 1; i < (curNum - 1); i++) {
                        constraintGraph.buildGraph(curGraph.getGraph().get(i));
                    }
                    constraintGraph.setIsClosed(true);
                }else{                                                                    //没有另一个约束点，则图元加在最后一个点之后，保持点线点的顺序
                    if(constraintPointUnit == onePointUnit) {
                        for(int i = 1; i < curNum; i++) {
                            constraintGraph.buildGraph(curGraph.getGraph().get(i));
                        }
                    } else {
                        for(int i = curNum - 2; i >= 0; i--) {
                            constraintGraph.buildGraph(curGraph.getGraph().get(i));
                        }
                    }
                }
            }
        }else{
            //一条直线与一条直线直线
            if(constraintPointUnit == firstPointUnit){
                constraintGraph.buildGraph(0, new LineUnit(otherPointUnit, constraintPointUnit));
                constraintGraph.buildGraph(0, otherPointUnit);
            }else{
                constraintGraph.buildGraph(consNum, new LineUnit(constraintPointUnit, otherPointUnit));
                constraintGraph.buildGraph(consNum + 1, otherPointUnit);
            }
        }
        return constraintGraph;
    }

    //重新构建闭合图形
    public Graph rebuildLinearClose(GraphControl graphControl, Graph graph) {
        Graph temp = null;
        if(graph instanceof LineGraph && graph.isClosed()){
            List<GUnit> units = graph.getGraph();
            if(units.size() == 6){
                //重新构造成三角形
                graphControl.deleteGraph(graph);
                temp = new TriangleGraph(units);
                temp.setID(graph.getID());
                graphControl.addGraph(temp);
            }
            if(units.size() == 8){
                //重新构造四边形
                graphControl.deleteGraph(graph);
                temp = new RectangleGraph(units);
                temp.setID(graph.getID());
                graphControl.addGraph(temp);
            }
        }
        return temp;
    }
}
