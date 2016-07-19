package com.sg.transformation.computeagent;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.sg.control.GraphControl;
import com.sg.control.OperationType;
import com.sg.control.UndoRedoSolver;
import com.sg.control.UndoRedoStruct;
import com.sg.logic.common.CommonFunc;
import com.sg.logic.common.VectorFunc;
import com.sg.object.Point;
import com.sg.object.constraint.ConstraintStruct;
import com.sg.object.constraint.ConstraintType;
import com.sg.object.graph.Sketch;
import com.sg.object.graph.Graph;
import com.sg.object.graph.LineGraph;
import com.sg.object.graph.RectangleGraph;
import com.sg.object.graph.TriangleGraph;
import com.sg.object.unit.CurveUnit;
import com.sg.object.unit.GUnit;
import com.sg.object.unit.LineUnit;
import com.sg.object.unit.PointUnit;
import com.sg.property.common.ThresholdProperty;

public class KeepConstrainter {

    private PointUnit point1,point2,point3;

    private static KeepConstrainter instance = new KeepConstrainter();

    public KeepConstrainter(){}

    public static KeepConstrainter getInstance() {
        return instance;
    }

    public void keepConstraint(Graph graph){
        if(!graph.isGraphConstrainted()){
            return;
        }
        if(graph instanceof TriangleGraph){									//判定是否三角形
            keepTriangleConstraint(graph);
        }
    }

    //保存三角形内切圆的约束
    public void keepInternallyTangentCircleOfTriangle(GraphControl graphControl, Graph graph) {
        if(graph instanceof TriangleGraph && ((TriangleGraph)graph).isCurveConstrainted()) {
            List<GUnit> units = graph.getGraph();
            point1 = (PointUnit)units.get(0);
            point2 = (PointUnit)units.get(2);
            point3 = (PointUnit)units.get(4);
            //计算内心,A(x1,y1),B(x2,y2),C(x3,y3) BC=a,CA=b,AB=c, M（（aX1+bX2+cX3)/(a+b+c)，（aY1+bY2+cY3)/(a+b+c)）
            double a = CommonFunc.distance(point2, point3);
            double b = CommonFunc.distance(point3, point1);
            double c = CommonFunc.distance(point1, point2);

            float x = (float) ((a*point1.getX() + b* point2.getX() + c*point3.getX())/(a+b+c));
            float y = (float) ((a*point1.getY() + b* point2.getY() + c*point3.getY())/(a+b+c));
            double dis = CommonFunc.lineDistance(point1.getPoint(), point2.getPoint(), new Point(x,y));
            if(dis == 0) {
                return;
            }
            //变化圆
            for(ConstraintStruct constraintStruct : graph.getConstraintStruct()) {
                if(constraintStruct.getConstraintType() == ConstraintType.InternallyTangentCircleOfTriangle) {
                    Graph curve = graphControl.getGraph(constraintStruct.getConstraintGraphKey());
                    CurveUnit unit = (CurveUnit) curve.getGraph().get(0);
                    float x1 = unit.getCenter().getX();
                    float y1 = unit.getCenter().getY();
                    double r = unit.getRadius();
                    float[][] transMatrix = {{1,0,x-x1},
                            {0,1,y-y1}, {0,0,1}};
//					unit.translate(transMatrix);
                    graphControl.translateGraph(curve, transMatrix, graph.getID());
                    float[][] scaleMatrix = {{(float) (dis/r),0,0},
                            {0,(float) (dis/r),0}, {0,0,1}};
//					unit.scale(scaleMatrix, unit.getCenter().getPoint());
                    graphControl.scaleGraph(curve, scaleMatrix, unit.getCenter().getPoint(), graph.getID());
                    return;
                }
            }
//			for(GUnit unit : units){
//				if(unit instanceof CurveUnit && ((CurveUnit) unit).isInternallyTangentCircleOfTriangle()){
//					float x1 = ((CurveUnit) unit).getCenter().getX();
//					float y1 = ((CurveUnit) unit).getCenter().getY();
//					double r = ((CurveUnit) unit).getRadius();
//					float[][] transMatrix = {{1,0,x-x1},
//							{0,1,y-y1}, {0,0,1}};
//					unit.translate(transMatrix);
//					float[][] scaleMatrix = {{(float) (dis/r),0,0},
//							{0,(float) (dis/r),0}, {0,0,1}};
//					unit.scale(scaleMatrix, ((CurveUnit) unit).getCenter().getPoint());
//				}
//			}
        }

    }

