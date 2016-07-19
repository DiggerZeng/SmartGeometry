package com.sg.transformation.computeagent;

import java.util.List;

import android.util.Log;

import com.sg.control.GraphControl;
import com.sg.logic.common.CommonFunc;
import com.sg.logic.common.CurveType;
import com.sg.object.Point;
import com.sg.object.constraint.ConstraintStruct;
import com.sg.object.constraint.ConstraintType;
import com.sg.object.graph.Graph;
import com.sg.object.graph.CurveGraph;
import com.sg.object.graph.TriangleGraph;
import com.sg.object.unit.CurveUnit;
import com.sg.object.unit.GUnit;
import com.sg.object.unit.LineUnit;
import com.sg.object.unit.PointUnit;
import com.sg.property.common.ThresholdProperty;

public class CurveConstraint {

    public CurveConstraint() {

    }

    //一个曲线与曲线图形的约束识别
    public Graph curveToCurveConstrain(Graph constraintGraph, Graph curGraph) {

        //只有一个曲线元的曲线图形
        CurveUnit curUnit = (CurveUnit) curGraph.getGraph().get(0);
        ConstraintType constraintType;
        List<GUnit> units = constraintGraph.getGraph();
        int size = units.size();
        for(int index = 0; index < size; index++) {
            GUnit unit = units.get(index);
            if(unit instanceof CurveUnit) {

                constraintType = isExternallyOrInternallyTangentCircle((CurveUnit) unit, curUnit);
                if(constraintType != ConstraintType.NONE) {
//					constraintGraph.buildGraph(curUnit);
                    constraintGraph.setGraphConstrainted(true);
                    curGraph.setGraphConstrainted(true);
//					((CurveUnit) unit).addConstraintStruct(new ConstraintStruct(constraintType, size));
//					curUnit.addConstraintStruct(new ConstraintStruct(constraintType, index));
                    constraintGraph.addConstraintStruct(new ConstraintStruct(constraintType, curGraph.getID()));
                    curGraph.addConstraintStruct(new ConstraintStruct(constraintType, constraintGraph.getID()));
                    return constraintGraph;
                }
            }
        }
        return null;
    }

    //判断内外切圆
    public ConstraintType isExternallyOrInternallyTangentCircle(CurveUnit unit, CurveUnit curUnit) {

        //如果两个都是圆
        if(curUnit.getCurveType() == CurveType.Circle && unit.getCurveType() == CurveType.Circle) {
            double radius1 = unit.getRadius();
            PointUnit center1 = unit.getCenter();
            double radius2= curUnit.getRadius();
            PointUnit center2 = curUnit.getCenter();
            double totalRadius = radius1 + radius2;   //两半径和
            double twoCenterDistance = CommonFunc.distance(center1, center2);  //两圆心距离
            double differenceOfR = Math.abs(radius1 - radius2);  //两半径差
            double minDistance = 25;

            //两圆心的距离  ~= 两圆半径之和 并且 切点在两曲线上
            if(Math.abs(twoCenterDistance - totalRadius) < minDistance) {
                //求切点
                double scale = radius1 / totalRadius;
                double x = center1.getX() + (center2.getX() - center1.getX()) * scale;
                double y = center1.getY() + (center2.getY() - center1.getY()) * scale;
                Point point = new Point((float)x, (float)y);

                if(curUnit.isInUnit(point) && unit.isInUnit(point)) {
                    //外切
                    scale = (twoCenterDistance - totalRadius) / twoCenterDistance;
                    float[][] transMatrix = {{1, 0, (float) ((center1.getX() - center2.getX()) * scale)},
                            {0, 1, (float) ((center1.getY() - center2.getY()) * scale)}, {0, 0, 1}};
                    curUnit.translate(transMatrix);
                    return ConstraintType.ExternallyTangentCircle;
                }
            }

            //两圆心的距离  ~= 两圆半径差的绝对值 && 两圆半径差的绝对值 > 0 (=0时同心等大圆)
            if(Math.abs(twoCenterDistance - differenceOfR) < minDistance && differenceOfR > 0) {
                Log.v("内切圆", "内切圆");
                //求切点
                PointUnit temp;  //半径大的圆的圆心
                if(radius1 - radius2 > 0) {
                    temp = center1;
                } else {
                    temp = center2;
                }
                double scale = Math.max(radius1, radius2) / (radius1 - radius2);
                double x = temp.getX() + (center2.getX() - center1.getX()) * scale;
                double y = temp.getY() + (center2.getY() - center1.getY()) * scale;
                Point point = new Point((float)x, (float)y);

                if(curUnit.isInUnit(point) && unit.isInUnit(point)) {
                    //内切
                    scale = (twoCenterDistance - differenceOfR) / twoCenterDistance;
                    float[][] transMatrix = {{1, 0, (float) ((center1.getX() - center2.getX()) * scale)},
                            {0, 1, (float) ((center1.getY() - center2.getY()) * scale)}, {0, 0, 1}};
                    curUnit.translate(transMatrix);
                    return ConstraintType.InternallyTangentCircle;
                }
            }
        }
        return ConstraintType.NONE;
    }

