package cn.demomaster.qdalive.model;

public class FileModel {
    private int fileSize;
    private byte[] data = new byte[0];

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public boolean isFull() {
        if(data==null){
            return false;
        }
        return fileSize==data.length;
    }
}
