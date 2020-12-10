package cn.demomaster.qdalive.model;

public class TouchPoint {
    private float x;
    private float y;
    private long time;

    public TouchPoint(){

    }
    public TouchPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }
    public TouchPoint(float x, float y, long time) {
        this.x = x;
        this.y = y;
        this.time = time;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