    //直线元与曲线元约束
    //相切
    //一点在曲线上，是直径，半径 独立出来，变动直线

    //三角形与曲线元，变动三角形后要保持三角形约束

    //四边形形与曲线元，变动四边形形后要保持三角形约束


    /**
     * 求圆切线的信息矩阵(斜率k，垂足坐标（x,y），平移向量（x1,y1）)
     */
    private float[] getMessageMatrix(PointUnit pointUnit1, PointUnit pointUnit2, Point center, double radius) {
        float k = ((float) (pointUnit2.getY() - pointUnit1
                .getY()))
                / (pointUnit2.getX() - pointUnit1.getX());
        float x = ((-k)
                * pointUnit1.getY()
                + k
                * k
                * pointUnit1.getX()
                + center.getX() + k
                * center.getY())
                / (1 + k * k);
        float y = k * x + pointUnit1.getY() - k
                * pointUnit1.getX();
        double curDistance = CommonFunc.distance(
                new Point(x, y), center);
        float[] messageMatrix = {k, x, y, (float) (((curDistance - radius))* (center.getX() - x) / curDistance),
                (float) (((curDistance - radius))* (center.getY() - y) / curDistance)};
        return messageMatrix;
    }

    //一条直线与曲线图形的约束识别
    public Graph lineToCurveConstrain(Graph constraintGraph, Graph curGraph) {
        // 如果有约束 ，返回约束后的图形
        // return constraintGraph;
        List<GUnit> units = constraintGraph.getGraph();
        int size = units.size();
        GUnit unit;
        for (int index = 0; index < size; index++) {
            unit = units.get(index);
            if (unit instanceof CurveUnit) {
                if (((CurveUnit) unit).getCurveType() == CurveType.Circle) {
                    List<GUnit> linePoint = curGraph.getGraph();
                    PointUnit pointUnit1 = (PointUnit) linePoint.get(0);
                    PointUnit pointUnit2 = (PointUnit) linePoint.get(2);
                    Point center = ((CurveUnit) unit).getCenter().getPoint();
                    double radius = ((CurveUnit) unit).getRadius();
                    //切线的计算
					/*
					 * 利用直线方程和垂直向量的公式可求出与已知直线垂直并且过圆心的直线与已知曲线的交点（垂足）H(x,Y)
					 * 利用点H和圆心求向量并平移
					 */
                    if (Math.abs(CommonFunc.lineDistance(pointUnit1.getPoint(),
                            pointUnit2.getPoint(), center)
                            - radius) < ThresholdProperty.GRAPH_CHECKED_DISTANCE / 2) {
                        float[] messageMatrix = getMessageMatrix(pointUnit1, pointUnit2, center, radius);
                        PointUnit tangentPoint = new PointUnit(messageMatrix[1]+ messageMatrix[3],
                                messageMatrix[2]+ messageMatrix[4]);
                        LineUnit line = (LineUnit) linePoint.get(1);
                        if(line.isInUnit(tangentPoint.getPoint()) && (line.getTangentPoint() == null)) {
                            float[][] transMatrix = {{1,0,messageMatrix[3]},
                                    {0,1,messageMatrix[4]}, {0,0,1}};
                            pointUnit1.translate(transMatrix);
                            pointUnit1.setInLine(true);
                            pointUnit1.setCommonConstrainted(true);
                            pointUnit1.setKeyOfLineOrCurve(curGraph.getID());
                            pointUnit1.setIdOfLineOrCurve(line.getID());
                            pointUnit2.translate(transMatrix);
                            pointUnit2.setInLine(true);
                            pointUnit2.setCommonConstrainted(true);
                            pointUnit2.setKeyOfLineOrCurve(curGraph.getID());
                            pointUnit2.setIdOfLineOrCurve(line.getID());

//							constraintGraph.buildGraph(pointUnit1);
//							constraintGraph.buildGraph(line);
                            line.setTangentPoint(tangentPoint);
//							((CurveUnit) unit).addConstraintStruct(new ConstraintStruct(ConstraintType.TangentOfCircle, size+1));
//							constraintGraph.buildGraph(pointUnit2);
                            constraintGraph.setGraphConstrainted(true);
                            curGraph.setGraphConstrainted(true);
                            constraintGraph.addConstraintStruct(new ConstraintStruct(ConstraintType.TangentOfCircle, curGraph.getID()));
                            curGraph.addConstraintStruct(new ConstraintStruct(ConstraintType.TangentOfCircle, constraintGraph.getID()));
                            return constraintGraph;
                        }

                    }

                    //半径，弦
//					CurveUnit constraintUnit = (CurveUnit) units.get(0);
                    List<Point> pList = ((CurveUnit) unit).getPList();

                    double curDistance1;// 线端点到圆上点的距离
                    double curDistance2;// 线端点到圆心的距离
                    Point p1 = null, p2 = null;
                    for (Point pt : pList) {
                        if (p1 == null) {
                            curDistance1 = CommonFunc.distance(pt,
                                    pointUnit1.getPoint());
                            if (curDistance1 < ThresholdProperty.POINT_SELECTED_DISTANCE) {
                                p1 = pt;
                            }
                        }
                        if (p2 == null) {
                            curDistance1 = CommonFunc.distance(pt,
                                    pointUnit2.getPoint());
                            if (curDistance1 < ThresholdProperty.POINT_SELECTED_DISTANCE) {
                                p2 = pt;
                            }
                        }
                    }
//					if (p1 != null
//							&& p2 != null
//							&& CommonFunc.lineDistance(pointUnit1.getPoint(),
//									pointUnit2.getPoint(), center) < radius) {
//						float[][] transMatrix = {
//								{
//										1,
//										0,
//										center.getX()- p1.getX() },
//								{
//										0,
//										1,
//										center.getY()- p1.getY() }, { 0, 0, 1 } };
//						PointUnit centerPoint = new PointUnit(center);
//						centerPoint.translate(transMatrix);
//						p2 = centerPoint.getPoint();
//					} else {
                    if (p1 == null && p2!= null) {
                        curDistance2 = CommonFunc.distance(center,
                                pointUnit1.getPoint());
                        if (curDistance2 < ThresholdProperty.POINT_SELECTED_DISTANCE) {
                            p1 = center;
                        }
                    }
                    if (p2 == null && p1 != null) {

                        curDistance2 = CommonFunc.distance(center,
                                pointUnit2.getPoint());
                        if (curDistance2 < ThresholdProperty.POINT_SELECTED_DISTANCE) {
                            p2 = center;
                        }
                    }
//					}

                    if (p1 != null && p2 != null) {
                        pointUnit1.setX(p1.getX());
                        pointUnit1.setY(p1.getY());
                        if(p1 != center) {
                            pointUnit1.setInCurve(true);
                            pointUnit1.setKeyOfLineOrCurve(constraintGraph.getID());
                            pointUnit1.setIdOfLineOrCurve(unit.getID());
                        } else {
                            pointUnit1.setInLine(true);
                            pointUnit1.setKeyOfLineOrCurve(curGraph.getID());
                        }

//						pointUnit1.setIndexOfCurve(index);
                        pointUnit2.setX(p2.getX());
                        pointUnit2.setY(p2.getY());
                        if(p2 != center) {
                            pointUnit2.setInCurve(true);
                            pointUnit2.setKeyOfLineOrCurve(constraintGraph.getID());
                            pointUnit2.setIdOfLineOrCurve(unit.getID());
                        } else {
                            pointUnit2.setInLine(true);
                            pointUnit2.setKeyOfLineOrCurve(curGraph.getID());
                        }
//						pointUnit2.setIndexOfCurve(index);

//						constraintGraph.buildGraph(pointUnit1);
//						constraintGraph.buildGraph(linePoint.get(1));
//						((CurveUnit) unit).addConstraintStruct(new ConstraintStruct(ConstraintType.HypotenuseOfCircle, size+1));
//						constraintGraph.buildGraph(pointUnit2);
                        constraintGraph.setGraphConstrainted(true);
                        curGraph.setGraphConstrainted(true);
                        constraintGraph.addConstraintStruct(new ConstraintStruct(ConstraintType.HypotenuseOfCircle, curGraph.getID()));
                        curGraph.addConstraintStruct(new ConstraintStruct(ConstraintType.HypotenuseOfCircle, constraintGraph.getID()));
                        return constraintGraph;
                    }

                }
            }
        }
        return null;
    }

