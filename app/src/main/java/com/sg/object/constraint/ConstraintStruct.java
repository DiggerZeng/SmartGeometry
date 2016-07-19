package com.sg.object.constraint;

import java.io.Serializable;

import android.R.integer;

import com.sg.object.unit.GUnit;

public class ConstraintStruct implements Cloneable, Serializable {

    private ConstraintType constraintType;
    //private GUnit constraintUnit;
//	private int constraintUnitIndex; //约束图元的下标
    private long constraintGraphKey;
    private int pointID;
    private int lineID;
    //约束类型是否相反  如 点在直线上: PointOnLine  false表示constraintGraphKey是Line,true表示constraintGraphKey是Point所在的图形
    private boolean isOpposite;

//	public ConstraintStruct() {
//		constraintType = ConstraintType.NONE;
//		constraintUnit = null;
//	}

    public ConstraintStruct(ConstraintType constraintType, long constraintGraphKey) {
        this.constraintType = constraintType;
        this.constraintGraphKey = constraintGraphKey;
        this.pointID = -1;
        this.lineID = -1;
        this.isOpposite = false;
    }

    public ConstraintStruct(ConstraintType constraintType, long constraintGraphKey, int pointID, int lineID, boolean isOpposite) {
        this.constraintType = constraintType;
        //this.constraintUnit = constraintUnit;
//		this.constraintUnitIndex = constraintUnitIndex;
        this.constraintGraphKey = constraintGraphKey;
        this.pointID = pointID;
        this.lineID = lineID;
        this.isOpposite = isOpposite;
    }

    public ConstraintType getConstraintType() {
        return constraintType;
    }

    public void setConstraintType(ConstraintType constraintType) {
        this.constraintType = constraintType;
    }

//	public GUnit getConstraintUnit() {
//		return constraintUnit;
//	}
//
//	public void setConstraintUnit(GUnit constraintUnit) {
//		this.constraintUnit = constraintUnit;
//	}
//
//	public int getConstraintUnitIndex() {
//		return constraintUnitIndex;
//	}
//
//	public void setConstraintUnitIndex(int constraintUnitIndex) {
//		this.constraintUnitIndex = constraintUnitIndex;
//	}

    public ConstraintStruct clone() {
        ConstraintStruct constraintStruct = null;
        try {
            constraintStruct = (ConstraintStruct) super.clone();
        } catch (CloneNotSupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return constraintStruct;
    }

    public long getConstraintGraphKey() {
        return constraintGraphKey;
    }

    public void setConstraintGraphKey(long constraintGraphKey) {
        this.constraintGraphKey = constraintGraphKey;
    }

    public int getPointID() {
        return pointID;
    }

    public void setPointID(int pointID) {
        this.pointID = pointID;
    }

    public boolean isOpposite() {
        return isOpposite;
    }

    public void setOpposite(boolean isOpposite) {
        this.isOpposite = isOpposite;
    }

    public int getLineId() {
        return lineID;
    }

    public void setLineId(int lineId) {
        this.lineID = lineId;
    }

}