    //保持直线与三角形的约束
    public void keepTriangleConstraint(Graph graph) {
        List<GUnit> units = graph.getGraph();
        for(GUnit unit : units){										//遍历三角形图形链表
            if(unit instanceof LineUnit){
                if(((LineUnit)unit).getType() != 0){					//判定是否约束直线
                    switch(((LineUnit)unit).getVer()){					//根据直线图元所存的顶点索引找出关联点
                        case 0:{
                            point1 = (PointUnit)units.get(0);
                            point2 = (PointUnit)units.get(2);
                            point3 = (PointUnit)units.get(4);
                            break;
                        }
                        case 2:{
                            point1 = (PointUnit)units.get(2);
                            point2 = (PointUnit)units.get(0);
                            point3 = (PointUnit)units.get(4);
                            break;
                        }
                        case 4:{
                            point1 = (PointUnit)units.get(4);
                            point2 = (PointUnit)units.get(0);
                            point3 = (PointUnit)units.get(2);
                            break;
                        }
                    }

                    //获取顶点坐标
                    float x1 = point1.getX();
                    float y1 = point1.getY();

                    //获取底边第一点坐标
                    float x2 = point2.getX();
                    float y2 = point2.getY();

                    //获取底边第二点坐标
                    float x3 = point3.getX();
                    float y3 = point3.getY();

                    //新约束点的坐标
                    float xo,yo;

                    //底边斜率
                    float k = (y2-y3)/(x2-x3);

                    switch(((LineUnit)unit).getType()){
                        case 1:{
                            xo = (x1*(x2-x3)+(y2-k*x2-y1)*(y3-y2))/(k*(y2-y3)+x2-x3);
                            yo = k*(xo-x2)+y2;
                            //((LineUnit)unit).setStartPointUnit(point1);
                            //((LineUnit)unit).setEndPointUnit(new PointUnit((int)xo,(int)yo));
                            ((LineUnit)unit).getEndPointUnit().setX(xo);
                            ((LineUnit)unit).getEndPointUnit().setY(yo);
                            break;
                        }
                        case 2:{
                            xo = (x1+x2)/2;
                            yo = (y1+y2)/2;
                            float tempX = (x1+x3)/2;
                            float tempY = (y1+y3)/2;
                            ((LineUnit)unit).getStartPointUnit().setX(tempX);
                            ((LineUnit)unit).getStartPointUnit().setY(tempY);
                            //((LineUnit)unit).setStartPointUnit(new PointUnit((int)tempX,(int)tempY));
                            //((LineUnit)unit).setEndPointUnit(new PointUnit((int)xo,(int)yo));
                            ((LineUnit)unit).getEndPointUnit().setX(xo);
                            ((LineUnit)unit).getEndPointUnit().setY(yo);
                            break;
                        }
                        case 3:{
                            //两腰的长度
                            double distance1 = CommonFunc.distance(point1.getPoint(), point2.getPoint());
                            double distance2 = CommonFunc.distance(point1.getPoint(), point3.getPoint());

                            //两腰的比值a
                            double a = distance1/distance2;

                            //底边向量，指向为由point2->point3
                            Point vector = new Point(x3-x2,y3-y2);

                            xo = (float) (x2 + vector.getX()*a/(a+1));
                            yo = (float) (y2 + vector.getY()*a/(a+1));
                            //((LineUnit)unit).setStartPointUnit(point1);
                            //((LineUnit)unit).setEndPointUnit(new PointUnit((int)xo,(int)yo));
                            ((LineUnit)unit).getEndPointUnit().setX(xo);
                            ((LineUnit)unit).getEndPointUnit().setY(yo);
                            break;
                        }
                        case 4:{
                            xo = (x2+x3)/2;
                            yo = (y2+y3)/2;
                            //((LineUnit)unit).setStartPointUnit(point1);
                            //((LineUnit)unit).setEndPointUnit(new PointUnit((int)xo,(int)yo));
                            ((LineUnit)unit).getEndPointUnit().setX(xo);
                            ((LineUnit)unit).getEndPointUnit().setY(yo);
                            break;
                        }
                        case 5:{
                            Point vector = new Point(x3-x2,y3-y2);
                            double a = ((LineUnit)unit).getProportion();
                            xo = (float) (x2 + vector.getX()*a/(a+1));
                            yo = (float) (y2 + vector.getY()*a/(a+1));
                            double kp = (yo-y1)/(xo-x1);
						/*	if(kp*k<(-0.7) && kp*k>(-2.5) && ((TriangleGraph) graph).getVer(((LineUnit)unit).getVer())){
								xo = (x1*(x2-x3)+(y2-k*x2-y1)*(y3-y2))/(k*(y2-y3)+x2-x3);
								yo = k*(xo-x2)+y2;
								((LineUnit)unit).setType(1);
								((TriangleGraph) graph).setVer(((LineUnit)unit).getVer()/2);
								}
								else{
									Point vector1 = new Point((int)(x1-x2),(int)(y1-y2));
									Point vector2 = new Point((int)(x1-x3),(int)(y1-y3));
									Point vectorpx = new Point((int)(x1-xo),(int)(y1-yo));
									if(VectorFunc.equalangle(vector1, vector2, vectorpx) && ((TriangleGraph) graph).getAngular(((LineUnit)unit).getVer())){
										double distance1 = CommonFunc.distance(point1.getPoint(), point2.getPoint());
										double distance2 = CommonFunc.distance(point1.getPoint(), point3.getPoint());
										double x = distance1/distance2;
										xo = x2 - vector.getX()*x/(x+1);
										yo = y2 - vector.getY()*x/(x+1);
										((LineUnit)unit).setType(3);
										((TriangleGraph) graph).setAngular(((LineUnit)unit).getVer()/2);
									}else{
										if(Math.abs(CommonFunc.distance(new Point((int)xo,(int)yo), point2.getPoint())-
											CommonFunc.distance(new Point((int)xo,(int)yo), point3.getPoint())) <7
										&& ((TriangleGraph) graph).getMid(((LineUnit)unit).getVer())){
											xo = (x2+x3)/2;
											yo = (y2+y3)/2;
											((LineUnit)unit).setType(4);
											((TriangleGraph) graph).setMid(((LineUnit)unit).getVer()/2);
										}
									}
								}*/
                            //((LineUnit)unit).setStartPointUnit(point1);
                            ((LineUnit)unit).getEndPointUnit().setX(xo);
                            ((LineUnit)unit).getEndPointUnit().setY(yo);
                            break;
                        }
                    }
                }
            }
        }
    }