    //三角形与曲线图形的约束识别
    public Graph triToCurveConstrain(GraphControl graphControl, Graph constraintGraph, Graph curGraph) {
        //如果有约束 ，返回约束后的图形
        //return constraintGraph;
        //只有一个曲线元
        if(constraintGraph.getGraph().size() == 1) {
            curGraph = curveToTriConstrain(graphControl, curGraph, constraintGraph);
            return curGraph;
        }
        return null;
    }

    //四边形与曲线图形的约束识别
    public Graph rectToCurveConstrain(Graph constraintGraph, Graph curGraph) {
        //如果有约束 ，返回约束后的图形
        //return constraintGraph;
        //如果没约束
        return null;
    }

    //一个曲线与直线图形的约束识别   要区分直线图形是否闭合
    public Graph curveToLineConstrain(Graph constraintGraph, Graph curGraph) {
        //如果有约束 ，返回约束后的曲线图形
        //return constraintGraph;
        //只有一个条直线
        if(constraintGraph.getGraph().size() == 3) {
            curGraph = lineToCurveConstrain(curGraph, constraintGraph);
            return curGraph;
        }
        return null;
    }

    //一个曲线与三角形的约束识别
    public Graph curveToTriConstrain(GraphControl graphControl, Graph constraintGraph, Graph curGraph) {
        //如果有约束 ，返回约束后的图形
        //return constraintGraph;
        CurveUnit curUnit = (CurveUnit) curGraph.getGraph().get(0);
        if (curUnit.getCurveType() == CurveType.Circle) {
            List<GUnit> units = constraintGraph.getGraph();
            PointUnit pointUnit1 = (PointUnit)units.get(0);
            PointUnit pointUnit2 = (PointUnit)units.get(2);
            PointUnit pointUnit3 = (PointUnit)units.get(4);

            Point center = curUnit.getCenter().getPoint();
            double radius = curUnit.getRadius();
            List<Point> pList = curUnit.getPList();

            //三角形内接圆
            double curDistance;
            Point p1 = null, p2 = null, p3 = null;
            for(Point pt : pList) {
                if(p1 == null) {
                    curDistance = CommonFunc.distance(pt, pointUnit1.getPoint());
                    if(curDistance < ThresholdProperty.POINT_SELECTED_DISTANCE) {
                        p1 = pt;
                    }
                }
                if(p2 == null) {
                    curDistance = CommonFunc.distance(pt, pointUnit2.getPoint());
                    if(curDistance < ThresholdProperty.POINT_SELECTED_DISTANCE) {
                        p2 = pt;
                    }
                }
                if(p3 == null) {
                    curDistance = CommonFunc.distance(pt, pointUnit3.getPoint());
                    if(curDistance < ThresholdProperty.POINT_SELECTED_DISTANCE) {
                        p3 = pt;
                    }
                }

            }
            if(p1 != null && p2 != null && p3 != null && !((TriangleGraph) constraintGraph).getCurveConstrainted(0)) {
                int size = units.size();
                pointUnit1.setX(p1.getX());
                pointUnit1.setY(p1.getY());
                pointUnit1.setInCurve(true);
                pointUnit1.setKeyOfLineOrCurve(curGraph.getID());
                pointUnit1.setIdOfLineOrCurve(curUnit.getID());
//				pointUnit1.setIndexOfCurve(size);
                pointUnit2.setX(p2.getX());
                pointUnit2.setY(p2.getY());
                pointUnit2.setInCurve(true);
                pointUnit2.setKeyOfLineOrCurve(curGraph.getID());
                pointUnit2.setIdOfLineOrCurve(curUnit.getID());
//				pointUnit2.setIndexOfCurve(size);
                pointUnit3.setX(p3.getX());
                pointUnit3.setY(p3.getY());
                pointUnit3.setInCurve(true);
                pointUnit3.setKeyOfLineOrCurve(curGraph.getID());
                pointUnit3.setIdOfLineOrCurve(curUnit.getID());
//				pointUnit3.setIndexOfCurve(size);
//				constraintGraph.buildGraph(curUnit);
                ((TriangleGraph) constraintGraph).setCurveConstrainted(0, true);
                constraintGraph.setEqualAngleToF();
                constraintGraph.setRightAngleToF();
                constraintGraph.setGraphConstrainted(true);
                curGraph.setGraphConstrainted(true);
                constraintGraph.addConstraintStruct(new ConstraintStruct(ConstraintType.CircumCircleOfTriangle, curGraph.getID()));
                curGraph.addConstraintStruct(new ConstraintStruct(ConstraintType.CircumCircleOfTriangle, constraintGraph.getID()));
//				((CurveGraph)curGraph).setTriangleConstraint(true);
                KeepConstrainter.getInstance().keepInternallyTangentCircleOfTriangle(graphControl, constraintGraph);
                return constraintGraph;
            }


            //三角形外切圆
            if (Math.abs(CommonFunc.lineDistance(pointUnit1.getPoint(),
                    pointUnit2.getPoint(), center)
                    - radius) < ThresholdProperty.GRAPH_CHECKED_DISTANCE
                    && Math.abs(CommonFunc.lineDistance(pointUnit2.getPoint(),
                    pointUnit3.getPoint(), center) - radius) < ThresholdProperty.GRAPH_CHECKED_DISTANCE
                    && Math.abs(CommonFunc.lineDistance(pointUnit3.getPoint(),
                    pointUnit1.getPoint(), center) - radius) < ThresholdProperty.GRAPH_CHECKED_DISTANCE
                    && !((TriangleGraph) constraintGraph).getCurveConstrainted(1)
                    && curGraph.getConstraintStruct().size() == 0) {

//				float[] messageMatrix1 = getMessageMatrix(pointUnit1, pointUnit2, center, radius);
//				float[] messageMatrix2 = getMessageMatrix(pointUnit2, pointUnit3, center, radius);
//				float[] messageMatrix3 = getMessageMatrix(pointUnit3, pointUnit1, center, radius);
//				//斜率
//				float k12 = messageMatrix1[0];
//				float k23 = messageMatrix2[0];
//				float k31 = messageMatrix3[0];
//				//切点坐标
//				float x12 = messageMatrix1[1] + messageMatrix1[3];
//				float y12 = messageMatrix1[2] + messageMatrix1[4];
//				float x23 = messageMatrix2[1] + messageMatrix2[3];
//				float y23 = messageMatrix2[2] + messageMatrix2[4];
//				float x31 = messageMatrix3[1] + messageMatrix3[3];
//				float y31 = messageMatrix3[2] + messageMatrix3[4];
//				//切线交点坐标
//				float x1 = (y31-k31*x31-y12+k12*x12)/(k12-k31);
//				float y1 = k12*x1+y12-k12*x12;
//				float x2 = (y23-k23*x23-y12+k12*x12)/(k12-k23);
//				float y2 = k12*x2+y12-k12*x12;
//				float x3 = (y31-k31*x31-y23+k23*x23)/(k23-k31);
//				float y3 = k23*x3+y23-k23*x23;
//
//				pointUnit1.setX(x1);
//				pointUnit1.setY(y1);
//				pointUnit2.setX(x2);
//				pointUnit2.setY(y2);
//				pointUnit3.setX(x3);
//				pointUnit3.setY(y3);
                curUnit.setInternallyTangentCircleOfTriangle(true);
//				constraintGraph.buildGraph(curUnit);
                ((TriangleGraph) constraintGraph).setCurveConstrainted(1, true);
                constraintGraph.setEqualAngleToF();
                constraintGraph.setRightAngleToF();
                constraintGraph.setGraphConstrainted(true);
                curGraph.setGraphConstrainted(true);
                constraintGraph.addConstraintStruct(new ConstraintStruct(ConstraintType.InternallyTangentCircleOfTriangle, curGraph.getID()));
                curGraph.addConstraintStruct(new ConstraintStruct(ConstraintType.InternallyTangentCircleOfTriangle, constraintGraph.getID()));
                ((CurveGraph)curGraph).setTriangleConstraint(true);
                KeepConstrainter.getInstance().keepInternallyTangentCircleOfTriangle(graphControl, constraintGraph);
                return constraintGraph;
            }
        }

        return null;
    }

    //一个曲线与四边形的约束识别
    public Graph curveToRectConstrain(Graph constraintGraph, Graph curGraph) {
        //如果有约束 ，返回约束后的图形
        //return constraintGraph;
        //如果没约束
        return null;
    }

}