    /*
     * 保持曲线约束
     * 先将与中心曲线centerCurve约束的曲线curUnit平移transMatrix保持约束，再递归将所有与curUnit约束的曲线都平移transMatrix保持约束
     */
    public void keepCurveConstraint(Graph curGraph, Graph centerCurve) {
//		List<GUnit> units = curGraph.getGraph();
//		List<ConstraintStruct> constraintStructs = centerCurve.getConstraintStruct();
//		CurveUnit curUnit;
//		GUnit temp;
//		int index;
//		float[][] transMatrix;
//		for(ConstraintStruct conStrut : constraintStructs) {
//			index = conStrut.getConstraintUnitIndex();
//			temp = units.get(index);
//			if(temp instanceof CurveUnit) {
//				curUnit = (CurveUnit) temp;
//				transMatrix = getTransMatrix(centerCurve, curUnit, conStrut.getConstraintType());
//				if(transMatrix != null) {
//					curUnit.translate(transMatrix);
//					CurveConstraintRecursion(units, curUnit, transMatrix, units.indexOf(centerCurve));
//				}
//			}
//
//		}
    }

    //递归将所都平移transMatrix保持约束
    public void CurveConstraintRecursion(List<GUnit> units, CurveUnit centerCurve, float[][] transMatrix, int lastCenterIndex) {
//		List<ConstraintStruct> constraintStructs = centerCurve.getConstraintStruct();
//		CurveUnit curUnit;
//		GUnit temp;
//		int index;
//		for(ConstraintStruct conStrut : constraintStructs) {
//			index = conStrut.getConstraintUnitIndex();
//			if(index != lastCenterIndex) {
//				temp = units.get(index);
//				if(temp instanceof CurveUnit) {
//					curUnit = (CurveUnit) temp;
//					curUnit.translate(transMatrix);
//					CurveConstraintRecursion(units, curUnit, transMatrix, units.indexOf(centerCurve));
//				} else {
//					//圆的弦或者切线
//					temp.translate(transMatrix);
//				}
//
//			}
//		}
    }

    //根据约束关系求出transMatrix
    private float[][] getTransMatrix(CurveUnit unit, CurveUnit curUnit, ConstraintType constraintType) {
        double radius1 = unit.getRadius();
        PointUnit center1 = unit.getCenter();
        double radius2= curUnit.getRadius();
        PointUnit center2 = curUnit.getCenter();
        double twoCenterDistance = CommonFunc.distance(center1, center2);  //两圆心距离
        double differenceOfR = Math.abs(radius1 - radius2);  //两半径差
        double totalRadius = radius1 + radius2;   //两半径和
        double scale; //比例
        switch(constraintType) {
            case ExternallyTangentCircle:
                scale = (twoCenterDistance - totalRadius) / twoCenterDistance;
                float[][] transMatrixEx = {{1, 0, (float) ((center1.getX() - center2.getX()) * scale)},
                        {0, 1, (float) ((center1.getY() - center2.getY()) * scale)}, {0, 0, 1}};
                return transMatrixEx;
            case InternallyTangentCircle:
                scale = (twoCenterDistance - differenceOfR) / twoCenterDistance;
                float[][] transMatrixIn = {{1, 0, (float) ((center1.getX() - center2.getX()) * scale)},
                        {0, 1, (float) ((center1.getY() - center2.getY()) * scale)}, {0, 0, 1}};
                return transMatrixIn;
            default:
                return null;
        }
    }

    //拖到一般约束点到特殊约束位置，自动识别为垂线，中线，角平分线
    public Graph rebuildTriangleConstraint(Graph graph){
        if(graph instanceof TriangleGraph){
            List<GUnit> units = graph.getGraph();
            for(int i = 0;i < units.size();i++){
                GUnit unit = units.get(i);
                if(unit instanceof LineUnit){
                    if(((LineUnit)unit).getType() == 5 && ((LineUnit)unit).isDrag()){
                        switch(((LineUnit)unit).getVer()){					//根据直线图元所存的顶点索引找出关联点
                            case 0:{
                                point1 = (PointUnit)units.get(0);
                                point2 = (PointUnit)units.get(2);
                                point3 = (PointUnit)units.get(4);
                                break;
                            }
                            case 2:{
                                point1 = (PointUnit)units.get(2);
                                point2 = (PointUnit)units.get(0);
                                point3 = (PointUnit)units.get(4);
                                break;
                            }
                            case 4:{
                                point1 = (PointUnit)units.get(4);
                                point2 = (PointUnit)units.get(0);
                                point3 = (PointUnit)units.get(2);
                                break;
                            }
                        }
                        //获取顶点坐标
                        float x1 = point1.getX();
                        float y1 = point1.getY();

                        //获取底边第一点坐标
                        float x2 = point2.getX();
                        float y2 = point2.getY();

                        //获取底边第二点坐标
                        float x3 = point3.getX();
                        float y3 = point3.getY();

                        //新约束点的坐标
                        float xo,yo;

                        //底边斜率
                        float k = (y2-y3)/(x2-x3);

                        Point vector = new Point(x3-x2,y3-y2);
                        double a = ((LineUnit)unit).getProportion();
                        xo = (float) (x2 + vector.getX()*a/(a+1));
                        yo = (float) (y2 + vector.getY()*a/(a+1));
                        double kp = (yo-y1)/(xo-x1);
                        if(kp*k<(-0.7) && kp*k>(-2.5) && ((TriangleGraph) graph).getVer(((LineUnit)unit).getVer()/2)){
                            xo = (x1*(x2-x3)+(y2-k*x2-y1)*(y3-y2))/(k*(y2-y3)+x2-x3);
                            yo = k*(xo-x2)+y2;
                            ((LineUnit)unit).setType(1);
                            ((TriangleGraph) graph).setVer(((LineUnit)unit).getVer()/2, false);
                            int j = i+1;
                            ((PointUnit)units.get(j)).setCommonConstrainted(false);
                        }
                        else{
                            Point vector1 = new Point(x1-x2,y1-y2);
                            Point vector2 = new Point(x1-x3,y1-y3);
                            Point vectorpx = new Point(x1-xo,y1-yo);
                            if(VectorFunc.equalangle(vector1, vector2, vectorpx) && ((TriangleGraph) graph).getAngular(((LineUnit)unit).getVer()/2)){
                                double distance1 = CommonFunc.distance(point1.getPoint(), point2.getPoint());
                                double distance2 = CommonFunc.distance(point1.getPoint(), point3.getPoint());
                                double x = distance1/distance2;
                                xo = (float) (x2 - vector.getX()*x/(x+1));
                                yo = (float) (y2 - vector.getY()*x/(x+1));
                                ((LineUnit)unit).setType(3);
                                ((TriangleGraph) graph).setAngular(((LineUnit)unit).getVer()/2, false);
                                int j = i+1;
                                ((PointUnit)units.get(j)).setCommonConstrainted(false);
                            }else{
                                if(Math.abs(CommonFunc.distance(new Point(xo,yo), point2.getPoint())-
                                        CommonFunc.distance(new Point(xo,yo), point3.getPoint())) < 7
                                        && ((TriangleGraph) graph).getMid(((LineUnit)unit).getVer()/2)){
                                    xo = (x2+x3)/2;
                                    yo = (y2+y3)/2;
                                    ((LineUnit)unit).setType(4);
                                    ((TriangleGraph) graph).setMid(((LineUnit)unit).getVer()/2, false);
                                    int j = i+1;
                                    ((PointUnit)units.get(j)).setCommonConstrainted(false);
                                }
                            }
                        }
                        ((LineUnit)unit).setStartPointUnit(point1);
                        ((LineUnit)unit).getEndPointUnit().setX(xo);
                        ((LineUnit)unit).getEndPointUnit().setY(yo);
                        ((LineUnit)unit).setDrag(false);
                    }
                }
            }
        }
        return graph;
    }
}
